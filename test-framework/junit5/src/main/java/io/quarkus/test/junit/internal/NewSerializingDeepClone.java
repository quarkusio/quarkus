package io.quarkus.test.junit.internal;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.marshalling.cloner.ClassCloner;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectCloners;
import org.junit.jupiter.api.TestInfo;

/**
 * A deep-clone implementation using JBoss Marshalling's fast object cloner.
 */
public final class NewSerializingDeepClone implements DeepClone {
    private final ObjectCloner cloner;

    public NewSerializingDeepClone(final ClassLoader sourceLoader, final ClassLoader targetLoader) {
        ClonerConfiguration cc = new ClonerConfiguration();
        cc.setSerializabilityChecker(clazz -> clazz != Object.class);
        cc.setClassCloner(new ClassCloner() {
            public Class<?> clone(final Class<?> original) {
                if (isUncloneable(original)) {
                    return original;
                }
                try {
                    return targetLoader.loadClass(original.getName());
                } catch (ClassNotFoundException ignored) {
                    return original;
                }
            }

            public Class<?> cloneProxy(final Class<?> proxyClass) {
                // not really supported
                return proxyClass;
            }
        });
        cc.setCloneTable(
                (original, objectCloner, classCloner) -> {
                    if (EXTRA_IDENTITY_CLASSES.contains(original.getClass())) {
                        // avoid copying things that do not need to be copied
                        return original;
                    } else if (isUncloneable(original.getClass())) {
                        if (original instanceof Supplier<?> s) {
                            // sneaky
                            return (Supplier<?>) () -> clone(s.get());
                        } else {
                            return original;
                        }
                    } else if (original instanceof TestInfo info) {
                        // copy the test info correctly
                        return new TestInfoImpl(info.getDisplayName(), info.getTags(),
                                info.getTestClass().map(this::cloneClass),
                                info.getTestMethod().map(this::cloneMethod));
                    } else if (original == sourceLoader) {
                        return targetLoader;
                    }
                    // let the default cloner handle it
                    return null;
                });
        cloner = ObjectCloners.getSerializingObjectClonerFactory().createCloner(cc);
    }

    private static boolean isUncloneable(Class<?> clazz) {
        return clazz.isHidden() && !Serializable.class.isAssignableFrom(clazz);
    }

    private Class<?> cloneClass(Class<?> clazz) {
        try {
            return (Class<?>) cloner.clone(clazz);
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    private Method cloneMethod(Method method) {
        try {
            Class<?> declaring = (Class<?>) cloner.clone(method.getDeclaringClass());
            Class<?>[] argTypes = (Class<?>[]) cloner.clone(method.getParameterTypes());
            return declaring.getDeclaredMethod(method.getName(), argTypes);
        } catch (Exception e) {
            return null;
        }
    }

    public Object clone(final Object objectToClone) {
        try {
            return cloner.clone(objectToClone);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Classes which do not need to be cloned.
     */
    private static final Set<Class<?>> EXTRA_IDENTITY_CLASSES = Set.of(
            Object.class,
            byte[].class,
            short[].class,
            int[].class,
            long[].class,
            char[].class,
            boolean[].class,
            float[].class,
            double[].class);
}
