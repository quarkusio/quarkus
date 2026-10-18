package io.quarkus.test.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

public class TestInstantiator {

    private static final Logger log = Logger.getLogger(TestInstantiator.class);

    public static Object instantiateTest(Class<?> testClass, ClassLoader classLoader) {

        try {
            Class<?> actualTestClass = Class.forName(testClass.getName(), true,
                    Thread.currentThread().getContextClassLoader());
            Class<?> delegate = Thread.currentThread().getContextClassLoader()
                    .loadClass("io.quarkus.test.common.TestInstantiator$Delegate");
            Method instantiate = delegate.getMethod("instantiate", Class.class);
            return instantiate.invoke(null, actualTestClass);
        } catch (Exception e) {
            log.warn("Failed to initialize test as a CDI bean, falling back to direct initialization", e);
            try {
                Constructor<?> ctor = testClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    // this class shall be loaded by the Quarkus CL
    public static class Delegate {
        public static Object instantiate(Class<?> clazz) {
            CDI<Object> cdi = CDI.current();
            Instance<?> instance = cdi.select(clazz);
            if (instance.isResolvable()) {
                return instance.get();
            }

            if (clazz.getTypeParameters().length > 0) {
                // fallback for generic test classes, whose set of bean types
                // does not contain a `Class` but a `ParameterizedType` instead
                for (Instance.Handle<Object> handle : cdi.select(Object.class).handles()) {
                    if (clazz.equals(handle.getBean().getBeanClass())) {
                        return handle.get();
                    }
                }
            }

            throw new IllegalStateException("No bean: " + clazz);
        }
    }
}
