package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultAuthenticationConfig {

    /**
     * Vault token, bypassing Vault authentication (kubernetes, userpass or approle). This is useful in development
     * where an authentication mode might not have been set up. In production we will usually prefer some
     * authentication such as userpass, or preferably kubernetes, where Vault tokens get generated with a TTL
     * and some ability to revoke them. Lease renewal does not apply.
     */
    @ConfigItem
    public Optional<String> clientToken;

    /**
     * Client token wrapped in a wrapping token, such as what is returned by:
     * <p>
     * vault token create -wrap-ttl=60s -policy=myapp
     * <p>
     * client-token and client-token-wrapping-token are exclusive. Lease renewal does not apply.
     */
    @ConfigItem
    public Optional<String> clientTokenWrappingToken;

    /**
     * AppRole authentication method
     * <p>
     * See https://www.vaultproject.io/api/auth/approle/index.html
     */
    @ConfigItem
    public VaultAppRoleAuthenticationConfig appRole;

    /**
     * Userpass authentication method
     * <p>
     * See https://www.vaultproject.io/api/auth/userpass/index.html
     */
    @ConfigItem
    public VaultUserpassAuthenticationConfig userpass;

    /**
     * Kubernetes authentication method
     * <p>
     * See https://www.vaultproject.io/docs/auth/kubernetes.html
     */
    @ConfigItem
    public VaultKubernetesAuthenticationConfig kubernetes;

    public boolean isDirectClientToken() {
        return clientToken.isPresent() || clientTokenWrappingToken.isPresent();
    }

    public boolean isAppRole() {
        return appRole.roleId.isPresent() && (appRole.secretId.isPresent() || appRole.secretIdWrappingToken.isPresent());
    }

    public boolean isUserpass() {
        return userpass.username.isPresent() && (userpass.password.isPresent() || userpass.passwordWrappingToken.isPresent());
    }

}
