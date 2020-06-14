package io.quarkus.vault.runtime.config;

import java.util.Arrays;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class VaultConfigSourceProvider implements ConfigSourceProvider {

    private VaultRuntimeConfig vaultRuntimeConfig;
    private VaultBuildTimeConfig vaultBuildTimeConfig;

    public VaultConfigSourceProvider(VaultBuildTimeConfig vaultBuildTimeConfig, VaultRuntimeConfig vaultRuntimeConfig) {
        this.vaultRuntimeConfig = vaultRuntimeConfig;
        this.vaultBuildTimeConfig = vaultBuildTimeConfig;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return Arrays.asList(new VaultConfigSource(150, vaultBuildTimeConfig, vaultRuntimeConfig));
    }

}
