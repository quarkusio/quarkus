package io.quarkus.registry.config.json;

import io.quarkus.registry.config.RegistryPlatformsConfig;

public class JsonRegistryPlatformsConfig extends JsonRegistryArtifactConfig implements RegistryPlatformsConfig {

    private Boolean extensionCatalogsIncluded;

    @Override
    public Boolean getExtensionCatalogsIncluded() {
        return extensionCatalogsIncluded;
    }

    public void setExtensionCatalogsIncluded(Boolean extensionCatalogsIncluded) {
        this.extensionCatalogsIncluded = extensionCatalogsIncluded;
    }
}
