package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvListSecrets;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2Write;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;

@Singleton
public class VaultInternalKvV2SecretEngine extends VaultInternalBase {

    public VaultKvSecretV2 getSecret(String token, String secretEnginePath, String path) {
        return vaultClient.get(secretEnginePath + "/data/" + path, token, VaultKvSecretV2.class);
    }

    public void writeSecret(String token, String secretEnginePath, String path, VaultKvSecretV2WriteBody body) {
        vaultClient.post(secretEnginePath + "/data/" + path, token, body, VaultKvSecretV2Write.class);
    }

    public void deleteSecret(String token, String secretEnginePath, String path) {
        vaultClient.delete(secretEnginePath + "/data/" + path, token, 204);
    }

    public VaultKvListSecrets listSecrets(String token, String secretEnginePath, String path) {
        return vaultClient.list(secretEnginePath + "/metadata/" + path, token, VaultKvListSecrets.class);
    }
}
