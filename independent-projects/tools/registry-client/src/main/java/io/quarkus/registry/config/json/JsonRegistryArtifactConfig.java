package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistryArtifactConfig;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonRegistryArtifactConfig implements RegistryArtifactConfig.Mutable {

    protected boolean disabled;
    protected ArtifactCoords artifact;

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public RegistryArtifactConfig.Mutable setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    @Override
    public RegistryArtifactConfig.Mutable setArtifact(ArtifactCoords artifact) {
        this.artifact = artifact;
        return this;
    }

    @Override
    public RegistryArtifactConfig.Mutable mutable() {
        return new JsonRegistryArtifactConfig()
                .setArtifact(this.artifact)
                .setDisabled(this.disabled);
    }

    @Override
    public RegistryArtifactConfig build() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact, disabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonRegistryArtifactConfig other = (JsonRegistryArtifactConfig) obj;
        return Objects.equals(artifact, other.artifact) && disabled == other.disabled;
    }
}
