package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Represents a bean that can be easily produced through a recorder (or other runtime Supplier implementation)
 */
public final class RuntimeBeanBuildItem extends MultiBuildItem {

    final String scope;
    final String type;
    final Supplier<Object> supplier;
    final RuntimeValue<?> runtimeValue;
    final NavigableMap<String, NavigableMap<String, Object>> qualifiers;
    final boolean removable;

    RuntimeBeanBuildItem(String scope, String type, Supplier<Object> supplier,
            NavigableMap<String, NavigableMap<String, Object>> qualifiers, boolean removable,
            RuntimeValue<?> runtimeValue) {
        if (supplier != null && runtimeValue != null) {
            throw new IllegalArgumentException("It is not possible to specify both - a supplier and a runtime value");
        }
        this.scope = scope;
        this.type = type;
        this.supplier = supplier;
        this.qualifiers = qualifiers;
        this.removable = removable;
        this.runtimeValue = runtimeValue;
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

    public RuntimeValue<?> getRuntimeValue() {
        return runtimeValue;
    }

    public boolean isRemovable() {
        return removable;
    }

    public NavigableMap<String, NavigableMap<String, Object>> getQualifiers() {
        return qualifiers;
    }

    public static Builder builder(Class<?> type) {
        return builder(type.getName());
    }

    public static Builder builder(String type) {
        Objects.requireNonNull(type);
        return new Builder(type);
    }

    public static class Builder {

        String scope = Dependent.class.getName();
        boolean removable = true;
        final String type;
        Supplier<Object> supplier;
        RuntimeValue<?> value;
        final NavigableMap<String, NavigableMap<String, Object>> qualifiers = new TreeMap<>();

        public Builder(String type) {
            this.type = type;
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

        public Builder setSupplier(Supplier<Object> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
            return this;
        }

        public Builder setRuntimeValue(RuntimeValue<?> runtimeValue) {
            this.value = Objects.requireNonNull(runtimeValue);
            return this;
        }

        public RuntimeBeanBuildItem build() {
            return new RuntimeBeanBuildItem(scope, type, supplier, qualifiers, removable, value);
        }
    }
}
