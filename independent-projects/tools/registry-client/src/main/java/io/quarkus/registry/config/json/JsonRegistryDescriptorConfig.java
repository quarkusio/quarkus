package io.quarkus.registry.config.json;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistryDescriptorConfig;

public class JsonRegistryDescriptorConfig implements RegistryDescriptorConfig {

    private ArtifactCoords artifact;

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    public void setArtifact(ArtifactCoords artifact) {
        this.artifact = artifact;
    }
}
