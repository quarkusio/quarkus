package io.quarkus.registry.config;

import io.quarkus.registry.json.JsonBuilder;

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

    /** @return a mutable copy of this configuration */
    default Mutable mutable() {
        return new RegistryMavenRepoConfigImpl.Builder(this);
    }

    interface Mutable extends RegistryMavenRepoConfig, JsonBuilder<RegistryMavenRepoConfig> {
        Mutable setId(String id);

        Mutable setUrl(String url);

        /** @return an immutable copy of this configuration */
        RegistryMavenRepoConfig build();
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new RegistryMavenRepoConfigImpl.Builder();
    }
}
