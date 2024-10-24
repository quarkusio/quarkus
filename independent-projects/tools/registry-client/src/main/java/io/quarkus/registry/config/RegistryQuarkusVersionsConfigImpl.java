package io.quarkus.registry.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkus.registry.json.JsonBuilder;

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
@JsonPropertyOrder({ "recognizedVersionsExpression", "recognizedGroupIds", "exclusiveProvider" })
public class RegistryQuarkusVersionsConfigImpl implements RegistryQuarkusVersionsConfig {

    private final String recognizedVersionsExpression;
    private final Collection<String> recognizedGroupIds;
    private final boolean exclusiveProvider;

    private RegistryQuarkusVersionsConfigImpl(String recognizedVersionsExpression, Collection<String> recognizedGroupIds,
            boolean exclusiveProvider) {
        this.exclusiveProvider = exclusiveProvider;
        this.recognizedVersionsExpression = recognizedVersionsExpression;
        this.recognizedGroupIds = recognizedGroupIds;
    }

    @Override
    public String getRecognizedVersionsExpression() {
        return recognizedVersionsExpression;
    }

    @Override
    public Collection<String> getRecognizedGroupIds() {
        return recognizedGroupIds;
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
        protected Collection<String> recognizedGroupIds = new ArrayList<>(0);

        public Builder() {
        }

        @JsonIgnore
        Builder(RegistryQuarkusVersionsConfig config) {
            this.recognizedVersionsExpression = config.getRecognizedVersionsExpression();
            this.exclusiveProvider = config.isExclusiveProvider();
        }

        @Override
        public RegistryQuarkusVersionsConfigImpl build() {
            return new RegistryQuarkusVersionsConfigImpl(recognizedVersionsExpression, recognizedGroupIds, exclusiveProvider);
        }

        @Override
        public String getRecognizedVersionsExpression() {
            return recognizedVersionsExpression;
        }

        @Override
        public Mutable setRecognizedVersionsExpression(String recognizedVersionsExpression) {
            this.recognizedVersionsExpression = recognizedVersionsExpression;
            return this;
        }

        @Override
        public Mutable addRecognizedGroupId(String recognizedGropuId) {
            this.recognizedGroupIds.add(recognizedGropuId);
            return this;
        }

        @Override
        public Mutable setRecognizedGroupIds(Collection<String> recognizedGroupIds) {
            this.recognizedGroupIds = recognizedGroupIds;
            return this;
        }

        @Override
        public Collection<String> getRecognizedGroupIds() {
            return recognizedGroupIds;
        }

        @Override
        public boolean isExclusiveProvider() {
            return exclusiveProvider;
        }

        @Override
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
