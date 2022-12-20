package io.quarkus.test.util.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public final class AnnotationContainer<A extends Annotation> {

    private final AnnotatedElement element;
    private final A annotation;

    public AnnotationContainer(AnnotatedElement element, A annotation) {
        this.element = element;
        this.annotation = annotation;
    }

    public AnnotatedElement getElement() {
        return element;
    }

    public A getAnnotation() {
        return annotation;
    }
}
