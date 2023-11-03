package io.quarkus.registry.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.json.JsonBuilder;

/**
 * Asymmetric data manipulation:
 * Deserialization always uses the builder;
 * Serialization always uses the Impl.
 *
 * @see RegistryDescriptorConfig#builder() creates a builder
 * @see RegistryDescriptorConfig#mutable() creates a builder from an existing RegistryDescriptorConfig
 * @see JsonBuilder.JsonBuilderSerializer for building a builder before serializing it.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryDescriptorConfigImpl implements RegistryDescriptorConfig {

    private final ArtifactCoords artifact;

    @JsonIgnore
    private final boolean generated;

    // Package private. Used when filling in defaults (that shouldn't be persisted), too
    RegistryDescriptorConfigImpl(ArtifactCoords artifact, boolean generated) {
        this.artifact = artifact;
        this.generated = generated;
    }

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RegistryDescriptorConfig))
            return false;
        RegistryDescriptorConfig that = (RegistryDescriptorConfig) o;
        return Objects.equals(artifact, that.getArtifact());
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() +
                "{artifact=" + artifact +
                '}';
    }

    /**
     * Builder.
     * {@literal set*} methods are used for deserialization
     */
    public static class Builder implements RegistryDescriptorConfig.Mutable {
        protected ArtifactCoords artifact;

        public Builder() {
        }

        @JsonIgnore
        Builder(RegistryDescriptorConfig config) {
            this.artifact = config.getArtifact();
        }

        @Override
        public ArtifactCoords getArtifact() {
            return artifact;
        }

        public Builder setArtifact(ArtifactCoords artifact) {
            this.artifact = artifact;
            return this;
        }

        @Override
        public RegistryDescriptorConfigImpl build() {
            return new RegistryDescriptorConfigImpl(artifact, false);
        }
    }

    static boolean isGenerated(Object config) {
        return config instanceof RegistryDescriptorConfigImpl
                && ((RegistryDescriptorConfigImpl) config).generated;
    }
}
