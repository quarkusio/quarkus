package io.quarkus.test.junit;

import static io.quarkus.registry.ValueRegistry.RuntimeKey.key;

import java.lang.reflect.Field;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.registry.ValueRegistry.RuntimeKey;

/**
 * A field injector for JUnit to allow the resolution of {@link ValueRegistry} and {@link ValueRegistry.RuntimeInfo}
 * objects, available in {@link io.quarkus.test.junit.QuarkusTestExtension} and
 * {@link io.quarkus.test.junit.QuarkusIntegrationTestExtension}.
 */
public class ValueRegistryInjector {
    public static void inject(Object testInstance, QuarkusTestExtensionState extensionContext) {
        ValueRegistry valueRegistry = extensionContext.getValueRegistry();
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
}
