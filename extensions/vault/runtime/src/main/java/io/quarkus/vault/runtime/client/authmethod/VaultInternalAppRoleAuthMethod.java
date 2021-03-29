package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthBody;

@Singleton
public class VaultInternalAppRoleAuthMethod extends VaultInternalBase {

    public VaultAppRoleAuth login(String roleId, String secretId) {
        VaultAppRoleAuthBody body = new VaultAppRoleAuthBody(roleId, secretId);
        return vaultClient.post("auth/approle/login", null, body, VaultAppRoleAuth.class);
    }
}
