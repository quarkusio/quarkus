package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;

import org.jboss.builder.item.MultiBuildItem;

/**
 * Represents a bean that can be easily produced through a template (or other runtime Supplier implementation)
 */
public final class RuntimeBeanBuildItem extends MultiBuildItem {

    final String scope;
    final String type;
    final Supplier<Object> supplier;
    final NavigableMap<String, NavigableMap<String, Object>> qualifiers;
    final boolean removable;

    RuntimeBeanBuildItem(String scope, String type, Supplier<Object> supplier,
            NavigableMap<String, NavigableMap<String, Object>> qualifiers, boolean removable) {
        this.scope = scope;
        this.type = type;
        this.supplier = supplier;
        this.qualifiers = qualifiers;
        this.removable = removable;
    }

    public String getScope() {
        return scope;
    }

    public String getType() {
        return type;
    }

    public Supplier<Object> getSupplier() {
        return supplier;
    }

    public NavigableMap<String, NavigableMap<String, Object>> getQualifiers() {
        return qualifiers;
    }

    public static Builder builder(Class<?> type, Supplier<Object> supplier) {
        return builder(type.getName(), supplier);
    }

    public static Builder builder(String type, Supplier<Object> supplier) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(supplier);
        return new Builder(type, supplier);
    }

    public static class Builder {

        String scope = Dependent.class.getName();
        boolean removable = true;
        final String type;
        final Supplier<Object> supplier;
        final NavigableMap<String, NavigableMap<String, Object>> qualifiers = new TreeMap<>();

        public Builder(String type, Supplier<Object> supplier) {
            this.type = type;
            this.supplier = supplier;
        }

        public Builder setScope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder setScope(Class<? extends Annotation> type) {
            this.scope = type.getName();
            return this;
        }

        public Builder addQualifier(String type) {
            qualifiers.put(type, new TreeMap<>());
            return this;
        }

        public Builder addQualifier(String type, NavigableMap<String, Object> values) {
            qualifiers.put(type, new TreeMap<>(values));
            return this;
        }

        public Builder addQualifier(Class<? extends Annotation> type) {
            return addQualifier(type.getName());
        }

        public Builder addQualifier(Class<? extends Annotation> type, NavigableMap<String, Object> values) {
            return addQualifier(type.getName(), values);
        }

        public Builder setRemovable(boolean removable) {
            this.removable = removable;
            return this;
        }

        public RuntimeBeanBuildItem build() {
            return new RuntimeBeanBuildItem(scope, type, supplier, qualifiers, removable);
        }
    }
}
