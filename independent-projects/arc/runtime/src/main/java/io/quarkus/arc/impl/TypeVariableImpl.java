package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

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

    @Override
    public int hashCode() {
        // This implementation is not compatible with JDK/guava,
        // but since it's not possible to implement a compatible equals() anyway,
        // it does not really matter.
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(name);
        result = prime * result + Objects.hashCode(bounds);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        // Note that JDK does not make it possible to implement a compatible equals()
        // as it checks a specific implementation class in its equals() method
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TypeVariable)) {
            return false;
        }
        TypeVariable<?> other = (TypeVariable<?>) obj;
        return Objects.equals(name, other.getName()) && Arrays.equals(getBounds(), other.getBounds());
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(" & ", " extends ", "");
        joiner.setEmptyValue("");
        for (Type bound : bounds) {
            if (bound instanceof Class) {
                joiner.add(((Class<?>) bound).getName());
            } else {
                joiner.add(bound.toString());
            }
        }
        return name + joiner;
    }
}
