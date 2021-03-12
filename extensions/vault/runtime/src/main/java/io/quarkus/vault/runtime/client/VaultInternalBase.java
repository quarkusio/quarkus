package io.quarkus.vault.runtime.client;

import javax.inject.Inject;

public abstract class VaultInternalBase {

    @Inject
    protected VaultClient vaultClient;

    public VaultInternalBase setVaultClient(VaultClient vaultClient) {
        this.vaultClient = vaultClient;
        return this;
    }
}
