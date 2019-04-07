package io.quarkus.vault.runtime;

import java.util.Map;

import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class VaultKvManager implements VaultKVSecretEngine {

    private VaultAuthManager vaultAuthManager;
    private VaultClient vaultClient;
    private VaultRuntimeConfig serverConfig;

    public VaultKvManager(VaultAuthManager vaultAuthManager, VaultClient vaultClient, VaultRuntimeConfig serverConfig) {
        this.vaultAuthManager = vaultAuthManager;
        this.vaultClient = vaultClient;
        this.serverConfig = serverConfig;
    }

    @Override
    public Map<String, String> readSecret(String path) {

        String clientToken = vaultAuthManager.getClientToken();
        String mount = serverConfig.kvSecretEngineMountPath;

        if (serverConfig.kvSecretEngineVersion == 1) {
            return vaultClient.getSecretV1(clientToken, mount, path).data;
        } else {
            return vaultClient.getSecretV2(clientToken, mount, path).data.data;
        }
    }

}
