package io.quarkus.vault.runtime;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultKubernetesAuthService;
import io.quarkus.vault.auth.VaultKubernetesAuthConfig;
import io.quarkus.vault.auth.VaultKubernetesAuthRole;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalKubernetesAuthMethod;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigData;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthRoleData;

@ApplicationScoped
public class VaultKubernetesAuthManager implements VaultKubernetesAuthService {

    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultInternalKubernetesAuthMethod vaultInternalKubernetesAuthMethod;

    @Override
    public void configure(VaultKubernetesAuthConfig config) {
        String token = vaultAuthManager.getClientToken();
        vaultInternalKubernetesAuthMethod.configureAuth(token, new VaultKubernetesAuthConfigData()
                .setIssuer(config.issuer)
                .setKubernetesCaCert(config.kubernetesCaCert)
                .setKubernetesHost(config.kubernetesHost)
                .setPemKeys(config.pemKeys)
                .setTokenReviewerJwt(config.tokenReviewerJwt));
    }

    @Override
    public VaultKubernetesAuthConfig getConfig() {
        String token = vaultAuthManager.getClientToken();
        VaultKubernetesAuthConfigData data = vaultInternalKubernetesAuthMethod.readAuthConfig(token).data;
        return new VaultKubernetesAuthConfig()
                .setKubernetesCaCert(data.kubernetesCaCert)
                .setKubernetesHost(data.kubernetesHost)
                .setIssuer(data.issuer)
                .setPemKeys(data.pemKeys)
                .setTokenReviewerJwt(data.tokenReviewerJwt);
    }

    public VaultKubernetesAuthRole getRole(String name) {
        String token = vaultAuthManager.getClientToken();
        VaultKubernetesAuthRoleData role = vaultInternalKubernetesAuthMethod.getVaultAuthRole(token, name).data;
        return new VaultKubernetesAuthRole()
                .setBoundServiceAccountNames(role.boundServiceAccountNames)
                .setBoundServiceAccountNamespaces(role.boundServiceAccountNamespaces)
                .setAudience(role.audience)
                .setTokenTtl(role.tokenTtl)
                .setTokenMaxTtl(role.tokenMaxTtl)
                .setTokenPolicies(role.tokenPolicies)
                .setTokenBoundCidrs(role.tokenBoundCidrs)
                .setTokenExplicitMaxTtl(role.tokenExplicitMaxTtl)
                .setTokenNoDefaultPolicy(role.tokenNoDefaultPolicy)
                .setTokenNumUses(role.tokenNumUses)
                .setTokenPeriod(role.tokenPeriod)
                .setTokenType(role.tokenType);
    }

    public void createRole(String name, VaultKubernetesAuthRole role) {
        String token = vaultAuthManager.getClientToken();
        VaultKubernetesAuthRoleData body = new VaultKubernetesAuthRoleData()
                .setBoundServiceAccountNames(role.boundServiceAccountNames)
                .setBoundServiceAccountNamespaces(role.boundServiceAccountNamespaces)
                .setAudience(role.audience)
                .setTokenTtl(role.tokenTtl)
                .setTokenMaxTtl(role.tokenMaxTtl)
                .setTokenPolicies(role.tokenPolicies)
                .setTokenBoundCidrs(role.tokenBoundCidrs)
                .setTokenExplicitMaxTtl(role.tokenExplicitMaxTtl)
                .setTokenNoDefaultPolicy(role.tokenNoDefaultPolicy)
                .setTokenNumUses(role.tokenNumUses)
                .setTokenPeriod(role.tokenPeriod)
                .setTokenType(role.tokenType);
        vaultInternalKubernetesAuthMethod.createAuthRole(token, name, body);
    }

    @Override
    public List<String> getRoles() {
        try {
            String token = vaultAuthManager.getClientToken();
            return vaultInternalKubernetesAuthMethod.listAuthRoles(token).data.keys;
        } catch (VaultClientException e) {
            if (e.getStatus() == 404) {
                return Collections.emptyList();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void deleteRole(String name) {
        String token = vaultAuthManager.getClientToken();
        vaultInternalKubernetesAuthMethod.deleteAuthRoles(token, name);
    }
}
