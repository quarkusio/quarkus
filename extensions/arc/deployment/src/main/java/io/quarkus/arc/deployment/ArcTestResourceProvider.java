package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Qualifier;

import io.quarkus.arc.Arc;
import io.quarkus.deployment.test.TestResourceProvider;

public class ArcTestResourceProvider implements TestResourceProvider {

    @Override
    public void inject(Object test) {
        Class<?> c = test.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Inject.class)) {
                    try {
                        BeanManager beanManager = Arc.container().beanManager();
                        List<Annotation> qualifiers = new ArrayList<>();
                        for (Annotation a : f.getAnnotations()) {
                            if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                                qualifiers.add(a);
                            }
                        }
                        Set<Bean<?>> beans = beanManager.getBeans(f.getType(),
                                qualifiers.toArray(new Annotation[qualifiers.size()]));
                        Bean<?> bean = beanManager.resolve(beans);
                        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
                        Object instance = beanManager.getReference(bean, f.getType(), ctx);
                        f.setAccessible(true);
                        try {
                            f.set(test, instance);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to inject field " + f, t);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }
}
