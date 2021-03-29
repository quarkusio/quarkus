package io.quarkus.registry.config;

/**
 * Registry Maven related configuration the client should use
 * to communicate with the registry.
 */
public interface RegistryMavenConfig {

    /**
     * Registry Maven repository configuration.
     *
     * @return registry Maven repository configuration
     */
    RegistryMavenRepoConfig getRepository();
}
