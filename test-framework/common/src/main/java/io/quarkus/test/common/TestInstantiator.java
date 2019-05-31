package io.quarkus.test.common;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

public class TestInstantiator {

    public static Object instantiateTest(Class<?> testClass) {

        try {
            BeanManager bm = CDI.current().getBeanManager();
            Set<Bean<?>> beans = bm.getBeans(testClass);
            Set<Bean<?>> nonSubClasses = new HashSet<>();
            for (Bean<?> i : beans) {
                if (i.getBeanClass() == testClass) {
                    nonSubClasses.add(i);
                }
            }
            Bean<?> bean = bm.resolve(nonSubClasses);
            return bm.getReference(bean, testClass, bm.createCreationalContext(bean));
        } catch (IllegalStateException e) {
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
