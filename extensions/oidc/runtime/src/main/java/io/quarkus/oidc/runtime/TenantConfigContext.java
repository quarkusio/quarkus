package io.quarkus.oidc.runtime;

import javax.crypto.SecretKey;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.smallrye.jwt.util.KeyUtils;

public class TenantConfigContext {

    /**
     * OIDC Provider
     */
    final OidcProvider provider;

    /**
     * Tenant configuration
     */
    final OidcTenantConfig oidcConfig;

    /**
     * PKCE Secret Key
     */
    private final SecretKey pkceSecretKey;

    final boolean ready;

    public TenantConfigContext(OidcProvider client, OidcTenantConfig config) {
        this(client, config, true);
    }

    public TenantConfigContext(OidcProvider client, OidcTenantConfig config, boolean ready) {
        this.provider = client;
        this.oidcConfig = config;
        this.ready = ready;

        pkceSecretKey = createPkceSecretKey(config);
    }

    private static SecretKey createPkceSecretKey(OidcTenantConfig config) {
        if (config.authentication.pkceRequired.orElse(false)) {
            String pkceSecret = config.authentication.pkceSecret
                    .orElse(OidcCommonUtils.clientSecret(config.credentials));
            if (pkceSecret.length() < 32) {
                throw new RuntimeException("Secret key for encrypting PKCE code verifier must be at least 32 characters long");
            }
            return KeyUtils.createSecretKeyFromSecret(pkceSecret);
        }
        return null;
    }

    public OidcTenantConfig getOidcTenantConfig() {
        return oidcConfig;
    }

    public SecretKey getPkceSecretKey() {
        return pkceSecretKey;
    }
}
