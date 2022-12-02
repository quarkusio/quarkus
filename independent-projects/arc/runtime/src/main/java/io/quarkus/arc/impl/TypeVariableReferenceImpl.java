package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class TypeVariableReferenceImpl<D extends GenericDeclaration> implements TypeVariable<D> {
    private final String name;
    private TypeVariableImpl<D> delegate;

    public TypeVariableReferenceImpl(String name) {
        this.name = name;
    }

    public void setDelegate(TypeVariableImpl<D> delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return delegate.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return delegate.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return delegate.getDeclaredAnnotations();
    }

    @Override
    public Type[] getBounds() {
        return delegate.getBounds();
    }

    @Override
    public D getGenericDeclaration() {
        return delegate.getGenericDeclaration();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public AnnotatedType[] getAnnotatedBounds() {
        return delegate.getAnnotatedBounds();
    }

    @Override
    public String toString() {
        return name;
    }
}
