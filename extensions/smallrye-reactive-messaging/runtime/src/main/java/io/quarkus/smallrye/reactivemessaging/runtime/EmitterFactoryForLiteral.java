package io.quarkus.smallrye.reactivemessaging.runtime;

import java.lang.annotation.Annotation;

import io.smallrye.reactive.messaging.annotations.EmitterFactoryFor;

public class EmitterFactoryForLiteral implements EmitterFactoryFor {

    private Class<?> value;

    public static EmitterFactoryFor of(Class<?> type) {
        return new io.quarkus.smallrye.reactivemessaging.runtime.EmitterFactoryForLiteral(type);
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
}
