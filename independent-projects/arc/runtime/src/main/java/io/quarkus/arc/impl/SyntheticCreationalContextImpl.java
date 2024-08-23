package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.SyntheticCreationalContext;

public final class SyntheticCreationalContextImpl<T> implements SyntheticCreationalContext<T> {

    final CreationalContext<T> creationalContext;
    private final Map<TypeAndQualifiers, Object> injectedReferences;
    private final Map<String, Object> params;

    public SyntheticCreationalContextImpl(CreationalContext<T> creationalContext, Map<String, Object> params,
            Map<TypeAndQualifiers, Object> injectedReferences) {
        this.creationalContext = Objects.requireNonNull(creationalContext);
        this.params = Objects.requireNonNull(params);
        this.injectedReferences = Objects.requireNonNull(injectedReferences);
    }

    public CreationalContext<T> getDelegateCreationalContext() {
        return creationalContext;
    }

    @Override
    public void push(T incompleteInstance) {
        creationalContext.push(incompleteInstance);
    }

    @Override
    public void release() {
        creationalContext.release();
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public <R> R getInjectedReference(Class<R> requiredType, Annotation... qualifiers) {
        return getReference(requiredType, qualifiers);
    }

    @Override
    public <R> R getInjectedReference(TypeLiteral<R> requiredType, Annotation... qualifiers) {
        return getReference(requiredType.getType(), qualifiers);
    }

    @Override
    public <R> InterceptionProxy<R> getInterceptionProxy() {
        for (Map.Entry<TypeAndQualifiers, Object> entry : injectedReferences.entrySet()) {
            if (entry.getKey().requiredType.getTypeName().startsWith(InterceptionProxy.class.getName())) {
                return (InterceptionProxy<R>) entry.getValue();
            }
        }
        throw new IllegalArgumentException(
                "No InterceptionProxy registered for this synthetic bean; call injectInterceptionProxy()");
    }

    @SuppressWarnings("unchecked")
    private <R> R getReference(Type requiredType, Annotation... qualifiers) {
        if (qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        }
        R ref = (R) injectedReferences.get(new TypeAndQualifiers(requiredType, qualifiers));
        if (ref == null) {
            throw new IllegalArgumentException("A synthetic injection point was not declared for required type [" + requiredType
                    + " and qualifiers: " + Arrays.toString(qualifiers));
        }
        return ref;
    }

    public final static class TypeAndQualifiers {

        private final Type requiredType;
        private final Annotation[] qualifiers;

        public TypeAndQualifiers(Type requiredType, Annotation[] qualifiers) {
            this.requiredType = Objects.requireNonNull(requiredType);
            this.qualifiers = qualifiers == null ? new Annotation[] { Default.Literal.INSTANCE } : qualifiers;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(qualifiers);
            result = prime * result + requiredType.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TypeAndQualifiers other = (TypeAndQualifiers) obj;
            return Objects.equals(requiredType, other.requiredType) && Arrays.equals(qualifiers, other.qualifiers);
        }

    }

}
