package io.quarkus.vault.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultPKISecretEngine;
import io.quarkus.vault.VaultPKISecretEngineFactory;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalPKISecretEngine;

@ApplicationScoped
public class VaultPKIManagerFactory implements VaultPKISecretEngineFactory {

    static final String PKI_ENGINE_NAME = "pki";

    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultInternalPKISecretEngine vaultInternalPKISecretEngine;
    @Inject
    private VaultInternalSystemBackend vaultInternalSystemBackend;

    @Override
    public VaultPKISecretEngine engine(String mount) {
        return new VaultPKIManager(mount, vaultAuthManager, vaultInternalPKISecretEngine);
    }
}
