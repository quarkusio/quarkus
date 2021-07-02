package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean.Kind;
import io.quarkus.arc.RemovedBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class RemovedBeanImpl implements RemovedBean {

    /**
     * Implementation note: this class needs to be optimised to
     * minimize the size of retained memory: runtime efficiency is less important.
     */
    private static final Type[] EMPTY_TYPES = new Type[] {};
    private static final Annotation[] EMPTY_QUALIFIERS = new Annotation[] {};
    private static final Annotation[] DEFAULT_QUALIFIERS = Qualifiers.DEFAULT_QUALIFIERS.toArray(EMPTY_QUALIFIERS);

    private final Kind kind;
    private final String description;
    private final Type[] types;
    private final Annotation[] qualifiers;

    public RemovedBeanImpl(Kind kind, String description, Set<Type> types, Set<Annotation> qualifiers) {
        this.kind = kind != null ? kind : Kind.CLASS;
        this.description = description;
        this.types = types == null ? EMPTY_TYPES : types.toArray(EMPTY_TYPES);
        this.qualifiers = qualifiers != null ? qualifiers.toArray(EMPTY_QUALIFIERS) : DEFAULT_QUALIFIERS;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Set<Type> getTypes() {
        return new HashSet<>(Arrays.asList(types));
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return new HashSet<>(Arrays.asList(qualifiers));
    }

    @Override
    public boolean matchesType(Type requiredType) {
        for (Type t : this.types) {
            if (BeanTypeAssignabilityRules.matches(requiredType, t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<Annotation> qualifiers() {
        return Arrays.asList(qualifiers);
    }

    @Override
    public Iterable<Type> types() {
        return Arrays.asList(types);
    }

    @Override
    public String toString() {
        return kind.toString() + " bean " + description +
                " [types=" + Arrays.toString(types) +
                ", qualifiers=" + Arrays.toString(qualifiers) + "]";
    }

}
