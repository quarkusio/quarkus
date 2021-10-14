package io.quarkus.registry.config.json;

import io.quarkus.registry.config.RegistryPlatformsConfig;
import java.util.Objects;

@Deprecated
public class JsonRegistryPlatformsConfig extends JsonRegistryArtifactConfig implements RegistryPlatformsConfig {

    private Boolean extensionCatalogsIncluded;

    @Override
    public Boolean getExtensionCatalogsIncluded() {
        return extensionCatalogsIncluded;
    }

    public void setExtensionCatalogsIncluded(Boolean extensionCatalogsIncluded) {
        this.extensionCatalogsIncluded = extensionCatalogsIncluded;
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
}
