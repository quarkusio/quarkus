package io.quarkus.vault.runtime;

import io.quarkus.vault.runtime.client.OkHttpVaultClient;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class VaultManager {

    private static volatile VaultManager instance;

    private VaultRuntimeConfig serverConfig;
    private VaultClient vaultClient;
    private VaultAuthManager vaultAuthManager;
    private VaultKvManager vaultKvManager;
    private VaultDbManager vaultDbManager;
    private VaultTransitManager vaultTransitManager;
    private VaultCredentialsProvider vaultCredentialsProvider;

    public static VaultManager getInstance() {
        return instance;
    }

    public static void init(VaultRuntimeConfig serverConfig) {
        if (instance == null) {
            instance = new VaultManager(serverConfig);
        }
    }

    public static void reset() {
        instance = null;
    }

    public VaultManager(VaultRuntimeConfig serverConfig) {
        this(serverConfig, new OkHttpVaultClient(serverConfig));
    }

    public VaultManager(VaultRuntimeConfig serverConfig, VaultClient vaultClient) {
        this.serverConfig = serverConfig;
        this.vaultClient = vaultClient;
        this.vaultAuthManager = new VaultAuthManager(this.vaultClient, serverConfig);
        this.vaultKvManager = new VaultKvManager(this.vaultAuthManager, this.vaultClient, serverConfig);
        this.vaultDbManager = new VaultDbManager(this.vaultAuthManager, this.vaultClient, serverConfig);
        this.vaultTransitManager = new VaultTransitManager(this.vaultAuthManager, this.vaultClient, serverConfig);
        this.vaultCredentialsProvider = new VaultCredentialsProvider(serverConfig, this.vaultKvManager, this.vaultDbManager);
    }

    public VaultClient getVaultClient() {
        return vaultClient;
    }

    public VaultAuthManager getVaultAuthManager() {
        return vaultAuthManager;
    }

    public VaultDbManager getVaultDbManager() {
        return vaultDbManager;
    }

    public VaultKvManager getVaultKvManager() {
        return vaultKvManager;
    }

    public VaultTransitManager getVaultTransitManager() {
        return vaultTransitManager;
    }

    public VaultCredentialsProvider getVaultCredentialsProvider() {
        return vaultCredentialsProvider;
    }

    public VaultRuntimeConfig getServerConfig() {
        return serverConfig;
    }
}
