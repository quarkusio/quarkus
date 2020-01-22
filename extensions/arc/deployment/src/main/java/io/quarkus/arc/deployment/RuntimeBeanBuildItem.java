package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.enterprise.context.NormalScope;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Represents a bean that can be easily produced through a recorder (or other runtime Supplier implementation)
 * 
 * @deprecated Use {@link SyntheticBeanBuildItem} instead
 */
@Deprecated
public final class RuntimeBeanBuildItem extends MultiBuildItem {

    final ScopeInfo scope;
    final String type;
    final Supplier<Object> supplier;
    final RuntimeValue<?> runtimeValue;
    final NavigableMap<String, NavigableMap<String, Object>> qualifiers;
    final boolean removable;

    RuntimeBeanBuildItem(ScopeInfo scope, String type, Supplier<Object> supplier,
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
        return scope.getDotName().toString();
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

        ScopeInfo scope = BuiltinScope.DEPENDENT.getInfo();
        boolean removable = true;
        final String type;
        Supplier<Object> supplier;
        RuntimeValue<?> value;
        final NavigableMap<String, NavigableMap<String, Object>> qualifiers = new TreeMap<>();

        public Builder(String type) {
            this.type = type;
        }

        public Builder setScope(String scope) {
            DotName scopeName = DotName.createSimple(scope);
            this.scope = Optional.ofNullable(BuiltinScope.from(scopeName)).map(BuiltinScope::getInfo)
                    .orElse(new ScopeInfo(scopeName, false));
            return this;
        }

        public Builder setScope(Class<? extends Annotation> type) {
            DotName scopeName = DotName.createSimple(type.getName());
            this.scope = Optional.ofNullable(BuiltinScope.from(scopeName)).map(BuiltinScope::getInfo).orElse(new ScopeInfo(
                    scopeName, type.isAnnotationPresent(NormalScope.class), type.isAnnotationPresent(Inherited.class)));
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
            if (supplier == null && value == null) {
                throw new IllegalStateException("Either a supplier or a runtime value must be set");
            }
            return new RuntimeBeanBuildItem(scope, type, supplier, qualifiers, removable, value);
        }
    }
}
