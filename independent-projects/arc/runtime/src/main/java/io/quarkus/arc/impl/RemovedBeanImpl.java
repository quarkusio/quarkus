package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean.Kind;
import io.quarkus.arc.RemovedBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public final class RemovedBeanImpl implements RemovedBean {

    /**
     * Implementation note: this class needs to be optimised to
     * minimize the size of retained memory: runtime efficiency is less important.
     */

    private final Kind kind;
    private final String description;
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;

    public RemovedBeanImpl(Kind kind, String description, Set<Type> types, Set<Annotation> qualifiers) {
        this.kind = kind != null ? kind : Kind.CLASS;
        this.description = description;
        this.types = CollectionHelpers.toImmutableSmallSet(types);
        this.qualifiers = qualifiers != null ? CollectionHelpers.toImmutableSmallSet(qualifiers)
                : Qualifiers.DEFAULT_QUALIFIERS;
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
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
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
        return qualifiers;
    }

    @Override
    public Iterable<Type> types() {
        return types;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(kind).append(" bean ").append(description).append(" [types=")
                .append(types).append(", qualifiers=").append(qualifiers).append("]");
        return builder.toString();
    }

}
