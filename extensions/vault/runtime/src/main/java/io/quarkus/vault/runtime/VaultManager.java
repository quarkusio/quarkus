package io.quarkus.vault.runtime;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.runtime.client.OkHttpVaultClient;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class VaultManager {

    private static volatile VaultManager instance;

    private VaultRuntimeConfig serverConfig;
    private VaultBuildTimeConfig buildTimeConfig;
    private TlsConfig tlsConfig;

    private VaultClient vaultClient;
    private VaultAuthManager vaultAuthManager;
    private VaultKvManager vaultKvManager;
    private VaultDbManager vaultDbManager;
    private VaultTransitManager vaultTransitManager;
    private VaultCredentialsProvider vaultCredentialsProvider;
    private VaultTOTPManager vaultTOTPManager;
    private VaultSystemBackendManager vaultSystemBackendManager;
    private VaultKubernetesAuthManager vaultKubernetesAuthManager;

    public static VaultManager getInstance() {
        return instance;
    }

    public static void init(VaultBuildTimeConfig buildTimeConfig, VaultRuntimeConfig serverConfig, TlsConfig tlsConfig) {
        instance = new VaultManager(buildTimeConfig, serverConfig, tlsConfig);
    }

    public static void reset() {
        instance = null;
    }

    public VaultManager(VaultBuildTimeConfig vaultBuildTimeConfig, VaultRuntimeConfig serverConfig, TlsConfig tlsConfig) {
        this(vaultBuildTimeConfig, serverConfig, new OkHttpVaultClient(serverConfig, tlsConfig), tlsConfig);
    }

    public VaultManager(VaultBuildTimeConfig vaultBuildTimeConfig, VaultRuntimeConfig serverConfig, VaultClient vaultClient,
            TlsConfig tlsConfig) {
        this.serverConfig = serverConfig;
        this.vaultClient = vaultClient;
        this.buildTimeConfig = vaultBuildTimeConfig;
        this.tlsConfig = tlsConfig;
        this.vaultAuthManager = new VaultAuthManager(this.vaultClient, serverConfig);
        this.vaultKvManager = new VaultKvManager(this.vaultAuthManager, this.vaultClient, serverConfig);
        this.vaultDbManager = new VaultDbManager(this.vaultAuthManager, this.vaultClient, serverConfig);
        this.vaultTransitManager = new VaultTransitManager(this.vaultAuthManager, this.vaultClient, serverConfig);
        this.vaultCredentialsProvider = new VaultCredentialsProvider(serverConfig, this.vaultKvManager, this.vaultDbManager);
        this.vaultTOTPManager = new VaultTOTPManager(this.vaultAuthManager, this.vaultClient);
        this.vaultSystemBackendManager = new VaultSystemBackendManager(this.buildTimeConfig, this.vaultAuthManager,
                this.vaultClient);
        this.vaultKubernetesAuthManager = new VaultKubernetesAuthManager(this.vaultAuthManager, this.vaultClient);
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

    public VaultTOTPManager getVaultTOTPManager() {
        return vaultTOTPManager;
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

    public VaultBuildTimeConfig getBuildTimeConfig() {
        return buildTimeConfig;
    }

    public TlsConfig getTlsConfig() {
        return tlsConfig;
    }

    public VaultSystemBackendManager getVaultSystemBackendManager() {
        return vaultSystemBackendManager;
    }

    public VaultKubernetesAuthManager getVaultKubernetesAuthManager() {
        return vaultKubernetesAuthManager;
    }
}
