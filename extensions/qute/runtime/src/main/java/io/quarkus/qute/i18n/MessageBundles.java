package io.quarkus.qute.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.Qute;
import io.quarkus.qute.Resolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.MessageBundleRecorder.BundleContext;

public final class MessageBundles {

    public static final String ATTRIBUTE_LOCALE = TemplateInstance.LOCALE;
    public static final String DEFAULT_LOCALE = "<<default>>";

    private MessageBundles() {
    }

    public static <T> T get(Class<T> bundleInterface) {
        return get(bundleInterface, null);
    }

    public static <T> T get(Class<T> bundleInterface, Localized localized) {
        if (!bundleInterface.isInterface()) {
            throw new IllegalArgumentException("Not a message bundle interface: " + bundleInterface.getName());
        }
        if (!bundleInterface.isAnnotationPresent(MessageBundle.class)
                && !bundleInterface.isAnnotationPresent(Localized.class)) {
            throw new IllegalArgumentException(
                    "Message bundle interface must be annotated either with @MessageBundle or with @Localized: "
                            + bundleInterface.getName());
        }
        InstanceHandle<T> handle = localized != null ? Arc.container().instance(bundleInterface, localized)
                : Arc.container().instance(bundleInterface);
        if (handle.isAvailable()) {
            return handle.get();
        }
        throw new IllegalStateException(Qute.fmt(
                "Unable to obtain a message bundle for interface [{iface.name}]{#if loc} and locale [{loc.value}]{/if}")
                .data("iface", bundleInterface)
                .data("loc", localized)
                .render());
    }

    static void setupNamespaceResolvers(@Observes EngineBuilder builder, Instance<BundleContext> context) {
        if (!context.isResolvable()) {
            return;
        }
        // Avoid injecting "Instance<Object> instance" which prevents unused beans removal
        ArcContainer container = Arc.container();
        // For every bundle register a new resolver
        for (Entry<String, Map<String, Class<?>>> entry : context.get().getBundleInterfaces().entrySet()) {
            final String bundleName = entry.getKey();
            final Map<String, Resolver> interfaces = new HashMap<>();
            Resolver resolver = null;
            for (Entry<String, Class<?>> locEntry : entry.getValue().entrySet()) {
                if (locEntry.getKey().equals(DEFAULT_LOCALE)) {
                    resolver = (Resolver) container.select(locEntry.getValue(), Default.Literal.INSTANCE).get();
                    continue;
                }
                Instance<?> found = container.select(locEntry.getValue(), new Localized.Literal(locEntry.getKey()));
                if (found.isUnsatisfied()) {
                    throw new IllegalStateException(
                            Qute.fmt("Bean not found for localized interface [{e.value}] and locale [{e.key}]")
                                    .data("e", locEntry).render());
                }
                if (found.isAmbiguous()) {
                    throw new IllegalStateException(
                            Qute.fmt("Multiple beans found for localized interface [{e.value}] and locale [{e.key}]")
                                    .data("e", locEntry).render());
                }
                interfaces.put(locEntry.getKey(), (Resolver) found.get());
            }
            final Resolver defaultResolver = resolver;

            builder.addNamespaceResolver(new NamespaceResolver() {
                @Override
                public CompletionStage<Object> resolve(EvalContext context) {
                    Object locale = context.getAttribute(ATTRIBUTE_LOCALE);
                    if (locale == null) {
                        Object selectedVariant = context.getAttribute(TemplateInstance.SELECTED_VARIANT);
                        if (selectedVariant != null) {
                            locale = ((Variant) selectedVariant).getLocale();
                        }
                        if (locale == null) {
                            return defaultResolver.resolve(context);
                        }
                    }
                    // First try the exact match
                    Resolver localeResolver = interfaces
                            .get(locale instanceof Locale ? ((Locale) locale).toLanguageTag() : locale.toString());
                    if (localeResolver == null && locale instanceof Locale) {
                        // Next try the language
                        localeResolver = interfaces.get(((Locale) locale).getLanguage());
                    }
                    return localeResolver != null ? localeResolver.resolve(context) : defaultResolver.resolve(context);
                }

                @Override
                public String getNamespace() {
                    return bundleName;
                }
            });
        }
    }

    static void preloadMessageTemplates(@Observes Engine engine, Instance<BundleContext> context) {
        if (!context.isResolvable()) {
            return;
        }
        for (String key : context.get().getMessageTemplates().keySet()) {
            Template messageTemplate = engine.getTemplate(key);
            if (messageTemplate == null) {
                throw new IllegalStateException("Unable to preload message template: " + key);
            }
        }
    }

    public static Template getTemplate(String id) {
        return Arc.container().instance(Engine.class).get().getTemplate(id);
    }

}
