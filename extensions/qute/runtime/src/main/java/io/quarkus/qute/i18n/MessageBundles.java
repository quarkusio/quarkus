package io.quarkus.qute.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.Resolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.runtime.MessageBundleRecorder.BundleContext;

public final class MessageBundles {

    public static final String ATTRIBUTE_LOCALE = "locale";
    public static final String DEFAULT_LOCALE = "<<default>>";

    private static final Logger LOGGER = Logger.getLogger(MessageBundles.class);

    private MessageBundles() {
    }

    public static <T> T get(Class<T> bundleInterface) {
        return get(bundleInterface, null);
    }

    public static <T> T get(Class<T> bundleInterface, Localized localized) {
        if (!bundleInterface.isInterface()) {
            throw new IllegalArgumentException("Not a message bundle interface: " + bundleInterface);
        }
        if (!bundleInterface.isAnnotationPresent(MessageBundle.class)
                && !bundleInterface.isAnnotationPresent(Localized.class)) {
            throw new IllegalArgumentException(
                    "Message bundle interface must be annotated either with @MessageBundle or with @Localized: "
                            + bundleInterface);
        }
        InstanceHandle<T> handle = localized != null ? Arc.container().instance(bundleInterface, localized)
                : Arc.container().instance(bundleInterface);
        if (handle.isAvailable()) {
            return handle.get();
        }
        throw new IllegalStateException("Unable to obtain a message bundle instance for: " + bundleInterface);
    }

    static void setupNamespaceResolvers(@Observes EngineBuilder builder, BundleContext context,
            @Any Instance<Object> instance) {
        // For every bundle register a new resolver
        for (Entry<String, Map<String, Class<?>>> entry : context.getBundleInterfaces().entrySet()) {
            final String bundle = entry.getKey();
            final Map<String, Resolver> interfaces = new HashMap<>();
            Resolver resolver = null;
            for (Entry<String, Class<?>> locEntry : entry.getValue().entrySet()) {
                if (locEntry.getKey().equals(DEFAULT_LOCALE)) {
                    resolver = (Resolver) instance.select(locEntry.getValue(), Default.Literal.INSTANCE).get();
                    continue;
                }
                Instance<?> found = instance.select(locEntry.getValue(), new Localized.Literal(locEntry.getKey()));
                if (!found.isResolvable()) {
                    throw new IllegalStateException("Bean instance for localized interface not found: " + locEntry.getValue());
                }
                interfaces.put(locEntry.getKey(), (Resolver) found.get());
            }
            final Resolver defaultResolver = resolver;

            builder.addNamespaceResolver(new NamespaceResolver() {
                @Override
                public CompletionStage<Object> resolve(EvalContext context) {
                    Object locale = context.getAttribute(ATTRIBUTE_LOCALE);
                    if (locale == null) {
                        return defaultResolver.resolve(context);
                    }
                    Resolver localeResolver = interfaces
                            .get(locale instanceof Locale ? ((Locale) locale).toLanguageTag() : locale.toString());
                    return localeResolver != null ? localeResolver.resolve(context) : defaultResolver.resolve(context);
                }

                @Override
                public String getNamespace() {
                    return bundle;
                }
            });
        }
    }

    static void setupMessageTemplates(@Observes Engine engine, BundleContext context) {
        for (Entry<String, String> entry : context.getMessageTemplates().entrySet()) {
            LOGGER.debugf("Register template for message [%s]", entry.getKey());
            engine.putTemplate(entry.getKey(), engine.parse(entry.getValue()));
        }
    }

    public static Template getTemplate(String id) {
        return Arc.container().instance(Engine.class).get().getTemplate(id);
    }

}
