package io.quarkus.vault.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultPKISecretEngine;
import io.quarkus.vault.VaultPKISecretEngineFactory;
import io.quarkus.vault.pki.EnableEngineOptions;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.sys.VaultEnableEngineBody;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalPKISecretEngine;

@ApplicationScoped
public class VaultPKIManagerFactory implements VaultPKISecretEngineFactory {

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

    @Override
    public void enable(String mount, String description, EnableEngineOptions options) {
        VaultEnableEngineBody body = new VaultEnableEngineBody();
        body.type = "pki";
        body.description = description;
        body.config = new VaultEnableEngineBody.Config();
        body.config.defaultLeaseTimeToLive = options.defaultLeaseTimeToLive;
        body.config.maxLeaseTimeToLive = options.maxLeaseTimeToLive;

        vaultInternalSystemBackend.enableEngine(vaultAuthManager.getClientToken(), mount, body);
    }

    @Override
    public void disable(String mount) {
        vaultInternalSystemBackend.disableEngine(vaultAuthManager.getClientToken(), mount);
    }

    @Override
    public boolean isEnabled(String mount) {
        return vaultInternalPKISecretEngine.checkEngineEnabled(vaultAuthManager.getClientToken(), mount);
    }
}
