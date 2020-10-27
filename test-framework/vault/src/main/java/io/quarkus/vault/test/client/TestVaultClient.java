package io.quarkus.vault.test.client;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.runtime.VaultManager;
import io.quarkus.vault.runtime.client.OkHttpVaultClient;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRandomBody;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.test.client.dto.VaultAppRoleRoleId;
import io.quarkus.vault.test.client.dto.VaultAppRoleSecretId;
import io.quarkus.vault.test.client.dto.VaultTransitHash;
import io.quarkus.vault.test.client.dto.VaultTransitHashBody;
import io.quarkus.vault.test.client.dto.VaultTransitRandom;

public class TestVaultClient extends OkHttpVaultClient {

    public TestVaultClient() {
        this(VaultManager.getInstance().getServerConfig(), VaultManager.getInstance().getTlsConfig());
    }

    public TestVaultClient(VaultRuntimeConfig serverConfig, TlsConfig tlsConfig) {
        super(serverConfig, tlsConfig);
    }

    public VaultAppRoleSecretId generateAppRoleSecretId(String token, String roleName) {
        return post("auth/approle/role/" + roleName + "/secret-id", token, null, VaultAppRoleSecretId.class);
    }

    public VaultAppRoleRoleId getAppRoleRoleId(String token, String role) {
        return get("auth/approle/role/" + role + "/role-id", token, VaultAppRoleRoleId.class);
    }

    public void rotate(String token, String keyName) {
        post("transit/keys/" + keyName + "/rotate", token, null, null, 204);
    }

    public VaultTransitRandom generateRandom(String token, int byteCount, VaultTransitRandomBody body) {
        return post("transit/random/" + byteCount, token, body, VaultTransitRandom.class);
    }

    public VaultTransitHash hash(String token, String hashAlgorithm, VaultTransitHashBody body) {
        return post("transit/hash/" + hashAlgorithm, token, body, VaultTransitHash.class);
    }
}
