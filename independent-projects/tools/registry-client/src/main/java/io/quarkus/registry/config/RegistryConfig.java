package io.quarkus.registry.config;

public interface RegistryConfig {

    String getId();

    boolean isDisabled();

    String getUpdatePolicy();

    RegistryDescriptorConfig getDescriptor();

    RegistryPlatformsConfig getPlatforms();

    RegistryNonPlatformExtensionsConfig getNonPlatformExtensions();

    RegistryMavenConfig getMaven();

    RegistryQuarkusVersionsConfig getQuarkusVersions();
}
