package io.quarkus.arc.test.injectionpoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

class DummyInjectionPoint implements InjectionPoint {
    private final Type type;
    private final Set<Annotation> qualifiers;

    DummyInjectionPoint(Type type, Annotation... qualifiers) {
        this.type = type;
        this.qualifiers = Set.of(qualifiers);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Bean<?> getBean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Member getMember() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Annotated getAnnotated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDelegate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTransient() {
        throw new UnsupportedOperationException();
    }
}
