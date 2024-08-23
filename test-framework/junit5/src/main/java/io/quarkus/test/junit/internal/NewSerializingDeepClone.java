package io.quarkus.test.junit.internal;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jboss.marshalling.cloner.ClassCloner;
import org.jboss.marshalling.cloner.ClonerConfiguration;
import org.jboss.marshalling.cloner.ObjectCloner;
import org.jboss.marshalling.cloner.ObjectCloners;
import org.junit.jupiter.api.TestInfo;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;

/**
 * A deep-clone implementation using JBoss Marshalling's fast object cloner.
 */
public final class NewSerializingDeepClone implements DeepClone {
    private final ObjectCloner cloner;
    private static Pattern clonePattern;
    private RunningQuarkusApplication runningQuarkusApplication;

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
                    // The class we're really dealing with, which might be wrapped inside an array, or a nest of arrays
                    Class<?> theClassWeCareAbout = original.getClass();
                    while (theClassWeCareAbout.isArray()) {
                        theClassWeCareAbout = theClassWeCareAbout.getComponentType();
                    }

                    // Short-circuit the checks if we've been configured to clone this
                    if (!clonePattern.matcher(theClassWeCareAbout.getName()).matches()) {

                        if (theClassWeCareAbout.isPrimitive()) {
                            // avoid copying things that do not need to be copied
                            return original;
                        } else if (isUncloneable(theClassWeCareAbout)) {
                            if (original instanceof Supplier<?> s) {
                                // sneaky
                                return (Supplier<?>) () -> clone(s.get());
                            } else {
                                return original;
                            }
                        } else if (original instanceof TestInfo info) {
                            // copy the test info correctly
                            return new TestInfoImpl(info.getDisplayName(), info.getTags(),
                                    info.getTestClass()
                                            .map(this::cloneClass),
                                    info.getTestMethod()
                                            .map(this::cloneMethod));
                        } else {
                            try {
                                if (runningQuarkusApplication != null && runningQuarkusApplication.getClassLoader()
                                        .loadClass(theClassWeCareAbout.getName()) == theClassWeCareAbout) {
                                    // Don't clone things which are already loaded by the quarkus application's classloader side of the tree
                                    return original;
                                }
                            } catch (ClassNotFoundException e) {

                                if (original instanceof Supplier<?> s) {
                                    // sneaky
                                    return (Supplier<?>) () -> clone(s.get());
                                } else {
                                    throw e;
                                }
                            }

                            if (original == sourceLoader) {
                                return targetLoader;
                            }
                        }
                    }

                    // let the default cloner handle it
                    return null;
                });
        cloner = ObjectCloners.getSerializingObjectClonerFactory()
                .createCloner(cc);
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

    @Override
    public void setRunningQuarkusApplication(RunningQuarkusApplication runningQuarkusApplication) {
        this.runningQuarkusApplication = runningQuarkusApplication;
        String patternString = runningQuarkusApplication.getConfigValue("quarkus.test.class-clone-pattern", String.class)
                .orElse("java\\..*");
        clonePattern = Pattern.compile(patternString);
    }

}
