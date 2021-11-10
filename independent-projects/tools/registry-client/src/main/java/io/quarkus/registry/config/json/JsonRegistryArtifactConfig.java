package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistryArtifactConfig;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonRegistryArtifactConfig implements RegistryArtifactConfig {

    protected boolean disabled;
    protected ArtifactCoords artifact;

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    public void setArtifact(ArtifactCoords artifact) {
        this.artifact = artifact;
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
