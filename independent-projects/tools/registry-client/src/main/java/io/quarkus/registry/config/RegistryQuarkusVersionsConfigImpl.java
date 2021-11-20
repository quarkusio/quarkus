package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.registry.json.JsonBuilder;
import java.util.Objects;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see RegistryQuarkusVersionsConfig#builder() creates a builder
 * @see RegistryQuarkusVersionsConfig#mutable() creates a builder from an existing RegistriesConfig
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryQuarkusVersionsConfigImpl implements RegistryQuarkusVersionsConfig {

    private final String recognizedVersionsExpression;
    private final boolean exclusiveProvider;

    private RegistryQuarkusVersionsConfigImpl(String recognizedVersionsExpression, boolean exclusiveProvider) {
        this.exclusiveProvider = exclusiveProvider;
        this.recognizedVersionsExpression = recognizedVersionsExpression;
    }

    @Override
    public String getRecognizedVersionsExpression() {
        return recognizedVersionsExpression;
    }

    @Override
    public boolean isExclusiveProvider() {
        return exclusiveProvider;
    }

    @Override
    public boolean equals(Object o) {
        return quarkusVersionConfigEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exclusiveProvider, recognizedVersionsExpression);
    }

    @Override
    public String toString() {
        return quarkusVersionConfigToString(this);
    }

    /**
     * Builder.
     */
    public static class Builder implements RegistryQuarkusVersionsConfig.Mutable {
        protected String recognizedVersionsExpression;
        protected boolean exclusiveProvider;

        public Builder() {
        }

        @JsonIgnore
        Builder(RegistryQuarkusVersionsConfig config) {
            this.recognizedVersionsExpression = config.getRecognizedVersionsExpression();
            this.exclusiveProvider = config.isExclusiveProvider();
        }

        @Override
        public RegistryQuarkusVersionsConfigImpl build() {
            return new RegistryQuarkusVersionsConfigImpl(recognizedVersionsExpression, exclusiveProvider);
        }

        @Override
        public String getRecognizedVersionsExpression() {
            return recognizedVersionsExpression;
        }

        public Mutable setRecognizedVersionsExpression(String recognizedVersionsExpression) {
            this.recognizedVersionsExpression = recognizedVersionsExpression;
            return this;
        }

        @Override
        public boolean isExclusiveProvider() {
            return exclusiveProvider;
        }

        public Mutable setExclusiveProvider(boolean exclusiveProvider) {
            this.exclusiveProvider = exclusiveProvider;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            return quarkusVersionConfigEquals(this, o);
        }

        @Override
        public int hashCode() {
            return Objects.hash(exclusiveProvider, recognizedVersionsExpression);
        }

        @Override
        public String toString() {
            return quarkusVersionConfigToString(this);
        }
    }

    static boolean quarkusVersionConfigEquals(RegistryQuarkusVersionsConfig v, Object o) {
        if (v == o)
            return true;
        if (!(o instanceof RegistryQuarkusVersionsConfig))
            return false;
        RegistryQuarkusVersionsConfig that = (RegistryQuarkusVersionsConfig) o;
        return v.isExclusiveProvider() == that.isExclusiveProvider()
                && Objects.equals(v.getRecognizedVersionsExpression(), that.getRecognizedVersionsExpression());
    }

    static String quarkusVersionConfigToString(RegistryQuarkusVersionsConfig v) {
        return "RegistryQuarkusVersionsConfig{" +
                "exclusiveProvider=" + v.isExclusiveProvider() +
                ", recognizedVersionsExpression='" + v.getRecognizedVersionsExpression() + '\'' +
                '}';
    }
}
