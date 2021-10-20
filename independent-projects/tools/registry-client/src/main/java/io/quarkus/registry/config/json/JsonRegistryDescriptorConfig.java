package io.quarkus.registry.config.json;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import java.util.Objects;

@Deprecated
public class JsonRegistryDescriptorConfig implements RegistryDescriptorConfig.Mutable {

    private ArtifactCoords artifact;

    @Override
    public ArtifactCoords getArtifact() {
        return artifact;
    }

    public Mutable setArtifact(ArtifactCoords artifact) {
        this.artifact = artifact;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonRegistryDescriptorConfig other = (JsonRegistryDescriptorConfig) obj;
        return Objects.equals(artifact, other.artifact);
    }

    public Mutable mutable() {
        return new JsonRegistryDescriptorConfig()
                .setArtifact(artifact);
    }

    public RegistryDescriptorConfig build() {
        return this;
    }
}
