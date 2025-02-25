package io.quarkus.registry.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see RegistryArtifactConfig#builder() creates a builder
 * @see RegistryArtifactConfig#mutable() creates a builder from an existing RegistryArtifactConfig
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "disabled", "artifact" })
public class RegistryArtifactConfigImpl implements RegistryArtifactConfig {

    protected final boolean disabled;
    protected final ArtifactCoords artifact;

    protected RegistryArtifactConfigImpl(boolean disabled, ArtifactCoords artifact) {
        this.disabled = disabled;
        this.artifact = artifact;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegistryArtifactConfigImpl that = (RegistryArtifactConfigImpl) o;
        return disabled == that.disabled && Objects.equals(artifact, that.artifact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disabled, artifact);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() +
                "{disabled=" + disabled +
                ", artifact=" + artifact +
                '}';
    }

    /**
     * Builder.
     */
    public static class Builder implements RegistryArtifactConfig.Mutable {
        protected boolean disabled;
        protected ArtifactCoords artifact;

        public Builder() {
        }

        @JsonIgnore
        Builder(RegistryArtifactConfig config) {
            this.artifact = config.getArtifact();
            this.disabled = config.isDisabled();
        }

        @Override
        public boolean isDisabled() {
            return this.disabled;
        }

        @Override
        public RegistryArtifactConfig.Mutable setDisabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        @Override
        public ArtifactCoords getArtifact() {
            return this.artifact;
        }

        @Override
        public RegistryArtifactConfig.Mutable setArtifact(ArtifactCoords artifact) {
            this.artifact = artifact;
            return this;
        }

        @Override
        public RegistryArtifactConfigImpl build() {
            return new RegistryArtifactConfigImpl(disabled, artifact);
        }
    }
}
