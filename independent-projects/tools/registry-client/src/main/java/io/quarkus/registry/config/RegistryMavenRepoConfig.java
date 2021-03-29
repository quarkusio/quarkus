package io.quarkus.registry.config;

/**
 * Registry Maven repository configuration.
 */
public interface RegistryMavenRepoConfig {

    /**
     * Default registry Maven repository ID.
     *
     * @return default registry Maven repository ID
     */
    String getId();

    /**
     * Registry Maven repository URL
     *
     * @return registry Maven repository URL
     */
    String getUrl();
}
