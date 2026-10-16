package io.quarkus.arc.impl.bcextensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticInjections;
import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.arc.impl.SyntheticCreationalContextImpl;

public class SyntheticInjectionsImpl implements SyntheticInjections {
    private final Map<SyntheticCreationalContextImpl.TypeAndQualifiers, Object> injections;

    public SyntheticInjectionsImpl(Map<SyntheticCreationalContextImpl.TypeAndQualifiers, Object> injections) {
        this.injections = injections;
    }

    @Override
    public <T> T get(Class<T> type, Annotation... qualifiers) {
        return get((Type) type, qualifiers);
    }

    @Override
    public <T> T get(TypeLiteral<T> type, Annotation... qualifiers) {
        return get(type.getType(), qualifiers);
    }

    @SuppressWarnings("unchecked")
    private <T> T get(Type type, Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        }
        SyntheticCreationalContextImpl.TypeAndQualifiers key = new SyntheticCreationalContextImpl.TypeAndQualifiers(
                type, qualifiers);
        T result = (T) injections.get(key);
        if (result == null) {
            // uncommon case, but dependent beans can be `null`
            if (injections.containsKey(key)) {
                return null;
            }
            throw new IllegalArgumentException(
                    "No injection point declared for type " + type + " and qualifiers " + Arrays.toString(qualifiers));
        }
        return result;
    }
}
