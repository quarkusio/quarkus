package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean.Kind;
import io.quarkus.arc.RemovedBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public final class RemovedBeanImpl implements RemovedBean {

    private final Kind kind;
    private final String description;
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;

    public RemovedBeanImpl(Kind kind, String description, Set<Type> types, Set<Annotation> qualifiers) {
        this.kind = kind;
        this.description = description;
        this.types = CollectionHelpers.toImmutableSmallSet(types);
        this.qualifiers = CollectionHelpers.toImmutableSmallSet(qualifiers);
    }

    @Override
    public Kind getKind() {
        return kind != null ? kind : Kind.CLASS;
    }

    @Override
    public String getDescription() {
        return description != null ? description : "";
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers != null ? qualifiers : Qualifiers.DEFAULT_QUALIFIERS;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getKind()).append(" bean ").append(getDescription()).append(" [types=")
                .append(types).append(", qualifiers=").append(qualifiers).append("]");
        return builder.toString();
    }

}
