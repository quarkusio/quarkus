package io.quarkus.oidc.common.runtime;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.crypto.SecretKey;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Provider;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Secret;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Tls.Verification;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtSignatureBuilder;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.jwt.util.ResourceUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcCommonUtils {
    public static final Duration CONNECTION_BACKOFF_DURATION = Duration.ofSeconds(2);

    static final byte AMP = '&';
    static final byte EQ = '=';

    private static final Logger LOG = Logger.getLogger(OidcCommonUtils.class);

    private OidcCommonUtils() {

    }

    public static void verifyCommonConfiguration(OidcCommonConfig oidcConfig, boolean clientIdOptional,
            boolean isServerConfig) {
        final String configPrefix = isServerConfig ? "quarkus.oidc." : "quarkus.oidc-client.";
        if (!oidcConfig.getAuthServerUrl().isPresent()) {
            throw new ConfigurationException(
                    String.format("'%sauth-server-url' property must be configured",
                            configPrefix));
        }

        if (!clientIdOptional && !oidcConfig.getClientId().isPresent()) {
            throw new ConfigurationException(
                    String.format("'%sclient-id' property must be configured", configPrefix));
        }

        try {
            // Verify that auth-server-url is a valid URL
            URI.create(oidcConfig.getAuthServerUrl().get()).toURL();
        } catch (Throwable ex) {
            throw new ConfigurationException(
                    String.format("'%sauth-server-url' is invalid", configPrefix), ex);
        }
        Credentials creds = oidcConfig.getCredentials();
        if (creds.secret.isPresent() && creds.clientSecret.value.isPresent()) {
            throw new ConfigurationException(
                    String.format(
                            "'%1$scredentials.secret' and '%1$scredentials.client-secret' properties are mutually exclusive",
                            configPrefix));
        }
        if ((creds.secret.isPresent() || creds.clientSecret.value.isPresent()) && creds.jwt.secret.isPresent()) {
            throw new ConfigurationException(
                    String.format(
                            "Use only '%1$scredentials.secret' or '%1$scredentials.client-secret' or '%1$scredentials.jwt.secret' property",
                            configPrefix));
        }
    }

    public static String prependSlash(String path) {
        return !path.startsWith("/") ? "/" + path : path;
    }

    public static Buffer encodeForm(MultiMap form) {
        Buffer buffer = Buffer.buffer();
        for (Map.Entry<String, String> entry : form) {
            if (buffer.length() != 0) {
                buffer.appendByte(AMP);
            }
            buffer.appendString(entry.getKey());
            buffer.appendByte(EQ);
            buffer.appendString(urlEncode(entry.getValue()));
        }
        return buffer;
    }

    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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

        OptionalInt maxPoolSize = oidcConfig.maxPoolSize;
        if (maxPoolSize.isPresent()) {
            options.setMaxPoolSize(maxPoolSize.getAsInt());
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

    private static long getConnectionDelay(OidcCommonConfig oidcConfig) {
        return oidcConfig.getConnectionDelay().isPresent()
                ? oidcConfig.getConnectionDelay().get().getSeconds()
                : 0;
    }

    public static long getConnectionDelayInMillis(OidcCommonConfig oidcConfig) {
        final long connectionDelayInSecs = getConnectionDelay(oidcConfig);
        final long connectionRetryCount = connectionDelayInSecs > 1 ? connectionDelayInSecs / 2 : 1;
        if (connectionRetryCount > 1) {
            LOG.infof("Connecting to OpenId Connect Provider for up to %d times every 2 seconds", connectionRetryCount);
        }
        return connectionDelayInSecs * 1000;
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
        return creds.secret.isPresent() ||
                ((creds.clientSecret.value.isPresent() || creds.clientSecret.provider.key.isPresent())
                        && creds.clientSecret.method.orElseGet(() -> Secret.Method.BASIC) == Secret.Method.BASIC);
    }

    public static boolean isClientJwtAuthRequired(Credentials creds) {
        return creds.jwt.secret.isPresent() || creds.jwt.secretProvider.key.isPresent() || creds.jwt.keyFile.isPresent()
                || creds.jwt.keyStoreFile.isPresent();
    }

    public static boolean isClientSecretPostAuthRequired(Credentials creds) {
        return (creds.clientSecret.value.isPresent() || creds.clientSecret.provider.key.isPresent())
                && creds.clientSecret.method.orElseGet(() -> Secret.Method.BASIC) == Secret.Method.POST;
    }

    public static String clientSecret(Credentials creds) {
        return creds.secret.orElse(creds.clientSecret.value.orElseGet(fromCredentialsProvider(creds.clientSecret.provider)));
    }

    private static Supplier<? extends String> fromCredentialsProvider(Provider provider) {
        return new Supplier<String>() {

            @Override
            public String get() {
                if (provider.key.isPresent()) {
                    String providerName = provider.name.orElse(null);
                    CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(providerName);
                    if (credentialsProvider != null) {
                        return credentialsProvider.getCredentials(providerName).get(provider.key.get());
                    }
                }
                return null;
            }
        };
    }

    public static Key clientJwtKey(Credentials creds) {
        if (creds.jwt.secret.isPresent() || creds.jwt.secretProvider.key.isPresent()) {
            return KeyUtils
                    .createSecretKeyFromSecret(creds.jwt.secret.orElseGet(fromCredentialsProvider(creds.jwt.secretProvider)));
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
        JwtSignatureBuilder builder = Jwt
                .issuer(oidcConfig.clientId.get())
                .subject(oidcConfig.clientId.get())
                .audience(getAuthServerUrl(oidcConfig))
                .expiresIn(oidcConfig.credentials.jwt.lifespan)
                .jws();
        if (oidcConfig.credentials.jwt.getTokenKeyId().isPresent()) {
            builder.keyId(oidcConfig.credentials.jwt.getTokenKeyId().get());
        }
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

    public static String initClientSecretBasicAuth(OidcCommonConfig oidcConfig) {
        if (isClientSecretBasicAuthRequired(oidcConfig.credentials)) {
            return OidcConstants.BASIC_SCHEME + " "
                    + Base64.getEncoder().encodeToString(
                            (oidcConfig.getClientId().get() + ":"
                                    + clientSecret(oidcConfig.credentials)).getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    public static Key initClientJwtKey(OidcCommonConfig oidcConfig) {
        if (isClientJwtAuthRequired(oidcConfig.credentials)) {
            return clientJwtKey(oidcConfig.credentials);
        }
        return null;
    }

    public static Predicate<? super Throwable> oidcEndpointNotAvailable() {
        return t -> (t instanceof ConnectException
                || (t instanceof OidcEndpointAccessException && ((OidcEndpointAccessException) t).getErrorStatus() == 404));
    }

    public static Uni<JsonObject> discoverMetadata(WebClient client, String authServerUrl, long connectionDelayInMillisecs) {
        final String discoveryUrl = authServerUrl + OidcConstants.WELL_KNOWN_CONFIGURATION;
        return client.getAbs(discoveryUrl).send().onItem().transform(resp -> {
            if (resp.statusCode() == 200) {
                return resp.bodyAsJsonObject();
            } else {
                LOG.tracef("Discovery has failed, status code: %d", resp.statusCode());
                throw new OidcEndpointAccessException(resp.statusCode());
            }
        }).onFailure(oidcEndpointNotAvailable())
                .retry()
                .withBackOff(CONNECTION_BACKOFF_DURATION, CONNECTION_BACKOFF_DURATION)
                .expireIn(connectionDelayInMillisecs)
                .onFailure().transform(t -> t.getCause());
    }
}
