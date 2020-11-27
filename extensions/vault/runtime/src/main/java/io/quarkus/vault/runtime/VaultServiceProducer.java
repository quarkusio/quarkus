package io.quarkus.vault.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.VaultSystemBackendEngine;
import io.quarkus.vault.VaultTOTPSecretEngine;
import io.quarkus.vault.VaultTransitSecretEngine;

@ApplicationScoped
public class VaultServiceProducer {

    @Produces
    @ApplicationScoped
    public VaultSystemBackendEngine createVaultSystemBackendEngine() {
        return VaultManager.getInstance().getVaultSystemBackendManager();
    }

    @Produces
    @ApplicationScoped
    public VaultKVSecretEngine createKVSecretEngine() {
        return VaultManager.getInstance().getVaultKvManager();
    }

    @Produces
    @ApplicationScoped
    public VaultTransitSecretEngine createTransitSecretEngine() {
        return VaultManager.getInstance().getVaultTransitManager();
    }

    @Produces
    @ApplicationScoped
    public VaultTOTPSecretEngine createVaultTOTPSecretEngine() {
        return VaultManager.getInstance().getVaultTOTPManager();
    }

    @Produces
    @ApplicationScoped
    public VaultKubernetesAuthManager createVaultKubernetesAuthManager() {
        return VaultManager.getInstance().getVaultKubernetesAuthManager();
    }

    @Produces
    @ApplicationScoped
    @Named("vault-credentials-provider")
    public CredentialsProvider createCredentialsProvider() {
        return VaultManager.getInstance().getVaultCredentialsProvider();
    }

    @PreDestroy
    public void close() {
        VaultManager.reset();
    }
}
