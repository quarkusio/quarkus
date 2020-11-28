package io.quarkus.vault.runtime;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;

@ApplicationScoped
public class VaultKvManager implements VaultKVSecretEngine {

    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultClient vaultClient;
    @Inject
    private VaultConfigHolder vaultConfigHolder;

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    @Override
    public Map<String, String> readSecret(String path) {

        String clientToken = vaultAuthManager.getClientToken();
        String mount = getConfig().kvSecretEngineMountPath;

        if (getConfig().kvSecretEngineVersion == 1) {
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
        String mount = getConfig().kvSecretEngineMountPath;

        if (getConfig().kvSecretEngineVersion == 1) {
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
        String mount = getConfig().kvSecretEngineMountPath;

        if (getConfig().kvSecretEngineVersion == 1) {
            vaultClient.deleteSecretV1(clientToken, mount, path);
        } else {
            vaultClient.deleteSecretV2(clientToken, mount, path);
        }
    }
}
