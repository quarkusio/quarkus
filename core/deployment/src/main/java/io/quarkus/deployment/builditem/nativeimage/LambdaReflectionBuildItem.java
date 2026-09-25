package io.quarkus.deployment.builditem.nativeimage;

import java.util.Arrays;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Lambda that needs reflection metadata for serialization.
 * This generates the lambda specific entries in reachability-metadata.json.
 * See:
 * <p>
 * @formatter:off
 * <a href="https://github.com/oracle/graal/issues/13665">Lambda deserialization ergonomics</a>
 * <a href="https://www.graalvm.org/latest/reference-manual/native-image/metadata/#specifying-metadata-with-json">JSON metadata</a>
 * <a href="https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json">reachability-metadata.json schema</a>
 * @formatter:on
 */
public final class LambdaReflectionBuildItem extends MultiBuildItem {

    private final String declaringClass;
    private final String declaringMethod;
    private final String[] parameterTypes;
    private final String[] interfaces;

    private LambdaReflectionBuildItem(Builder builder) {
        this.declaringClass = Objects.requireNonNull(builder.declaringClass, "declaringClass must not be null");
        this.declaringMethod = Objects.requireNonNull(builder.declaringMethod, "declaringMethod must not be null");
        if (this.declaringClass.isBlank()) {
            throw new IllegalArgumentException("declaringClass must not be blank");
        }
        if (this.declaringMethod.isBlank()) {
            throw new IllegalArgumentException(
                    "declaringMethod must not be blank, every lambda must be declared in a method. "
                            + "Got blank declaringMethod for declaringClass: " + this.declaringClass);
        }
        this.parameterTypes = builder.parameterTypes != null ? builder.parameterTypes : new String[0];
        this.interfaces = Objects.requireNonNull(builder.interfaces, "interfaces must not be null");
        if (this.interfaces.length == 0) {
            throw new IllegalArgumentException(
                    "interfaces must not be empty, every lambda must implement at least one interface. "
                            + "Got empty interfaces for " + this.declaringClass + "#" + this.declaringMethod);
        }
    }

    public static Builder builder(String declaringClass, String declaringMethod) {
        return new Builder(declaringClass, declaringMethod);
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getDeclaringMethod() {
        return declaringMethod;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        // LambdaReflectionBuildItem class is final...
        if (!(o instanceof LambdaReflectionBuildItem that)) {
            return false;
        }
        return declaringClass.equals(that.declaringClass) &&
                declaringMethod.equals(that.declaringMethod) &&
                Arrays.equals(parameterTypes, that.parameterTypes) &&
                Arrays.equals(interfaces, that.interfaces);
    }

    @Override
    public int hashCode() {
        int result = declaringClass.hashCode();
        result = 31 * result + declaringMethod.hashCode();
        result = 31 * result + Arrays.hashCode(parameterTypes);
        result = 31 * result + Arrays.hashCode(interfaces);
        return result;
    }

    public static final class Builder {
        private final String declaringClass;
        private final String declaringMethod;
        private String[] parameterTypes = new String[0];
        private String[] interfaces;

        private Builder(String declaringClass, String declaringMethod) {
            this.declaringClass = declaringClass;
            this.declaringMethod = declaringMethod;
        }

        public Builder parameterTypes(String... parameterTypes) {
            this.parameterTypes = parameterTypes;
            return this;
        }

        public Builder interfaces(String... interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        public LambdaReflectionBuildItem build() {
            return new LambdaReflectionBuildItem(this);
        }
    }
}
