package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.maven.ArtifactCoords;
import java.util.Objects;

@JsonDeserialize(builder = RegistryArtifactConfigImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryArtifactConfigImpl implements RegistryArtifactConfig {

    protected final boolean disabled;
    protected final ArtifactCoords artifact;

    protected RegistryArtifactConfigImpl(Builder builder) {
        this.disabled = builder.disabled;
        this.artifact = builder.artifact;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     * {@literal with*} methods are used for deserialization
     */
    @JsonPOJOBuilder
    public static class Builder implements RegistryArtifactConfig {
        protected boolean disabled;
        protected ArtifactCoords artifact;

        public Builder() {
        }

        @JsonIgnore
        public Builder(RegistryArtifactConfig config) {
            this.artifact = config.getArtifact();
            this.disabled = config.isDisabled();
        }

        Builder withDisabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        Builder withArtifact(ArtifactCoords artifact) {
            this.artifact = artifact;
            return this;
        }

        @Override
        public boolean isDisabled() {
            return this.disabled;
        }

        @Override
        public ArtifactCoords getArtifact() {
            return this.artifact;
        }

        public RegistryArtifactConfigImpl build() {
            return new RegistryArtifactConfigImpl(this);
        }
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
}
