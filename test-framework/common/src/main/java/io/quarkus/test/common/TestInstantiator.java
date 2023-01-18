package io.quarkus.test.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.jboss.logging.Logger;

public class TestInstantiator {

    private static final Logger log = Logger.getLogger(TestInstantiator.class);

    public static Object instantiateTest(Class<?> testClass, ClassLoader classLoader) {

        try {
            Class<?> actualTestClass = Class.forName(testClass.getName(), true,
                    Thread.currentThread().getContextClassLoader());
            Class<?> cdi = Thread.currentThread().getContextClassLoader().loadClass("jakarta.enterprise.inject.spi.CDI");
            Object instance = cdi.getMethod("current").invoke(null);
            Method selectMethod = cdi.getMethod("select", Class.class, Annotation[].class);
            Object cdiInstance = selectMethod.invoke(instance, actualTestClass, new Annotation[0]);
            return selectMethod.getReturnType().getMethod("get").invoke(cdiInstance);
            //            BeanManager bm = CDI.current().getBeanManager();
            //            Set<Bean<?>> beans = bm.getBeans(testClass);
            //            Set<Bean<?>> nonSubClasses = new HashSet<>();
            //            for (Bean<?> i : beans) {
            //                if (i.getBeanClass() == testClass) {
            //                    nonSubClasses.add(i);
            //                }
            //            }
            //            Bean<?> bean = bm.resolve(nonSubClasses);
            //            return bm.getReference(bean, testClass, bm.createCreationalContext(bean));
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
}
