package io.quarkus.vault.test.client;

import io.quarkus.vault.runtime.client.OkHttpVaultClient;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.test.client.dto.VaultAppRoleRoleId;
import io.quarkus.vault.test.client.dto.VaultAppRoleSecretId;
import io.quarkus.vault.test.client.dto.VaultHealth;
import io.quarkus.vault.test.client.dto.VaultInit;
import io.quarkus.vault.test.client.dto.VaultInitBody;
import io.quarkus.vault.test.client.dto.VaultSealStatus;

public class TestVaultClient extends OkHttpVaultClient {

    public TestVaultClient(VaultRuntimeConfig serverConfig) {
        super(serverConfig);
    }

    public VaultInit init(int secretShares, int secretThreshold) {
        VaultInitBody body = new VaultInitBody(secretShares, secretThreshold);
        return put("sys/init", null, body, VaultInit.class);
    }

    public VaultHealth getHealth() {
        return get("sys/health", null, VaultHealth.class);
    }

    public VaultSealStatus getSealStatus() {
        return get("sys/seal-status", null, VaultSealStatus.class);
    }

    public VaultAppRoleSecretId generateAppRoleSecretId(String token, String roleName) {
        return post("auth/approle/role/" + roleName + "/secret-id", token, null, VaultAppRoleSecretId.class);
    }

    public VaultAppRoleRoleId getAppRoleRoleId(String token, String role) {
        return get("auth/approle/role/" + role + "/role-id", token, VaultAppRoleRoleId.class);
    }
}
