package io.quarkus.qute.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateInstanceBase;
import io.quarkus.qute.Variant;
import io.quarkus.qute.api.ResourcePath;
import io.quarkus.qute.api.VariantTemplate;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.runtime.Startup;

@Startup(Interceptor.Priority.PLATFORM_BEFORE)
@Singleton
public class VariantTemplateProducer {

    private static final Logger LOGGER = Logger.getLogger(VariantTemplateProducer.class);

    @Inject
    Instance<Engine> engine;

    private Map<String, TemplateVariants> templateVariants;

    VariantTemplateProducer(QuteContext context) {
        Map<String, TemplateVariants> templateVariants = new HashMap<>();
        for (Entry<String, List<String>> entry : context.getVariants().entrySet()) {
            TemplateVariants var = new TemplateVariants(initVariants(entry.getKey(), entry.getValue()), entry.getKey());
            templateVariants.put(entry.getKey(), var);
        }
        this.templateVariants = Collections.unmodifiableMap(templateVariants);
        LOGGER.debugf("Initializing Qute variant templates: %s", templateVariants);
    }

    @Typed(VariantTemplate.class)
    @Produces
    VariantTemplate getDefaultVariantTemplate(InjectionPoint injectionPoint) {
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
        return new VariantTemplateImpl(name);
    }

    @Typed(VariantTemplate.class)
    @Produces
    @ResourcePath("ignored")
    VariantTemplate getVariantTemplate(InjectionPoint injectionPoint) {
        ResourcePath path = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ResourcePath.class)) {
                path = (ResourcePath) qualifier;
                break;
            }
        }
        if (path == null || path.value().isEmpty()) {
            throw new IllegalStateException("No variant template resource path specified");
        }
        return new VariantTemplateImpl(path.value());
    }

    class VariantTemplateImpl implements VariantTemplate {

        private final String baseName;

        VariantTemplateImpl(String baseName) {
            this.baseName = baseName;
        }

        @Override
        public TemplateInstance instance() {
            return new VariantTemplateInstanceImpl(templateVariants.get(baseName));
        }

        @Override
        public Set<Expression> getExpressions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getGeneratedId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Variant> getVariant() {
            throw new UnsupportedOperationException();
        }

    }

    class VariantTemplateInstanceImpl extends TemplateInstanceBase {

        private final TemplateVariants variants;

        VariantTemplateInstanceImpl(TemplateVariants variants) {
            this.variants = Objects.requireNonNull(variants);
            setAttribute(VariantTemplate.VARIANTS, new ArrayList<>(variants.variantToTemplate.keySet()));
        }

        @Override
        public String render() {
            return template().instance().data(data()).render();
        }

        @Override
        public CompletionStage<String> renderAsync() {
            return template().instance().data(data()).renderAsync();
        }

        @Override
        public Publisher<String> publisher() {
            return template().instance().data(data()).publisher();
        }

        @Override
        public CompletionStage<Void> consume(Consumer<String> consumer) {
            return template().instance().data(data()).consume(consumer);
        }

        private Template template() {
            Variant selected = (Variant) getAttribute(VariantTemplate.SELECTED_VARIANT);
            String name = selected != null ? variants.variantToTemplate.get(selected) : variants.defaultTemplate;
            return engine.get().getTemplate(name);
        }

    }

    static class TemplateVariants {

        public final Map<Variant, String> variantToTemplate;
        public final String defaultTemplate;

        public TemplateVariants(Map<Variant, String> variants, String defaultTemplate) {
            this.variantToTemplate = variants;
            this.defaultTemplate = defaultTemplate;
        }

    }

    static String parseMediaType(String suffix) {
        // TODO we need a proper way to parse the media type
        if (suffix.equalsIgnoreCase(".html") || suffix.equalsIgnoreCase(".htm")) {
            return Variant.TEXT_HTML;
        } else if (suffix.equalsIgnoreCase(".xml")) {
            return Variant.TEXT_XML;
        } else if (suffix.equalsIgnoreCase(".txt")) {
            return Variant.TEXT_PLAIN;
        } else if (suffix.equalsIgnoreCase(".json")) {
            return Variant.APPLICATION_JSON;
        }
        LOGGER.warn("Unknown media type for suffix: " + suffix);
        return "application/octet-stream";
    }

    static String parseMediaType(String base, String variant) {
        String suffix = variant.substring(base.length());
        return parseMediaType(suffix);
    }

    private static Map<Variant, String> initVariants(String base, List<String> availableVariants) {
        Map<Variant, String> map = new HashMap<>();
        for (String path : availableVariants) {
            if (!base.equals(path)) {
                String mediaType = parseMediaType(base, path);
                map.put(new Variant(null, mediaType, null), path);
            }
        }
        return map;
    }

}
