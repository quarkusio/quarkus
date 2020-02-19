package io.quarkus.vault.runtime;

import java.util.Map;

import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
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
            VaultKvSecretV1 secretV1 = vaultClient.getSecretV1(clientToken, mount, path);
            return secretV1.data;
        } else {
            VaultKvSecretV2 secretV2 = vaultClient.getSecretV2(clientToken, mount, path);
            return secretV2.data.data;
        }
    }

    @Override
    public void writeSecret(String path, Map<String, String> secret) {

        String clientToken = vaultAuthManager.getClientToken();
        String mount = serverConfig.kvSecretEngineMountPath;

        if (serverConfig.kvSecretEngineVersion == 1) {
            vaultClient.writeSecretV1(clientToken, mount, path, secret);
        } else {
            VaultKvSecretV2WriteBody body = new VaultKvSecretV2WriteBody();
            body.data = secret;
            vaultClient.writeSecretV2(clientToken, mount, path, body);
        }
    }

    @Override
    public void deleteSecret(String path) {
        String clientToken = vaultAuthManager.getClientToken();
        String mount = serverConfig.kvSecretEngineMountPath;

        if (serverConfig.kvSecretEngineVersion == 1) {
            vaultClient.deleteSecretV1(clientToken, mount, path);
        } else {
            vaultClient.deleteSecretV2(clientToken, mount, path);
        }
    }
}
