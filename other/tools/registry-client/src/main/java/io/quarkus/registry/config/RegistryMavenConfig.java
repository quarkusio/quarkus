package io.quarkus.registry.config;

import io.quarkus.registry.json.JsonBuilder;

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

    /** @return a mutable copy of this configuration */
    default Mutable mutable() {
        return new RegistryMavenConfigImpl.Builder(this);
    }

    interface Mutable extends RegistryMavenConfig, JsonBuilder<RegistryMavenConfig> {
        Mutable setRepository(RegistryMavenRepoConfig repository);

        /** @return an immutable copy of this configuration */
        RegistryMavenConfig build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new RegistryMavenConfigImpl.Builder();
    }
}
