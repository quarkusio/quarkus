package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Qualifier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.deployment.test.TestResourceProvider;

public class ArcTestResourceProvider implements TestResourceProvider {

    public static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    @Override
    public void inject(Object test) {
        Class<?> c = test.getClass();
        ArcContainer container = Arc.container();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Inject.class)) {
                    try {
                        List<Annotation> qualifiers = new ArrayList<>();
                        for (Annotation a : f.getAnnotations()) {
                            if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                                qualifiers.add(a);
                            }
                        }
                        Object instance = container.instance(f.getGenericType(), qualifiers.toArray(EMPTY_ANNOTATION_ARRAY)).get();
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
