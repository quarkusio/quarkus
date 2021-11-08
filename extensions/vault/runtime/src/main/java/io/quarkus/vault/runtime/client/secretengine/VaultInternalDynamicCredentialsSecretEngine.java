package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.dynamic.VaultDynamicCredentials;

@Singleton
public class VaultInternalDynamicCredentialsSecretEngine extends VaultInternalBase {

    public VaultDynamicCredentials generateCredentials(String token, String mount, String requestPath, String role) {
        return vaultClient.get(mount + "/" + requestPath + "/" + role, token, VaultDynamicCredentials.class);
    }
}
