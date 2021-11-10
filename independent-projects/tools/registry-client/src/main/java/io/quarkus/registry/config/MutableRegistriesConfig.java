package io.quarkus.registry.config;

import java.io.IOException;
import java.nio.file.Path;

public interface MutableRegistriesConfig extends RegistriesConfig {

    boolean addRegistry(String registryId);

    boolean addRegistry(RegistryConfig config);

    boolean removeRegistry(String registryId);

    /**
     * Persist this configuration to the specified file.
     * 
     * @param targetFile Target file
     * @return an immutable form of this configuration.
     * @throws IOException if the specified file can not be written to.
     */
    void persist(Path targetFile) throws IOException;

    /**
     * Persist this configuration to the original source (if possible).
     * Does nothing for configurations read from environment variables.
     *
     * @return an immutable form of this configuration.
     * @throws IOException if the source file can not be written to.
     */
    void persist() throws IOException;
}
