package io.quarkus.hibernate.orm.runtime.boot.fakebeanmanager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

public class FakeAnnotatedType<T> implements AnnotatedType<T> {
    private final Class<T> type;

    public FakeAnnotatedType(Class<T> type) {
        this.type = type;
    }

    @Override
    public Class<T> getJavaClass() {
        return type;
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return null;
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        return null;
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return null;
    }

    @Override
    public Type getBaseType() {
        return null;
    }

    @Override
    public Set<Type> getTypeClosure() {
        return null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return null;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return null;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return false;
    }
}
