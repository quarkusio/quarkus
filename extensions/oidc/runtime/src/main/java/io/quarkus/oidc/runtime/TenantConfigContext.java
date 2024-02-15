package io.quarkus.oidc.runtime;

import java.nio.charset.StandardCharsets;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.runtime.configuration.ConfigurationException;

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
    private final SecretKey stateSecretKey;

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

        boolean isService = OidcUtils.isServiceApp(config);
        stateSecretKey = !isService && provider != null && provider.client != null ? createStateSecretKey(config) : null;
        tokenEncSecretKey = !isService && provider != null && provider.client != null ? createTokenEncSecretKey(config) : null;
    }

    private static SecretKey createStateSecretKey(OidcTenantConfig config) {
        if (config.authentication.pkceRequired.orElse(false) || config.authentication.nonceRequired) {
            String stateSecret = null;
            if (config.authentication.pkceSecret.isPresent() && config.authentication.getStateSecret().isPresent()) {
                throw new ConfigurationException(
                        "Both 'quarkus.oidc.authentication.state-secret' and 'quarkus.oidc.authentication.pkce-secret' are configured");
            }
            if (config.authentication.getStateSecret().isPresent()) {
                stateSecret = config.authentication.getStateSecret().get();
            } else if (config.authentication.pkceSecret.isPresent()) {
                stateSecret = config.authentication.pkceSecret.get();
            }

            if (stateSecret == null) {
                LOG.debug("'quarkus.oidc.authentication.state-secret' is not configured, "
                        + "trying to use the configured client secret");
                String possiblePkceSecret = fallbackToClientSecret(config);
                if (possiblePkceSecret != null && possiblePkceSecret.length() < 32) {
                    LOG.debug("Client secret is less than 32 characters long, the state secret will be generated");
                } else {
                    stateSecret = possiblePkceSecret;
                }
            }
            try {
                if (stateSecret == null) {
                    LOG.debug("Secret key for encrypting state cookie is missing, auto-generating it");
                    SecretKey key = generateSecretKey();
                    return key;
                }
                byte[] secretBytes = stateSecret.getBytes(StandardCharsets.UTF_8);
                if (secretBytes.length < 32) {
                    String errorMessage = "Secret key for encrypting state cookie should be at least 32 characters long"
                            + " for the strongest state cookie encryption to be produced."
                            + " Please update 'quarkus.oidc.authentication.state-secret' or update the configured client secret.";
                    if (secretBytes.length < 16) {
                        throw new ConfigurationException(
                                "Secret key for encrypting state cookie is less than 16 characters long");
                    } else {
                        LOG.debug(errorMessage);
                    }
                }
                return new SecretKeySpec(OidcUtils.getSha256Digest(secretBytes), "AES");
            } catch (Exception ex) {
                throw new OIDCException(ex);
            }
        }
        return null;
    }

    private static SecretKey createTokenEncSecretKey(OidcTenantConfig config) {
        if (config.tokenStateManager.encryptionRequired) {
            String encSecret = null;
            if (config.tokenStateManager.encryptionSecret.isPresent()) {
                encSecret = config.tokenStateManager.encryptionSecret.get();
            } else {
                LOG.debug("'quarkus.oidc.token-state-manager.encryption-secret' is not configured, "
                        + "trying to use the configured client secret");
                encSecret = fallbackToClientSecret(config);
            }
            try {
                if (encSecret == null) {
                    LOG.warn("Secret key for encrypting tokens in a session cookie is missing, auto-generating it");
                    return generateSecretKey();
                }
                byte[] secretBytes = encSecret.getBytes(StandardCharsets.UTF_8);
                if (secretBytes.length < 32) {
                    String errorMessage = "Secret key for encrypting tokens in a session cookie should be at least 32 characters long"
                            + " for the strongest cookie encryption to be produced."
                            + " Please configure 'quarkus.oidc.token-state-manager.encryption-secret'"
                            + " or update the configured client secret. You can disable the session cookie"
                            + " encryption with 'quarkus.oidc.token-state-manager.encryption-required=false'"
                            + " but only if it is considered to be safe in your application's network.";
                    if (secretBytes.length < 16) {
                        LOG.warn(errorMessage);
                    } else {
                        LOG.debug(errorMessage);
                    }
                }
                return new SecretKeySpec(OidcUtils.getSha256Digest(secretBytes), "AES");
            } catch (Exception ex) {
                throw new OIDCException(ex);
            }
        }
        return null;
    }

    private static String fallbackToClientSecret(OidcTenantConfig config) {
        String encSecret = OidcCommonUtils.clientSecret(config.credentials);
        if (encSecret == null) {
            LOG.debug("Client secret is not configured, "
                    + "trying to use the configured 'client_jwt_secret' secret");
            encSecret = OidcCommonUtils.jwtSecret(config.credentials);
        }
        return encSecret;
    }

    private static SecretKey generateSecretKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    public OidcTenantConfig getOidcTenantConfig() {
        return oidcConfig;
    }

    public OidcConfigurationMetadata getOidcMetadata() {
        return provider != null ? provider.getMetadata() : null;
    }

    public OidcProviderClient getOidcProviderClient() {
        return provider != null ? provider.client : null;
    }

    public SecretKey getStateEncryptionKey() {
        return stateSecretKey;
    }

    public SecretKey getTokenEncSecretKey() {
        return tokenEncSecretKey;
    }
}
