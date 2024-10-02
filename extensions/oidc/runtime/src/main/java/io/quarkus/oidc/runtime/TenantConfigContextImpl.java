package io.quarkus.oidc.runtime;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

import io.quarkus.arc.ClientProxy;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcRedirectFilter;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.Redirect;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.runtime.configuration.ConfigurationException;

final class TenantConfigContextImpl implements TenantConfigContext {
    private static final Logger LOG = Logger.getLogger(TenantConfigContextImpl.class);

    /**
     * OIDC Provider
     */
    private final OidcProvider provider;

    /**
     * Tenant configuration
     */
    private final OidcTenantConfig oidcConfig;

    private final Map<Redirect.Location, List<OidcRedirectFilter>> redirectFilters;

    /**
     * PKCE Secret Key
     */
    private final SecretKey stateSecretKey;

    /**
     * Token Encryption Secret Key
     */
    private final SecretKey tokenEncSecretKey;

    /**
     * Internal ID token generated key
     */
    private final SecretKey internalIdTokenGeneratedKey;

    private final boolean ready;

    TenantConfigContextImpl(OidcProvider client, OidcTenantConfig config) {
        this(client, config, true);
    }

    TenantConfigContextImpl(OidcProvider provider, OidcTenantConfig config, boolean ready) {
        this.provider = provider;
        this.oidcConfig = config;
        this.redirectFilters = getRedirectFiltersMap(TenantFeatureFinder.find(config, OidcRedirectFilter.class));
        this.ready = ready;

        boolean isService = OidcUtils.isServiceApp(config);
        stateSecretKey = !isService && provider != null && provider.client != null ? createStateSecretKey(config) : null;
        tokenEncSecretKey = !isService && provider != null && provider.client != null
                ? createTokenEncSecretKey(config, provider)
                : null;
        internalIdTokenGeneratedKey = !isService && provider != null && provider.client != null
                ? generateIdTokenSecretKey(config, provider)
                : null;
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
                LOG.debug("'quarkus.oidc.authentication.state-secret' is not configured");
                String possiblePkceSecret = OidcCommonUtils.getClientOrJwtSecret(config.credentials);
                if (possiblePkceSecret != null && possiblePkceSecret.length() < 32) {
                    LOG.debug("Client secret is less than 32 characters long, the state secret will be generated");
                } else {
                    stateSecret = possiblePkceSecret;
                }
            }
            try {
                if (stateSecret == null) {
                    LOG.debug("Secret key for encrypting state cookie is missing, auto-generating it");
                    SecretKey key = OidcCommonUtils.generateSecretKey();
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

    private static SecretKey createTokenEncSecretKey(OidcTenantConfig config, OidcProvider provider) {
        if (config.tokenStateManager.encryptionRequired) {
            String encSecret = null;
            if (config.tokenStateManager.encryptionSecret.isPresent()) {
                encSecret = config.tokenStateManager.encryptionSecret.get();
            } else {
                LOG.debug("'quarkus.oidc.token-state-manager.encryption-secret' is not configured");
                encSecret = OidcCommonUtils.getClientOrJwtSecret(config.credentials);
            }
            try {
                if (encSecret != null) {
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
                    return OidcUtils.createSecretKeyFromDigest(secretBytes);
                } else if (provider.client.getClientJwtKey() instanceof PrivateKey) {
                    return OidcUtils.createSecretKeyFromDigest(((PrivateKey) provider.client.getClientJwtKey()).getEncoded());
                }

                LOG.warn(
                        "Secret key for encrypting OIDC authorization code flow tokens in a session cookie is not configured, auto-generating it."
                                + " Note that a new secret will be generated after a restart, thus making it impossible to decrypt the session cookie and requiring a user re-authentication."
                                + " Use 'quarkus.oidc.token-state-manager.encryption-secret' to configure an encryption secret."
                                + " Alternatively, disable session cookie encryption with 'quarkus.oidc.token-state-manager.encryption-required=false'"
                                + " but only if it is considered to be safe in your application's network.");
                return OidcCommonUtils.generateSecretKey();
            } catch (Exception ex) {
                throw new OIDCException(ex);
            }
        }
        return null;
    }

    private static SecretKey generateIdTokenSecretKey(OidcTenantConfig config, OidcProvider provider) {
        try {
            return (!config.authentication.idTokenRequired.orElse(true)
                    && OidcCommonUtils.getClientOrJwtSecret(config.credentials) == null
                    && provider.client.getClientJwtKey() == null) ? OidcCommonUtils.generateSecretKey() : null;
        } catch (Exception ex) {
            throw new OIDCException(ex);
        }
    }

    @Override
    public OidcTenantConfig oidcConfig() {
        return oidcConfig;
    }

    @Override
    public OidcProvider provider() {
        return provider;
    }

    @Override
    public boolean ready() {
        return ready;
    }

    @Override
    public OidcTenantConfig getOidcTenantConfig() {
        return oidcConfig;
    }

    @Override
    public OidcConfigurationMetadata getOidcMetadata() {
        return provider != null ? provider.getMetadata() : null;
    }

    @Override
    public OidcProviderClient getOidcProviderClient() {
        return provider != null ? provider.client : null;
    }

    @Override
    public SecretKey getStateEncryptionKey() {
        return stateSecretKey;
    }

    @Override
    public SecretKey getTokenEncSecretKey() {
        return tokenEncSecretKey;
    }

    @Override
    public SecretKey getInternalIdTokenSecretKey() {
        return this.internalIdTokenGeneratedKey;
    }

    private static Map<Redirect.Location, List<OidcRedirectFilter>> getRedirectFiltersMap(List<OidcRedirectFilter> filters) {
        Map<Redirect.Location, List<OidcRedirectFilter>> map = new HashMap<>();
        for (OidcRedirectFilter filter : filters) {
            Redirect redirect = ClientProxy.unwrap(filter).getClass().getAnnotation(Redirect.class);
            if (redirect != null) {
                for (Redirect.Location loc : redirect.value()) {
                    map.computeIfAbsent(loc, k -> new ArrayList<OidcRedirectFilter>()).add(filter);
                }
            } else {
                map.computeIfAbsent(Redirect.Location.ALL, k -> new ArrayList<OidcRedirectFilter>()).add(filter);
            }
        }
        return map;
    }

    @Override
    public List<OidcRedirectFilter> getOidcRedirectFilters(Redirect.Location loc) {
        List<OidcRedirectFilter> typeSpecific = redirectFilters.get(loc);
        List<OidcRedirectFilter> all = redirectFilters.get(Redirect.Location.ALL);
        if (typeSpecific == null && all == null) {
            return List.of();
        }
        if (typeSpecific != null && all == null) {
            return typeSpecific;
        } else if (typeSpecific == null && all != null) {
            return all;
        } else {
            List<OidcRedirectFilter> combined = new ArrayList<>(typeSpecific.size() + all.size());
            combined.addAll(typeSpecific);
            combined.addAll(all);
            return combined;
        }
    }
}
