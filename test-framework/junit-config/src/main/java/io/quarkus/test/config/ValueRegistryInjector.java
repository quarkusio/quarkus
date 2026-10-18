package io.quarkus.test.config;

import static io.quarkus.value.registry.ValueRegistry.RuntimeKey.key;

import java.lang.reflect.Field;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;

/**
 * An injector to resolve fields and parameters of f {@link ValueRegistry} and {@link ValueRegistry.RuntimeInfo}
 * objects in Quarkus Test Extensions.
 */
public class ValueRegistryInjector {
    public static final ParameterResolver PARAMETER_RESOLVER = new ValueRegistryParameterResolver();

    public static void inject(Object testInstance, ValueRegistry valueRegistry) {
        Class<?> c = testInstance.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType().equals(ValueRegistry.class)) {
                    try {
                        f.setAccessible(true);
                        f.set(testInstance, valueRegistry);
                        continue;
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to set field '" + f.getName() + "'", e);
                    }
                }

                RuntimeKey<?> key = key(f.getType());
                if (valueRegistry.containsKey(key)) {
                    try {
                        f.setAccessible(true);
                        f.set(testInstance, valueRegistry.get(key));
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to set field '" + f.getName() + "'", e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    public static ValueRegistry get(ExtensionContext context) {
        Store store = context.getStore(Namespace.GLOBAL);
        return store.get(ValueRegistry.class.getName(), ValueRegistry.class);
    }

    public static void set(ExtensionContext context, ValueRegistry valueRegistry) {
        context.getStore(Namespace.GLOBAL).put(ValueRegistry.class.getName(), valueRegistry);
    }

    public static void clear(ExtensionContext context) {
        context.getStore(Namespace.GLOBAL).remove(ValueRegistry.class.getName());
    }

    private static class ValueRegistryParameterResolver implements ParameterResolver {
        public static final ValueRegistryParameterResolver INSTANCE = new ValueRegistryParameterResolver();

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            ValueRegistry valueRegistry = get(extensionContext);
            if (parameterContext.getParameter().getType().equals(ValueRegistry.class)) {
                return true;
            }
            return valueRegistry != null && valueRegistry.containsKey(key(parameterContext.getParameter().getType()));
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            ValueRegistry valueRegistry = get(extensionContext);
            if (parameterContext.getParameter().getType().equals(ValueRegistry.class)) {
                return valueRegistry;
            }
            if (valueRegistry == null) {
                throw new ParameterResolutionException("Could not retrieve parameter: " + parameterContext.getParameter());
            }
            return valueRegistry.get(key(parameterContext.getParameter().getType()));
        }
    }
}
