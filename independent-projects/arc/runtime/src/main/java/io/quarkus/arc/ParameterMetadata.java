package io.quarkus.arc;

import java.lang.annotation.Annotation;

// TODO documentation
public interface ParameterMetadata {
    ParameterMetadata[] EMPTY_ARRAY = new ParameterMetadata[0];

    String getName();

    Class<?> getType();

    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    Annotation[] getAnnotations();
}
