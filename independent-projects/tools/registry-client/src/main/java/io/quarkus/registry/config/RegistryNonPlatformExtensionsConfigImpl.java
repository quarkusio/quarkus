package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.maven.ArtifactCoords;

@JsonDeserialize(builder = RegistryNonPlatformExtensionsConfigImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryNonPlatformExtensionsConfigImpl extends RegistryArtifactConfigImpl
        implements RegistryNonPlatformExtensionsConfig {

    private RegistryNonPlatformExtensionsConfigImpl(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder extends RegistryArtifactConfigImpl.Builder implements RegistryNonPlatformExtensionsConfig {

        public Builder() {
        }

        @JsonIgnore
        public Builder(RegistryNonPlatformExtensionsConfig config) {
            super(config);
        }

        @Override
        public Builder withDisabled(boolean disabled) {
            super.withDisabled(disabled);
            return this;
        }

        @Override
        public Builder withArtifact(ArtifactCoords artifact) {
            super.withArtifact(artifact);
            return this;
        }

        public RegistryNonPlatformExtensionsConfigImpl build() {
            return new RegistryNonPlatformExtensionsConfigImpl(this);
        }
    }

    // see super for equals, hashCode, toString
}
