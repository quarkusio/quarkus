package io.quarkus.oidc.runtime;

import java.nio.charset.StandardCharsets;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.smallrye.jwt.util.KeyUtils;

public class TenantConfigContext {
    private static final Logger LOG = Logger.getLogger(TenantConfigContext.class);

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

    /**
     * Token Encryption Secret Key
     */
    private final SecretKey tokenEncSecretKey;

    final boolean ready;

    public TenantConfigContext(OidcProvider client, OidcTenantConfig config) {
        this(client, config, true);
    }

    public TenantConfigContext(OidcProvider client, OidcTenantConfig config, boolean ready) {
        this.provider = client;
        this.oidcConfig = config;
        this.ready = ready;

        pkceSecretKey = provider != null && provider.client != null ? createPkceSecretKey(config) : null;
        tokenEncSecretKey = provider != null && provider.client != null ? createTokenEncSecretKey(config) : null;
    }

    private static SecretKey createPkceSecretKey(OidcTenantConfig config) {
        if (config.authentication.pkceRequired.orElse(false)) {
            String pkceSecret = config.authentication.pkceSecret
                    .orElse(OidcCommonUtils.clientSecret(config.credentials));
            if (pkceSecret == null) {
                throw new RuntimeException("Secret key for encrypting PKCE code verifier is missing");
            }
            if (pkceSecret.length() != 32) {
                throw new RuntimeException("Secret key for encrypting PKCE code verifier must be 32 characters long");
            }
            return KeyUtils.createSecretKeyFromSecret(pkceSecret);
        }
        return null;
    }

    private static SecretKey createTokenEncSecretKey(OidcTenantConfig config) {
        if (config.tokenStateManager.encryptionRequired) {
            String encSecret = config.tokenStateManager.encryptionSecret
                    .orElse(OidcCommonUtils.clientSecret(config.credentials));
            try {
                if (encSecret == null) {
                    LOG.warn("Secret key for encrypting tokens is missing, auto-generating it");
                    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                    keyGenerator.init(256);
                    return keyGenerator.generateKey();
                }
                byte[] secretBytes = encSecret.getBytes(StandardCharsets.UTF_8);
                if (secretBytes.length < 32) {
                    LOG.warn("Secret key for encrypting tokens should be 32 characters long");
                }
                return new SecretKeySpec(OidcUtils.getSha256Digest(secretBytes), "AES");
            } catch (Exception ex) {
                throw new OIDCException(ex);
            }
        }
        return null;
    }

    public OidcTenantConfig getOidcTenantConfig() {
        return oidcConfig;
    }

    public SecretKey getPkceSecretKey() {
        return pkceSecretKey;
    }

    public SecretKey getTokenEncSecretKey() {
        return tokenEncSecretKey;
    }
}
