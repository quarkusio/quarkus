package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;

public class TypeVariableImpl<D extends GenericDeclaration> implements TypeVariable<D> {

    private final String name;

    private final List<Type> bounds;

    public TypeVariableImpl(String name, Type... bounds) {
        this.name = name;
        this.bounds = Arrays.asList(bounds);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Annotation[] getAnnotations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type[] getBounds() {
        return bounds.toArray(new Type[bounds.size()]);
    }

    @Override
    public D getGenericDeclaration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AnnotatedType[] getAnnotatedBounds() {
        throw new UnsupportedOperationException();
    }

}
