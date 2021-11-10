package io.quarkus.registry.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.quarkus.maven.ArtifactCoords;
import java.util.Objects;

@JsonDeserialize(builder = RegistryDescriptorConfigImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RegistryDescriptorConfigImpl implements RegistryDescriptorConfig {

    private final ArtifactCoords artifact;

    private RegistryDescriptorConfigImpl(Builder builder) {
        this.artifact = builder.artifact;
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
    public static class Builder implements RegistryDescriptorConfig {
        protected ArtifactCoords artifact;

        public Builder() {
        }

        @JsonIgnore
        public Builder(RegistryDescriptorConfig config) {
            this.artifact = config.getArtifact();
        }

        @Override
        public ArtifactCoords getArtifact() {
            return artifact;
        }

        public Builder withArtifact(ArtifactCoords artifact) {
            this.artifact = artifact;
            return this;
        }

        public RegistryDescriptorConfigImpl build() {
            return new RegistryDescriptorConfigImpl(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof RegistryDescriptorConfig))
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
        return "BaseRegistryDescriptorConfig{" +
                "artifact=" + artifact +
                '}';
    }

    /** Internal class used for generated coordinates. These are not written out again */
    static class Generated implements RegistryDescriptorConfig {
        final ArtifactCoords artifact;

        Generated(ArtifactCoords artifact) {
            this.artifact = artifact;
        }

        @Override
        public ArtifactCoords getArtifact() {
            return artifact;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || !(o instanceof RegistryDescriptorConfig))
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
            return "Generated{" +
                    "artifact=" + artifact +
                    '}';
        }
    }
}
