package io.quarkus.qute.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateInstanceBase;
import io.quarkus.qute.Variant;
import io.quarkus.qute.api.ResourcePath;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Singleton
public class TemplateProducer {

    private static final Logger LOGGER = Logger.getLogger(TemplateProducer.class);

    private final Engine engine;

    private final Map<String, TemplateVariants> templateVariants;

    TemplateProducer(Engine engine, QuteContext context, ContentTypes contentTypes) {
        this.engine = engine;
        Map<String, TemplateVariants> templateVariants = new HashMap<>();
        for (Entry<String, List<String>> entry : context.getVariants().entrySet()) {
            TemplateVariants var = new TemplateVariants(initVariants(entry.getKey(), entry.getValue(), contentTypes),
                    entry.getKey());
            templateVariants.put(entry.getKey(), var);
        }
        this.templateVariants = Collections.unmodifiableMap(templateVariants);
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
        return new InjectableTemplate(name, templateVariants, engine);
    }

    @Produces
    @ResourcePath("ignored")
    Template getTemplate(InjectionPoint injectionPoint) {
        ResourcePath path = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ResourcePath.class)) {
                path = (ResourcePath) qualifier;
                break;
            }
        }
        if (path == null || path.value().isEmpty()) {
            throw new IllegalStateException("No template reource path specified");
        }
        // We inject a delegating template in order to:
        // 1. Be able to select an appropriate variant if needed
        // 2. Be able to reload the template when needed, i.e. when the cache is cleared
        return new InjectableTemplate(path.value(), templateVariants, engine);
    }

    /**
     * Used by NativeCheckedTemplateEnhancer to inject calls to this method in the native type-safe methods.
     */
    public Template getInjectableTemplate(String path) {
        return new InjectableTemplate(path, templateVariants, engine);
    }

    static class InjectableTemplate implements Template {

        private final String path;
        private final TemplateVariants variants;
        private final Engine engine;

        public InjectableTemplate(String path, Map<String, TemplateVariants> templateVariants, Engine engine) {
            this.path = path;
            this.variants = templateVariants.get(path);
            this.engine = engine;
        }

        @Override
        public TemplateInstance instance() {
            return new InjectableTemplateInstanceImpl(path, variants, engine);
        }

        @Override
        public List<Expression> getExpressions() {
            throw new UnsupportedOperationException("Injected templates do not support getExpressions()");
        }

        @Override
        public String getGeneratedId() {
            throw new UnsupportedOperationException("Injected templates do not support getGeneratedId()");
        }

        @Override
        public Optional<Variant> getVariant() {
            throw new UnsupportedOperationException("Injected templates do not support getVariant()");
        }

    }

    static class InjectableTemplateInstanceImpl extends TemplateInstanceBase {

        private final String path;
        private final TemplateVariants variants;
        private final Engine engine;

        public InjectableTemplateInstanceImpl(String path, TemplateVariants variants, Engine engine) {
            this.path = path;
            this.variants = variants;
            this.engine = engine;
            if (variants != null) {
                setAttribute(TemplateInstance.VARIANTS, new ArrayList<>(variants.variantToTemplate.keySet()));
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

        private TemplateInstance templateInstance() {
            TemplateInstance instance = template().instance();
            instance.data(data());
            if (!attributes.isEmpty()) {
                for (Entry<String, Object> entry : attributes.entrySet()) {
                    instance.setAttribute(entry.getKey(), entry.getValue());
                }
            }
            return instance;
        }

        private Template template() {
            Variant selected = (Variant) getAttribute(TemplateInstance.SELECTED_VARIANT);
            String id;
            if (selected != null) {
                id = variants.variantToTemplate.get(selected);
                if (id == null) {
                    id = variants.defaultTemplate;
                }
            } else {
                id = path;
            }
            return engine.getTemplate(id);
        }

    }

    static class TemplateVariants {

        public final Map<Variant, String> variantToTemplate;
        public final String defaultTemplate;

        public TemplateVariants(Map<Variant, String> variants, String defaultTemplate) {
            this.variantToTemplate = variants;
            this.defaultTemplate = defaultTemplate;
        }

        @Override
        public String toString() {
            return "TemplateVariants{default=" + defaultTemplate + ", variants=" + variantToTemplate + "}";
        }
    }

    private static Map<Variant, String> initVariants(String base, List<String> availableVariants, ContentTypes contentTypes) {
        Map<Variant, String> map = new HashMap<>();
        for (String path : availableVariants) {
            if (!base.equals(path)) {
                map.put(new Variant(null, contentTypes.getContentType(path), null), path);
            }
        }
        return map;
    }

}
