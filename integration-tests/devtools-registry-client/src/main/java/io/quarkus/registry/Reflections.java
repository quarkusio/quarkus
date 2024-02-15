package io.quarkus.registry;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        io.quarkus.registry.catalog.CategoryImpl.class,
        io.quarkus.registry.catalog.ExtensionCatalogImpl.class,
        io.quarkus.registry.catalog.ExtensionImpl.class,
        io.quarkus.registry.catalog.ExtensionOriginImpl.class,
        io.quarkus.registry.catalog.PlatformCatalogImpl.class,
        io.quarkus.registry.catalog.PlatformImpl.class,
        io.quarkus.registry.catalog.PlatformReleaseImpl.class,
        io.quarkus.registry.catalog.PlatformReleaseVersion.class,
        io.quarkus.registry.catalog.PlatformStreamImpl.class,

        io.quarkus.registry.config.RegistriesConfigImpl.class,
        io.quarkus.registry.config.RegistryArtifactConfigImpl.class,
        io.quarkus.registry.config.RegistryConfigImpl.class,
        io.quarkus.registry.config.RegistryDescriptorConfigImpl.class,
        io.quarkus.registry.config.RegistryMavenConfigImpl.class,
        io.quarkus.registry.config.RegistryMavenRepoConfigImpl.class,
        io.quarkus.registry.config.RegistryNonPlatformExtensionsConfigImpl.class,
        io.quarkus.registry.config.RegistryPlatformsConfigImpl.class,
        io.quarkus.registry.config.RegistryQuarkusVersionsConfigImpl.class,

        io.quarkus.registry.json.JsonArtifactCoordsDeserializer.class,
        io.quarkus.registry.json.JsonArtifactCoordsMixin.class,
        io.quarkus.registry.json.JsonArtifactCoordsSerializer.class,
        io.quarkus.registry.json.JsonBooleanTrueFilter.class,
        io.quarkus.registry.json.JsonEntityWithAnySupport.class,
}, ignoreNested = true)
public class Reflections {
}
