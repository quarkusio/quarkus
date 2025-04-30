package io.quarkus.qute.runtime;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.impl.LazyValue;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Location;
import io.quarkus.qute.ParameterDeclaration;
import io.quarkus.qute.RenderedResults;
import io.quarkus.qute.ResultsCollectingTemplateInstance;
import io.quarkus.qute.SectionNode;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateInstanceBase;
import io.quarkus.qute.TemplateNode;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Singleton
public class TemplateProducer {

    private static final Logger LOGGER = Logger.getLogger(TemplateProducer.class);

    private final Engine engine;

    private final Map<String, TemplateVariants> templateVariants;

    // In the dev mode, we need to keep track of injected templates so that we can clear the cached values
    private final List<WeakReference<InjectableTemplate>> injectedTemplates;

    private final RenderedResults renderedResults;

    TemplateProducer(Engine engine, QuteContext context, ContentTypes contentTypes, LaunchMode launchMode,
            Instance<RenderedResults> renderedResults) {
        this.engine = engine;
        Map<String, TemplateVariants> templateVariants = new HashMap<>();
        for (Entry<String, List<String>> entry : context.getVariants().entrySet()) {
            TemplateVariants var = new TemplateVariants(initVariants(entry.getKey(), entry.getValue(), contentTypes),
                    entry.getKey());
            templateVariants.put(entry.getKey(), var);
        }
        this.templateVariants = Collections.unmodifiableMap(templateVariants);
        this.renderedResults = launchMode == LaunchMode.TEST && renderedResults.isResolvable() ? renderedResults.get() : null;
        this.injectedTemplates = launchMode == LaunchMode.DEVELOPMENT ? Collections.synchronizedList(new ArrayList<>()) : null;
        LOGGER.debugf("Initializing Qute variant templates: %s", templateVariants);
    }

    @Produces
    Template getDefaultTemplate(InjectionPoint injectionPoint) {
        String name = null;
        if (injectionPoint.getMember() instanceof Field) {
            // For "@Inject Template items" use "items"
            name = injectionPoint.getMember().getName();
        } else {
            AnnotatedParameter<?> parameter = (AnnotatedParameter<?>) injectionPoint.getAnnotated();
            if (parameter.getJavaParameter().isNamePresent()) {
                name = parameter.getJavaParameter().getName();
            } else {
                name = injectionPoint.getMember().getName();
                LOGGER.warnf("Parameter name not present - using the method name as the template name instead %s", name);
            }
        }
        return newInjectableTemplate(name);
    }

    @Produces
    @Location("ignored")
    Template getTemplate(InjectionPoint injectionPoint) {
        String path = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(Location.class)) {
                path = ((Location) qualifier).value();
                break;
            }
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalStateException("No template location specified");
        }
        return newInjectableTemplate(path);
    }

    /**
     * Used by NativeCheckedTemplateEnhancer to inject calls to this method in the native type-safe methods.
     */
    public Template getInjectableTemplate(String path) {
        return newInjectableTemplate(path);
    }

    public void clearInjectedTemplates() {
        if (injectedTemplates != null) {
            synchronized (injectedTemplates) {
                for (Iterator<WeakReference<InjectableTemplate>> it = injectedTemplates.iterator(); it.hasNext();) {
                    WeakReference<InjectableTemplate> ref = it.next();
                    InjectableTemplate template = ref.get();
                    if (template == null) {
                        it.remove();
                    } else if (template.unambiguousTemplate != null) {
                        template.unambiguousTemplate.clear();
                    }
                }
            }
        }
    }

    private Template newInjectableTemplate(String path) {
        InjectableTemplate template = new InjectableTemplate(path, templateVariants, engine, renderedResults);
        if (injectedTemplates != null) {
            injectedTemplates.add(new WeakReference<>(template));
        }
        return template;
    }

    /**
     * We inject a delegating template in order to:
     *
     * 1. Be able to select an appropriate variant if needed
     * 2. Be able to reload the template when needed, i.e. when the cache is cleared
     */
    static class InjectableTemplate implements Template {

        private final String path;
        private final TemplateVariants variants;
        private final Engine engine;
        // Some methods may only work if a single template variant is found
        private final LazyValue<Template> unambiguousTemplate;
        private final RenderedResults renderedResults;

        InjectableTemplate(String path, Map<String, TemplateVariants> templateVariants, Engine engine,
                RenderedResults renderedResults) {
            this.path = path;
            this.variants = templateVariants.get(path);
            this.engine = engine;
            if (variants == null || variants.variantToTemplate.size() == 1) {
                unambiguousTemplate = new LazyValue<>(new Supplier<Template>() {
                    @Override
                    public Template get() {
                        String id = variants != null ? variants.defaultTemplate : path;
                        return engine.getTemplate(id);
                    }
                });
            } else {
                unambiguousTemplate = null;
            }
            this.renderedResults = renderedResults;
        }

        @Override
        public SectionNode getRootNode() {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().getRootNode();
            }
            throw ambiguousTemplates("getRootNode()");
        }

        @Override
        public TemplateInstance instance() {
            TemplateInstance instance = new InjectableTemplateInstanceImpl();
            return renderedResults != null ? new ResultsCollectingTemplateInstance(instance, renderedResults) : instance;
        }

        @Override
        public List<Expression> getExpressions() {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().getExpressions();
            }
            throw ambiguousTemplates("getExpressions()");
        }

        @Override
        public Expression findExpression(Predicate<Expression> predicate) {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().findExpression(predicate);
            }
            throw ambiguousTemplates("findExpression()");
        }

        @Override
        public List<ParameterDeclaration> getParameterDeclarations() {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().getParameterDeclarations();
            }
            throw ambiguousTemplates("getParameterDeclarations()");
        }

        @Override
        public String getGeneratedId() {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().getGeneratedId();
            }
            throw ambiguousTemplates("getGeneratedId()");
        }

        @Override
        public Optional<Variant> getVariant() {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().getVariant();
            }
            throw ambiguousTemplates("getVariant()");
        }

        @Override
        public String getId() {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().getId();
            }
            throw ambiguousTemplates("getId()");
        }

        @Override
        public Fragment getFragment(String identifier) {
            return new InjectableFragment(identifier);
        }

        @Override
        public Set<String> getFragmentIds() {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().getFragmentIds();
            }
            throw ambiguousTemplates("getFragmentIds()");
        }

        @Override
        public List<TemplateNode> getNodes() {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().getNodes();
            }
            throw ambiguousTemplates("getNodes()");
        }

        @Override
        public Collection<TemplateNode> findNodes(Predicate<TemplateNode> predicate) {
            if (unambiguousTemplate != null) {
                return unambiguousTemplate.get().findNodes(predicate);
            }
            throw ambiguousTemplates("findNodes()");
        }

        private UnsupportedOperationException ambiguousTemplates(String method) {
            return new UnsupportedOperationException("Ambiguous injected templates do not support " + method);
        }

        @Override
        public String toString() {
            return "Injectable template [path=" + path + "]";
        }

        class InjectableFragment implements Fragment {

            private final String identifier;

            InjectableFragment(String identifier) {
                this.identifier = identifier;
            }

            @Override
            public List<Expression> getExpressions() {
                return InjectableTemplate.this.getExpressions();
            }

            @Override
            public Expression findExpression(Predicate<Expression> predicate) {
                return InjectableTemplate.this.findExpression(predicate);
            }

            @Override
            public String getGeneratedId() {
                return InjectableTemplate.this.getGeneratedId();
            }

            @Override
            public Optional<Variant> getVariant() {
                return InjectableTemplate.this.getVariant();
            }

            @Override
            public List<ParameterDeclaration> getParameterDeclarations() {
                return InjectableTemplate.this.getParameterDeclarations();
            }

            @Override
            public String getId() {
                return identifier;
            }

            @Override
            public Template getOriginalTemplate() {
                return InjectableTemplate.this;
            }

            @Override
            public Fragment getFragment(String id) {
                return InjectableTemplate.this.getFragment(id);
            }

            @Override
            public Set<String> getFragmentIds() {
                return InjectableTemplate.this.getFragmentIds();
            }

            @Override
            public List<TemplateNode> getNodes() {
                return InjectableTemplate.this.getNodes();
            }

            @Override
            public SectionNode getRootNode() {
                return InjectableTemplate.this.getRootNode();
            }

            @Override
            public Collection<TemplateNode> findNodes(Predicate<TemplateNode> predicate) {
                return InjectableTemplate.this.findNodes(predicate);
            }

            @Override
            public TemplateInstance instance() {
                TemplateInstance instance = new InjectableFragmentTemplateInstanceImpl(identifier);
                return renderedResults != null ? new ResultsCollectingTemplateInstance(instance, renderedResults) : instance;
            }

        }

        class InjectableTemplateInstanceImpl extends TemplateInstanceBase {

            InjectableTemplateInstanceImpl() {
                if (variants != null) {
                    setAttribute(TemplateInstance.VARIANTS, List.copyOf(variants.variantToTemplate.keySet()));
                }
            }

            @Override
            public String render() {
                return templateInstance().render();
            }

            @Override
            public CompletionStage<String> renderAsync() {
                return templateInstance().renderAsync();
            }

            @Override
            public Multi<String> createMulti() {
                return templateInstance().createMulti();
            }

            @Override
            public Uni<String> createUni() {
                return templateInstance().createUni();
            }

            @Override
            public CompletionStage<Void> consume(Consumer<String> consumer) {
                return templateInstance().consume(consumer);
            }

            @Override
            protected Engine engine() {
                return engine;
            }

            @Override
            public Template getTemplate() {
                return template();
            }

            private TemplateInstance templateInstance() {
                TemplateInstance instance = template().instance();
                if (dataMap != null) {
                    dataMap.forEachData(instance::data);
                    dataMap.forEachComputedData(instance::computedData);
                } else if (data != null) {
                    instance.data(data);
                }
                if (!attributes.isEmpty()) {
                    attributes.forEach(instance::setAttribute);
                }
                if (renderedActions != null) {
                    renderedActions.forEach(instance::onRendered);
                }
                return instance;
            }

            protected Template template() {
                if (unambiguousTemplate != null) {
                    return unambiguousTemplate.get();
                }
                Variant selected = (Variant) getAttribute(TemplateInstance.SELECTED_VARIANT);
                String id;
                if (selected != null) {
                    // Currently, we only use the content type to match the template
                    id = variants.getId(selected.getContentType());
                    if (id == null) {
                        id = variants.defaultTemplate;
                    }
                } else {
                    id = path;
                }
                return engine.getTemplate(id);
            }

        }

        class InjectableFragmentTemplateInstanceImpl extends InjectableTemplateInstanceImpl {

            private final String identifier;

            private InjectableFragmentTemplateInstanceImpl(String identifier) {
                this.identifier = identifier;
            }

            @Override
            protected Template template() {
                return super.template().getFragment(identifier);
            }

        }

    }

    static class TemplateVariants {

        public final Map<Variant, String> variantToTemplate;
        public final String defaultTemplate;

        public TemplateVariants(Map<Variant, String> variants, String defaultTemplate) {
            this.variantToTemplate = variants;
            this.defaultTemplate = defaultTemplate;
        }

        String getId(String contentType) {
            for (Entry<Variant, String> entry : variantToTemplate.entrySet()) {
                if (entry.getKey().getContentType().equals(contentType)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "TemplateVariants [default=" + defaultTemplate + ", variants=" + variantToTemplate + "]";
        }
    }

    private static Map<Variant, String> initVariants(String base, List<String> availableVariants, ContentTypes contentTypes) {
        Map<Variant, String> map = new LinkedHashMap<>();
        for (String path : availableVariants) {
            if (!base.equals(path)) {
                map.put(new Variant(null, contentTypes.getContentType(path), null), path);
            }
        }
        return map;
    }

}
