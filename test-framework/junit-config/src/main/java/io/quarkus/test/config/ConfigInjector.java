package io.quarkus.test.config;

import java.lang.reflect.Field;

import org.eclipse.microprofile.config.Config;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * An injector to resolve fields and parameters of {@link io.smallrye.config.Config} objects in Quarkus Test Extensions.
 */
public class ConfigInjector {
    public static final ParameterResolver PARAMETER_RESOLVER = new ConfigParameterResolver();

    public static void inject(Object testInstance, Config config) {
        Class<?> c = testInstance.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (org.eclipse.microprofile.config.Config.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        f.set(testInstance, config);
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to set field '" + f.getName() + "'", e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    public static io.smallrye.config.Config get(ExtensionContext extensionContext) {
        Store store = extensionContext.getStore(Namespace.GLOBAL);
        return store.get(io.smallrye.config.Config.class.getName(), io.smallrye.config.Config.class);
    }

    public static void set(ExtensionContext context, io.smallrye.config.Config config) {
        context.getStore(Namespace.GLOBAL).put(io.smallrye.config.Config.class.getName(), config);
    }

    public static void clear(ExtensionContext context) {
        context.getStore(Namespace.GLOBAL).remove(io.smallrye.config.Config.class.getName());
    }

    private static class ConfigParameterResolver implements ParameterResolver {
        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            Class<?> type = parameterContext.getParameter().getType();
            return Config.class.isAssignableFrom(type);
        }

        @Override
        public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return get(extensionContext);
        }
    }
}
