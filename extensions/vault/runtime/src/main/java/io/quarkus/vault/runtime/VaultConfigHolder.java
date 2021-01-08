package io.quarkus.vault.runtime;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.config.VaultBootstrapConfig;

@Singleton
public class VaultConfigHolder {

    private VaultBootstrapConfig vaultBootstrapConfig;

    public VaultBootstrapConfig getVaultBootstrapConfig() {
        return vaultBootstrapConfig;
    }

    public VaultConfigHolder setVaultBootstrapConfig(VaultBootstrapConfig vaultBootstrapConfig) {
        this.vaultBootstrapConfig = vaultBootstrapConfig;
        return this;
    }
}
