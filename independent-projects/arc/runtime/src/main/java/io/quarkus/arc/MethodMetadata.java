package io.quarkus.arc;

import java.lang.annotation.Annotation;

// TODO documentation
public interface MethodMetadata {
    boolean isConstructor();

    boolean isStatic();

    String getName();

    int getModifiers();

    Class<?> getDeclaringClass();

    Class<?> getReturnType();

    int getParameterCount();

    Class<?>[] getParameterTypes();

    ParameterMetadata[] getParameters();

    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    Annotation[] getAnnotations();
}
