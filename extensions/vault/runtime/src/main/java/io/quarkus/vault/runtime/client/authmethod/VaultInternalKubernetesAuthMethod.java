package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigData;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthListRolesResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthReadRoleResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthRoleData;

@Singleton
public class VaultInternalKubernetesAuthMethod extends VaultInternalBase {

    @Inject
    private VaultConfigHolder vaultConfigHolder;

    private String getKubernetesAuthMountPath() {
        return vaultConfigHolder.getVaultBootstrapConfig().authentication.kubernetes.authMountPath;
    }

    public VaultKubernetesAuth login(String role, String jwt) {
        VaultKubernetesAuthBody body = new VaultKubernetesAuthBody(role, jwt);
        return vaultClient.post(getKubernetesAuthMountPath() + "/login", null, body, VaultKubernetesAuth.class);
    }

    public void createAuthRole(String token, String name, VaultKubernetesAuthRoleData body) {
        vaultClient.post(getKubernetesAuthMountPath() + "/role/" + name, token, body, 204);
    }

    public VaultKubernetesAuthReadRoleResult getVaultAuthRole(String token, String name) {
        return vaultClient.get(getKubernetesAuthMountPath() + "/role/" + name, token, VaultKubernetesAuthReadRoleResult.class);
    }

    public VaultKubernetesAuthListRolesResult listAuthRoles(String token) {
        return vaultClient.list(getKubernetesAuthMountPath() + "/role", token, VaultKubernetesAuthListRolesResult.class);
    }

    public void deleteAuthRoles(String token, String name) {
        vaultClient.delete(getKubernetesAuthMountPath() + "/role/" + name, token, 204);
    }

    public void configureAuth(String token, VaultKubernetesAuthConfigData config) {
        vaultClient.post(getKubernetesAuthMountPath() + "/config", token, config, 204);
    }

    public VaultKubernetesAuthConfigResult readAuthConfig(String token) {
        return vaultClient.get(getKubernetesAuthMountPath() + "/config", token, VaultKubernetesAuthConfigResult.class);
    }

    public VaultInternalKubernetesAuthMethod setVaultConfigHolder(VaultConfigHolder vaultConfigHolder) {
        this.vaultConfigHolder = vaultConfigHolder;
        return this;
    }
}
