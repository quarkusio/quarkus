package io.quarkus.arc.processor;

import org.jboss.jandex.DotName;

public class BeanDefiningAnnotation {

    private final DotName annotation;
    private final DotName defaultScope;

    public BeanDefiningAnnotation(DotName annotation, DotName defaultScope) {
        this.annotation = annotation;
        this.defaultScope = defaultScope;
    }

    public DotName getAnnotation() {
        return annotation;
    }

    public DotName getDefaultScope() {
        return defaultScope;
    }
}
