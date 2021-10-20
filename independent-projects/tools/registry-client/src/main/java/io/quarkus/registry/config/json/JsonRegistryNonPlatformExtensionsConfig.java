package io.quarkus.registry.config.json;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistryNonPlatformExtensionsConfig;

@Deprecated
public class JsonRegistryNonPlatformExtensionsConfig extends JsonRegistryArtifactConfig
        implements RegistryNonPlatformExtensionsConfig.Mutable {

    @Override
    public RegistryNonPlatformExtensionsConfig.Mutable setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        return this;
    }

    @Override
    public RegistryNonPlatformExtensionsConfig.Mutable setArtifact(ArtifactCoords artifact) {
        super.setArtifact(artifact);
        return this;
    }

    public RegistryNonPlatformExtensionsConfig.Mutable mutable() {
        return new JsonRegistryNonPlatformExtensionsConfig()
                .setArtifact(this.artifact)
                .setDisabled(this.disabled);
    }

    public JsonRegistryNonPlatformExtensionsConfig build() {
        return this;
    }
}
