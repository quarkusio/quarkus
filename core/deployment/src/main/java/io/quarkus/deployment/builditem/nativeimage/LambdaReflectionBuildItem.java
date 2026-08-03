package io.quarkus.deployment.builditem.nativeimage;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Lambda that needs reflection metadata for serialization.
 * This generates the lambda specific entries in reachability-metadata.json.
 * See:
 * </p>
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
        this.parameterTypes = builder.parameterTypes != null ? builder.parameterTypes : new String[0];
        this.interfaces = Objects.requireNonNull(builder.interfaces, "interfaces must not be null");
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
