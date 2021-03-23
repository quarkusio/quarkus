package io.quarkus.registry.config;

import java.util.Map;

public interface RegistryConfig {

    String getId();

    boolean isDisabled();

    String getUpdatePolicy();

    RegistryDescriptorConfig getDescriptor();

    RegistryPlatformsConfig getPlatforms();

    RegistryNonPlatformExtensionsConfig getNonPlatformExtensions();

    RegistryMavenConfig getMaven();

    RegistryQuarkusVersionsConfig getQuarkusVersions();

    Map<String, Object> getExtra();
}
