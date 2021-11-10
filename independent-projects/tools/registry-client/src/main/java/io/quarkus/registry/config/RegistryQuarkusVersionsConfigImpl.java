package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;

@JsonDeserialize(builder = RegistryQuarkusVersionsConfigImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryQuarkusVersionsConfigImpl implements RegistryQuarkusVersionsConfig {

    private final String recognizedVersionsExpression;
    private final boolean exclusiveProvider;

    private RegistryQuarkusVersionsConfigImpl(Builder builder) {
        this.exclusiveProvider = builder.exclusiveProvider;
        this.recognizedVersionsExpression = builder.recognizedVersionsExpression;
    }

    @Override
    public String getRecognizedVersionsExpression() {
        return recognizedVersionsExpression;
    }

    @Override
    public boolean isExclusiveProvider() {
        return exclusiveProvider;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder {
        protected String recognizedVersionsExpression;
        protected boolean exclusiveProvider;

        public Builder() {
        }

        @JsonIgnore
        public Builder(RegistryQuarkusVersionsConfig config) {
            this.recognizedVersionsExpression = config.getRecognizedVersionsExpression();
            this.exclusiveProvider = config.isExclusiveProvider();
        }

        public Builder withRecognizedVersionsExpression(String recognizedVersionsExpression) {
            this.recognizedVersionsExpression = recognizedVersionsExpression;
            return this;
        }

        public Builder withExclusiveProvider(boolean exclusiveProvider) {
            this.exclusiveProvider = exclusiveProvider;
            return this;
        }

        public RegistryQuarkusVersionsConfigImpl build() {
            return new RegistryQuarkusVersionsConfigImpl(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegistryQuarkusVersionsConfigImpl that = (RegistryQuarkusVersionsConfigImpl) o;
        return exclusiveProvider == that.exclusiveProvider
                && Objects.equals(recognizedVersionsExpression, that.recognizedVersionsExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exclusiveProvider, recognizedVersionsExpression);
    }

    @Override
    public String toString() {
        return "BaseRegistryQuarkusVersionsConfig{" +
                "exclusiveProvider=" + exclusiveProvider +
                ", recognizedVersionsExpression='" + recognizedVersionsExpression + '\'' +
                '}';
    }
}
