package io.quarkus.vault.runtime.config;

import java.util.Arrays;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

public class VaultConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(VaultConfigSourceProvider.class);

    private VaultBootstrapConfig vaultBootstrapConfig;

    public VaultConfigSourceProvider(VaultBootstrapConfig vaultBootstrapConfig) {
        this.vaultBootstrapConfig = vaultBootstrapConfig;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return Arrays.asList(new VaultConfigSource(vaultBootstrapConfig));
    }
}
