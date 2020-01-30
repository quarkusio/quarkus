package io.quarkus.vault.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import io.quarkus.vault.CredentialsProvider;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

@ApplicationScoped
public class VaultServiceProducer {

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
    @Named("vault-credentials-provider")
    public CredentialsProvider createCredentialsProvider() {
        return VaultManager.getInstance().getVaultCredentialsProvider();
    }

    @PreDestroy
    public void close() {
        VaultManager.reset();
    }

    public void setVaultRuntimeConfig(VaultRuntimeConfig serverConfig) {
        VaultManager.init(serverConfig);
    }
}
