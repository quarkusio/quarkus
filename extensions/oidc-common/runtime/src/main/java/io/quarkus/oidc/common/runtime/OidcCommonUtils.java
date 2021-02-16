package io.quarkus.oidc.common.runtime;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Optional;

import javax.crypto.SecretKey;

import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Secret;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Tls.Verification;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.jwt.util.ResourceUtils;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;

public class OidcCommonUtils {
    private OidcCommonUtils() {

    }

    public static void verifyCommonConfiguration(OidcCommonConfig oidcConfig) {
        if (!oidcConfig.getAuthServerUrl().isPresent() || !oidcConfig.getClientId().isPresent()) {
            throw new ConfigurationException("Both 'auth-server-url' and 'client-id' properties must be configured");
        }

        Credentials creds = oidcConfig.getCredentials();
        if (creds.secret.isPresent() && creds.clientSecret.value.isPresent()) {
            throw new ConfigurationException(
                    "'credentials.secret' and 'credentials.client-secret' properties are mutually exclusive");
        }
        if ((creds.secret.isPresent() || creds.clientSecret.value.isPresent()) && creds.jwt.secret.isPresent()) {
            throw new ConfigurationException(
                    "Use only 'credentials.secret' or 'credentials.client-secret' or 'credentials.jwt.secret' property");
        }
    }

    public static String prependSlash(String path) {
        return !path.startsWith("/") ? "/" + path : path;
    }

    public static void setHttpClientOptions(OidcCommonConfig oidcConfig, TlsConfig tlsConfig, HttpClientOptions options) {
        boolean trustAll = oidcConfig.tls.verification.isPresent() ? oidcConfig.tls.verification.get() == Verification.NONE
                : tlsConfig.trustAll;
        if (trustAll) {
            options.setTrustAll(true);
            options.setVerifyHost(false);
        }
        Optional<ProxyOptions> proxyOpt = toProxyOptions(oidcConfig.getProxy());
        if (proxyOpt.isPresent()) {
            options.setProxyOptions(proxyOpt.get());
        }
        options.setConnectTimeout((int) oidcConfig.getConnectionTimeout().toMillis());
    }

    public static String getAuthServerUrl(OidcCommonConfig oidcConfig) {
        String authServerUrl = oidcConfig.getAuthServerUrl().get();
        if (authServerUrl.endsWith("/")) {
            authServerUrl = authServerUrl.substring(0, authServerUrl.length() - 1);
        }
        return authServerUrl;
    }

    public static String getOidcEndpointUrl(String authServerUrl, Optional<String> endpointPath) {
        if (endpointPath.isPresent()) {
            return authServerUrl + prependSlash(endpointPath.get());
        } else {
            return null;
        }
    }

    public static long getConnectionRetryCount(OidcCommonConfig oidcConfig) {
        final long connectionDelayInSecs = getConnectionDelay(oidcConfig);
        return connectionDelayInSecs > 1 ? connectionDelayInSecs / 2 : 1;
    }

    public static long getMaximumConnectionDelay(OidcCommonConfig oidcConfig) {
        final long connectionDelayInSecs = getConnectionDelay(oidcConfig);
        final long connectionRetryCountSecs = connectionDelayInSecs > 1 ? connectionDelayInSecs / 2 : 1;
        return connectionDelayInSecs + connectionRetryCountSecs * oidcConfig.getConnectionTimeout().getSeconds();
    }

    private static long getConnectionDelay(OidcCommonConfig oidcConfig) {
        return oidcConfig.getConnectionDelay().isPresent()
                ? oidcConfig.getConnectionDelay().get().getSeconds()
                : 0;
    }

    public static long getConnectionDelayInMillis(OidcCommonConfig oidcConfig) {
        return getConnectionDelay(oidcConfig) * 1000;
    }

    public static Optional<ProxyOptions> toProxyOptions(OidcCommonConfig.Proxy proxyConfig) {
        // Proxy is enabled if (at least) "host" is configured.
        if (!proxyConfig.host.isPresent()) {
            return Optional.empty();
        }
        JsonObject jsonOptions = new JsonObject();
        jsonOptions.put("host", proxyConfig.host.get());
        jsonOptions.put("port", proxyConfig.port);
        if (proxyConfig.username.isPresent()) {
            jsonOptions.put("username", proxyConfig.username.get());
        }
        if (proxyConfig.password.isPresent()) {
            jsonOptions.put("password", proxyConfig.password.get());
        }
        return Optional.of(new ProxyOptions(jsonOptions));
    }

    public static String formatConnectionErrorMessage(String authServerUrlString) {
        return String.format("OIDC server is not available at the '%s' URL. "
                + "Please make sure it is correct. Note it has to end with a realm value if you work with Keycloak, for example:"
                + " 'https://localhost:8180/auth/realms/quarkus'", authServerUrlString);
    }

    public static boolean isClientSecretBasicAuthRequired(Credentials creds) {
        return creds.secret.isPresent() || creds.clientSecret.value.isPresent()
                && creds.clientSecret.method.orElseGet(() -> Secret.Method.BASIC) == Secret.Method.BASIC;
    }

    public static boolean isClientJwtAuthRequired(Credentials creds) {
        return creds.jwt.secret.isPresent() || creds.jwt.keyFile.isPresent() || creds.jwt.keyStoreFile.isPresent();
    }

    public static boolean isClientSecretPostAuthRequired(Credentials creds) {
        return creds.clientSecret.value.isPresent()
                && creds.clientSecret.method.orElseGet(() -> Secret.Method.BASIC) == Secret.Method.POST;
    }

    public static String clientSecret(Credentials creds) {
        return creds.secret.orElseGet(() -> creds.clientSecret.value.get());
    }

    public static Key clientJwtKey(Credentials creds) {
        if (creds.jwt.secret.isPresent()) {
            return KeyUtils.createSecretKeyFromSecret(creds.jwt.secret.get());
        } else {
            Key key = null;
            try {
                if (creds.jwt.keyFile.isPresent()) {
                    key = KeyUtils.readSigningKey(creds.jwt.keyFile.get(), creds.jwt.keyId.orElse(null));
                } else if (creds.jwt.keyStoreFile.isPresent()) {
                    KeyStore ks = KeyStore.getInstance("JKS");
                    InputStream is = ResourceUtils.getResourceStream(creds.jwt.keyStoreFile.get());
                    ks.load(is, creds.jwt.keyStorePassword.toCharArray());
                    key = ks.getKey(creds.jwt.keyId.get(), creds.jwt.keyPassword.toCharArray());
                }
            } catch (Exception ex) {
                throw new ConfigurationException("Key can not be loaded");
            }
            if (key == null) {
                throw new ConfigurationException("Key is null");
            }
            return key;
        }
    }

    public static String signJwt(OidcCommonConfig oidcConfig) {
        return signJwtWithKey(oidcConfig, clientJwtKey(oidcConfig.credentials));
    }

    public static String signJwtWithKey(OidcCommonConfig oidcConfig, Key key) {
        // 'jti' and 'iat' claim is created by default, iat - is set to the current time
        JwtClaimsBuilder builder = Jwt
                .issuer(oidcConfig.clientId.get())
                .subject(oidcConfig.clientId.get())
                .audience(getAuthServerUrl(oidcConfig))
                .expiresIn(oidcConfig.credentials.jwt.lifespan);
        if (key instanceof SecretKey) {
            return builder.sign((SecretKey) key);
        } else {
            return builder.sign((PrivateKey) key);
        }
    }

    public static void verifyConfigurationId(String defaultId, String configKey, Optional<String> configId) {
        if (configKey.equals(defaultId)) {
            throw new ConfigurationException("configuration id '" + configKey + "' duplicates the default configuration id");
        }
        if (configId.isPresent() && !configKey.equals(configId.get())) {
            throw new ConfigurationException("Configuration has 2 different id values: '"
                    + configKey + "' and '" + configId.get() + "'");
        }

    }
}
