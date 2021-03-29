package io.quarkus.vault.runtime;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV1SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV2SecretEngine;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;

@ApplicationScoped
public class VaultKvManager implements VaultKVSecretEngine {

    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultConfigHolder vaultConfigHolder;
    @Inject
    private VaultInternalKvV1SecretEngine vaultInternalKvV1SecretEngine;
    @Inject
    private VaultInternalKvV2SecretEngine vaultInternalKvV2SecretEngine;

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    @Override
    public Map<String, String> readSecret(String path) {

        String clientToken = vaultAuthManager.getClientToken();
        String mount = getConfig().kvSecretEngineMountPath;

        if (isV1()) {
            VaultKvSecretV1 secretV1 = vaultInternalKvV1SecretEngine.getSecret(clientToken, mount, path);
            return secretV1.data;
        } else {
            VaultKvSecretV2 secretV2 = vaultInternalKvV2SecretEngine.getSecret(clientToken, mount, path);
            return secretV2.data.data;
        }
    }

    @Override
    public void writeSecret(String path, Map<String, String> secret) {

        String clientToken = vaultAuthManager.getClientToken();
        String mount = getConfig().kvSecretEngineMountPath;

        if (isV1()) {
            vaultInternalKvV1SecretEngine.writeSecret(clientToken, mount, path, secret);
        } else {
            VaultKvSecretV2WriteBody body = new VaultKvSecretV2WriteBody();
            body.data = secret;
            vaultInternalKvV2SecretEngine.writeSecret(clientToken, mount, path, body);
        }
    }

    @Override
    public void deleteSecret(String path) {
        String clientToken = vaultAuthManager.getClientToken();
        String mount = getConfig().kvSecretEngineMountPath;

        if (isV1()) {
            vaultInternalKvV1SecretEngine.deleteSecret(clientToken, mount, path);
        } else {
            vaultInternalKvV2SecretEngine.deleteSecret(clientToken, mount, path);
        }
    }

    @Override
    public List<String> listSecrets(String path) {
        String clientToken = vaultAuthManager.getClientToken();
        String mount = getConfig().kvSecretEngineMountPath;

        return (isV1()
                ? vaultInternalKvV1SecretEngine.listSecrets(clientToken, mount, path)
                : vaultInternalKvV2SecretEngine.listSecrets(clientToken, mount, path)).data.keys;
    }

    private boolean isV1() {
        return getConfig().kvSecretEngineVersion == 1;
    }
}
