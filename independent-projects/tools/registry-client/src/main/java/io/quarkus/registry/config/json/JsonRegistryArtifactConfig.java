package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistryArtifactConfig;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
}
