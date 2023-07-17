package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see RegistryArtifactConfigImpl#builder() creates a builder
 * @see RegistryArtifactConfigImpl#mutable() creates a builder from an existing RegistriesConfig
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryNonPlatformExtensionsConfigImpl extends RegistryArtifactConfigImpl
        implements RegistryNonPlatformExtensionsConfig {

    private RegistryNonPlatformExtensionsConfigImpl(boolean disabled, ArtifactCoords artifact) {
        super(disabled, artifact);
    }

    /**
     * Builder.
     */
    public static class Builder extends RegistryArtifactConfigImpl.Builder
            implements RegistryNonPlatformExtensionsConfig.Mutable {

        public Builder() {
        }

        @JsonIgnore
        Builder(RegistryNonPlatformExtensionsConfig config) {
            super(config);
        }

        @Override
        public Builder setDisabled(boolean disabled) {
            super.setDisabled(disabled);
            return this;
        }

        @Override
        public Builder setArtifact(ArtifactCoords artifact) {
            super.setArtifact(artifact);
            return this;
        }

        @Override
        public RegistryNonPlatformExtensionsConfigImpl build() {
            return new RegistryNonPlatformExtensionsConfigImpl(disabled, artifact);
        }
    }
}
