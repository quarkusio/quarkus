package io.quarkus.registry.config.json;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import java.util.Objects;

@Deprecated
public class JsonRegistryPlatformsConfig extends JsonRegistryArtifactConfig implements RegistryPlatformsConfig.Mutable {

    private Boolean extensionCatalogsIncluded;

    @Override
    public RegistryPlatformsConfig.Mutable setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        return this;
    }

    @Override
    public RegistryPlatformsConfig.Mutable setArtifact(ArtifactCoords artifact) {
        super.setArtifact(artifact);
        return this;
    }

    @Override
    public Boolean getExtensionCatalogsIncluded() {
        return extensionCatalogsIncluded;
    }

    public RegistryPlatformsConfig.Mutable setExtensionCatalogsIncluded(Boolean extensionCatalogsIncluded) {
        this.extensionCatalogsIncluded = extensionCatalogsIncluded;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(extensionCatalogsIncluded);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonRegistryPlatformsConfig other = (JsonRegistryPlatformsConfig) obj;
        return Objects.equals(extensionCatalogsIncluded, other.extensionCatalogsIncluded);
    }

    @Override
    public RegistryPlatformsConfig.Mutable mutable() {
        return new JsonRegistryPlatformsConfig()
                .setArtifact(this.artifact)
                .setExtensionCatalogsIncluded(this.extensionCatalogsIncluded)
                .setDisabled(this.disabled);
    }

    @Override
    public RegistryPlatformsConfig build() {
        return this;
    }
}
