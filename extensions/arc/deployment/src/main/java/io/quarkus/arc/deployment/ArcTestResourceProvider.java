package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Arc;
import io.quarkus.arc.CurrentInjectionPointProvider.InjectionPointImpl;
import io.quarkus.deployment.test.TestResourceProvider;

public class ArcTestResourceProvider implements TestResourceProvider {

    @Override
    public void inject(Object test) {
        Class<?> c = test.getClass();
        BeanManager beanManager = Arc.container().beanManager();
        while (c != Object.class) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(ConfigProperty.class)) {
                    Object instance;

                    try {
                        if (field.getType().equals(BeanManager.class)) {
                            instance = beanManager;
                        } else {
                            Set<Annotation> qualifiers = new HashSet<>();
                            Set<Annotation> annotations = new HashSet<>();
                            for (Annotation a : field.getAnnotations()) {
                                annotations.add(a);
                                if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                                    qualifiers.add(a);
                                }
                            }
                            instance = beanManager.getInjectableReference(
                                    new InjectionPointImpl(field.getGenericType(), field.getGenericType(),
                                            qualifiers, null, annotations, field, -1),
                                    beanManager.createCreationalContext(null));
                        }

                        // Set the field value
                        field.setAccessible(true);
                        try {
                            field.set(test, instance);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to inject field " + field, t);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

}
