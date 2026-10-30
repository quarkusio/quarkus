package io.quarkus.smallrye.reactivemessaging.runtime;

import java.lang.annotation.Annotation;

import jakarta.enterprise.util.AnnotationLiteral;

import io.smallrye.reactive.messaging.annotations.EmitterFactoryFor;

public class EmitterFactoryForLiteral extends AnnotationLiteral<EmitterFactoryFor> implements EmitterFactoryFor {

    private static final long serialVersionUID = 1L;

    private Class<?> value;

    public static EmitterFactoryFor of(Class<?> type) {
        return new EmitterFactoryForLiteral(type);
    }

    public EmitterFactoryForLiteral() {
    }

    public EmitterFactoryForLiteral(Class<?> type) {
        this.value = type;
    }

    public Class<?> getValue() {
        return value;
    }

    public void setValue(Class<?> value) {
        this.value = value;
    }

    @Override
    public Class<?> value() {
        return value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return EmitterFactoryFor.class;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof EmitterFactoryFor that) {
            return this.value.equals(that.value());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 127 * "value".hashCode() ^ this.value.hashCode();
    }
}
