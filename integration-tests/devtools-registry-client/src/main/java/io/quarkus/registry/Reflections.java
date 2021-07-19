package io.quarkus.registry;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        io.quarkus.registry.catalog.json.JsonArtifactCoordsDeserializer.class,
        io.quarkus.registry.catalog.json.JsonArtifactCoordsMixin.class,
        io.quarkus.registry.catalog.json.JsonArtifactCoordsSerializer.class,
        io.quarkus.registry.catalog.json.JsonCategory.class,
        io.quarkus.registry.catalog.json.JsonExtension.class,
        io.quarkus.registry.catalog.json.JsonExtensionCatalog.class,
        io.quarkus.registry.catalog.json.JsonExtensionOrigin.class,
        io.quarkus.registry.catalog.json.JsonPlatform.class,
        io.quarkus.registry.catalog.json.JsonPlatformCatalog.class,
        io.quarkus.registry.catalog.json.JsonPlatformRelease.class,
        io.quarkus.registry.catalog.json.JsonPlatformReleaseVersion.class,
        io.quarkus.registry.catalog.json.JsonPlatformReleaseVersionDeserializer.class,
        io.quarkus.registry.catalog.json.JsonPlatformReleaseVersionSerializer.class,
        io.quarkus.registry.catalog.json.JsonPlatformStream.class,

        io.quarkus.registry.config.json.JsonBooleanTrueFilter.class,
        io.quarkus.registry.config.json.JsonRegistriesConfig.class,
        io.quarkus.registry.config.json.JsonRegistryArtifactConfig.class,
        io.quarkus.registry.config.json.JsonRegistryConfig.class,
        io.quarkus.registry.config.json.JsonRegistryConfigDeserializer.class,
        io.quarkus.registry.config.json.JsonRegistryConfigSerializer.class,
        io.quarkus.registry.config.json.JsonRegistryDescriptorConfig.class,
        io.quarkus.registry.config.json.JsonRegistryMavenConfig.class,
        io.quarkus.registry.config.json.JsonRegistryMavenRepoConfig.class,
        io.quarkus.registry.config.json.JsonRegistryNonPlatformExtensionsConfig.class,
        io.quarkus.registry.config.json.JsonRegistryPlatformsConfig.class,
        io.quarkus.registry.config.json.JsonRegistryQuarkusVersionsConfig.class
})
public class Reflections {
}
