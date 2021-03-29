package io.quarkus.registry.config;

/**
 * Configuration related to the resolution of catalogs of available platforms.
 */
public interface RegistryPlatformsConfig extends RegistryArtifactConfig {

    /**
     * Whether the client should send requests to resolve the platform extension catalogs (platform descriptors)
     * to the registry or resolve them from Maven Central directly instead.
     * Returning <code>null</code> from this method will be equivalent to returning <code>false</code>, in which case
     * the client will not send requests to resolve platform extension catalogs to the registry.
     *
     * @return true if the registry will be able to handle platform descriptor requests, otherwise - false
     */
    Boolean getExtensionCatalogsIncluded();
}
