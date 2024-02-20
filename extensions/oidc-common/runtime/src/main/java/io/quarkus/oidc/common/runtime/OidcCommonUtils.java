package io.quarkus.oidc.common.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Provider;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Secret;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Tls.Verification;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ClassPathUtils;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtSignatureBuilder;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.jwt.util.ResourceUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcCommonUtils {
    public static final Duration CONNECTION_BACKOFF_DURATION = Duration.ofSeconds(2);

    static final byte AMP = '&';
    static final byte EQ = '=';
    static final String HTTP_SCHEME = "http";

    private static final Logger LOG = Logger.getLogger(OidcCommonUtils.class);

    private OidcCommonUtils() {

    }

    public static void verifyEndpointUrl(String endpointUrl) {
        try {
            // Verify that endpoint url is a valid URL
            URI.create(endpointUrl).toURL();
        } catch (Throwable ex) {
            throw new ConfigurationException(
                    String.format("'%s' is invalid", endpointUrl), ex);
        }
    }

    public static void verifyCommonConfiguration(OidcCommonConfig oidcConfig, boolean clientIdOptional,
            boolean isServerConfig) {
        final String configPrefix = isServerConfig ? "quarkus.oidc." : "quarkus.oidc-client.";
        if (!clientIdOptional && !oidcConfig.getClientId().isPresent()) {
            throw new ConfigurationException(
                    String.format("'%sclient-id' property must be configured", configPrefix));
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
        } else if (oidcConfig.tls.trustStoreFile.isPresent()) {
            try {
                byte[] trustStoreData = getFileContent(oidcConfig.tls.trustStoreFile.get());
                io.vertx.core.net.KeyStoreOptions trustStoreOptions = new KeyStoreOptions()
                        .setPassword(oidcConfig.tls.getTrustStorePassword().orElse("password"))
                        .setAlias(oidcConfig.tls.getTrustStoreCertAlias().orElse(null))
                        .setValue(io.vertx.core.buffer.Buffer.buffer(trustStoreData))
                        .setType(getKeyStoreType(oidcConfig.tls.trustStoreFileType, oidcConfig.tls.trustStoreFile.get()))
                        .setProvider(oidcConfig.tls.trustStoreProvider.orElse(null));
                options.setTrustOptions(trustStoreOptions);
                if (Verification.CERTIFICATE_VALIDATION == oidcConfig.tls.verification.orElse(Verification.REQUIRED)) {
                    options.setVerifyHost(false);
                }
            } catch (IOException ex) {
                throw new ConfigurationException(String.format(
                        "OIDC truststore file does not exist or can not be read",
                        oidcConfig.tls.trustStoreFile.get().toString()), ex);
            }
        }
        if (oidcConfig.tls.keyStoreFile.isPresent()) {
            try {
                byte[] keyStoreData = getFileContent(oidcConfig.tls.keyStoreFile.get());
                io.vertx.core.net.KeyStoreOptions keyStoreOptions = new KeyStoreOptions()
                        .setAlias(oidcConfig.tls.keyStoreKeyAlias.orElse(null))
                        .setAliasPassword(oidcConfig.tls.keyStoreKeyPassword.orElse(null))
                        .setValue(io.vertx.core.buffer.Buffer.buffer(keyStoreData))
                        .setType(getKeyStoreType(oidcConfig.tls.keyStoreFileType, oidcConfig.tls.keyStoreFile.get()))
                        .setProvider(oidcConfig.tls.keyStoreProvider.orElse(null));

                if (oidcConfig.tls.keyStorePassword.isPresent()) {
                    keyStoreOptions.setPassword(oidcConfig.tls.keyStorePassword.get());
                }

                options.setKeyCertOptions(keyStoreOptions);

            } catch (IOException ex) {
                throw new ConfigurationException(String.format(
                        "OIDC keystore file does not exist or can not be read",
                        oidcConfig.tls.keyStoreFile.get().toString()), ex);
            }
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

    public static String getKeyStoreType(Optional<String> fileType, Path storePath) {
        if (fileType.isPresent()) {
            return fileType.get().toUpperCase();
        }
        final String pathName = storePath.toString();
        if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
            return "PKCS12";
        } else {
            // assume jks
            return "JKS";
        }
    }

    public static String getAuthServerUrl(OidcCommonConfig oidcConfig) {
        return removeLastPathSeparator(oidcConfig.getAuthServerUrl().get());
    }

    private static String removeLastPathSeparator(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public static String getOidcEndpointUrl(String authServerUrl, Optional<String> endpointPath) {
        if (endpointPath != null && endpointPath.isPresent()) {
            return isAbsoluteUrl(endpointPath) ? endpointPath.get() : authServerUrl + prependSlash(endpointPath.get());
        } else {
            return null;
        }
    }

    public static boolean isAbsoluteUrl(Optional<String> endpointUrl) {
        return endpointUrl.isPresent() && endpointUrl.get().startsWith(HTTP_SCHEME);
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
        // Vert.x Client currently does not expect a host having a scheme but keycloak-authorization expects scheme and host.
        // Having a dedicated scheme property is probably better, but since it is property is not taken into account in Vertx Client
        // it does not really make sense as it can send a misleading message that users can choose between `http` and `https`.
        String host = URI.create(proxyConfig.host.get()).getHost();
        if (host == null) {
            host = proxyConfig.host.get();
        }
        jsonOptions.put("host", host);
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
                        && clientSecretMethod(creds) == Secret.Method.BASIC);
    }

    public static boolean isClientJwtAuthRequired(Credentials creds) {
        return creds.jwt.secret.isPresent() || creds.jwt.secretProvider.key.isPresent() || creds.jwt.keyFile.isPresent()
                || creds.jwt.keyStoreFile.isPresent();
    }

    public static boolean isClientSecretPostAuthRequired(Credentials creds) {
        return (creds.clientSecret.value.isPresent() || creds.clientSecret.provider.key.isPresent())
                && clientSecretMethod(creds) == Secret.Method.POST;
    }

    public static boolean isClientSecretPostJwtAuthRequired(Credentials creds) {
        return clientSecretMethod(creds) == Secret.Method.POST_JWT && isClientJwtAuthRequired(creds);
    }

    public static String clientSecret(Credentials creds) {
        return creds.secret.orElse(creds.clientSecret.value.orElseGet(fromCredentialsProvider(creds.clientSecret.provider)));
    }

    public static String jwtSecret(Credentials creds) {
        return creds.jwt.secret.orElseGet(fromCredentialsProvider(creds.jwt.secretProvider));
    }

    public static Secret.Method clientSecretMethod(Credentials creds) {
        return creds.clientSecret.method.orElseGet(() -> Secret.Method.BASIC);
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
                    .createSecretKeyFromSecret(jwtSecret(creds));
        } else {
            Key key = null;
            try {
                if (creds.jwt.getKeyFile().isPresent()) {
                    key = KeyUtils.readSigningKey(creds.jwt.getKeyFile().get(), creds.jwt.keyId.orElse(null),
                            getSignatureAlgorithm(creds, SignatureAlgorithm.RS256));
                } else if (creds.jwt.keyStoreFile.isPresent()) {
                    KeyStore ks = KeyStore.getInstance("JKS");
                    InputStream is = ResourceUtils.getResourceStream(creds.jwt.keyStoreFile.get());

                    if (creds.jwt.keyStorePassword.isPresent()) {
                        ks.load(is, creds.jwt.keyStorePassword.get().toCharArray());
                    } else {
                        ks.load(is, null);
                    }

                    if (creds.jwt.keyPassword.isPresent()) {
                        key = ks.getKey(creds.jwt.keyId.get(), creds.jwt.keyPassword.get().toCharArray());
                    } else {
                        throw new ConfigurationException(
                                "When using a key store, the `quarkus.oidc-client.credentials.jwt.key-password` property must be set");
                    }
                }
            } catch (Exception ex) {
                throw new ConfigurationException("Key can not be loaded", ex);
            }
            if (key == null) {
                throw new ConfigurationException("Key is null");
            }
            return key;
        }
    }

    public static String signJwtWithKey(OidcCommonConfig oidcConfig, String tokenRequestUri, Key key) {
        // 'jti' and 'iat' claims are created by default, 'iat' - is set to the current time
        JwtSignatureBuilder builder = Jwt
                .claims(additionalClaims(oidcConfig.credentials.jwt.getClaims()))
                .issuer(oidcConfig.credentials.jwt.issuer.orElse(oidcConfig.clientId.get()))
                .subject(oidcConfig.credentials.jwt.subject.orElse(oidcConfig.clientId.get()))
                .audience(oidcConfig.credentials.jwt.getAudience().isPresent()
                        ? removeLastPathSeparator(oidcConfig.credentials.jwt.getAudience().get())
                        : tokenRequestUri)
                .expiresIn(oidcConfig.credentials.jwt.lifespan)
                .jws();
        if (oidcConfig.credentials.jwt.getTokenKeyId().isPresent()) {
            builder.keyId(oidcConfig.credentials.jwt.getTokenKeyId().get());
        }
        SignatureAlgorithm signatureAlgorithm = getSignatureAlgorithm(oidcConfig.credentials, null);
        if (signatureAlgorithm != null) {
            builder.algorithm(signatureAlgorithm);
        }

        if (key instanceof SecretKey) {
            return builder.sign((SecretKey) key);
        } else {
            return builder.sign((PrivateKey) key);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String, Object> additionalClaims(Map<String, String> claims) {
        return (Map) claims;
    }

    private static SignatureAlgorithm getSignatureAlgorithm(Credentials credentials, SignatureAlgorithm defaultAlgorithm) {
        if (credentials.jwt.getSignatureAlgorithm().isPresent()) {
            try {
                return SignatureAlgorithm.fromAlgorithm(credentials.jwt.getSignatureAlgorithm().get());
            } catch (Exception ex) {
                throw new ConfigurationException("Unsupported signature algorithm");
            }
        } else {
            return defaultAlgorithm;
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
            return basicSchemeValue(oidcConfig.getClientId().get(), clientSecret(oidcConfig.credentials));
        }
        return null;
    }

    public static String basicSchemeValue(String name, String secret) {
        return OidcConstants.BASIC_SCHEME + " "
                + Base64.getEncoder().encodeToString((name + ":" + secret).getBytes(StandardCharsets.UTF_8));

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

    public static Uni<JsonObject> discoverMetadata(WebClient client, Map<OidcEndpoint.Type, List<OidcRequestFilter>> filters,
            String authServerUrl, long connectionDelayInMillisecs) {
        final String discoveryUrl = getDiscoveryUri(authServerUrl);
        HttpRequest<Buffer> request = client.getAbs(discoveryUrl);
        if (!filters.isEmpty()) {
            OidcRequestContextProperties requestProps = new OidcRequestContextProperties(
                    Map.of(OidcRequestContextProperties.DISCOVERY_ENDPOINT, discoveryUrl));
            for (OidcRequestFilter filter : getMatchingOidcRequestFilters(filters, OidcEndpoint.Type.DISCOVERY)) {
                filter.filter(request, null, requestProps);
            }
        }
        return request.send().onItem().transform(resp -> {
            if (resp.statusCode() == 200) {
                return resp.bodyAsJsonObject();
            } else {
                String errorMessage = resp.bodyAsString();
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    LOG.warnf("Discovery request %s has failed, status code: %d, error message: %s", discoveryUrl,
                            resp.statusCode(), errorMessage);
                } else {
                    LOG.warnf("Discovery request %s has failed, status code: %d", discoveryUrl, resp.statusCode());
                }
                throw new OidcEndpointAccessException(resp.statusCode());
            }
        }).onFailure(oidcEndpointNotAvailable())
                .retry()
                .withBackOff(CONNECTION_BACKOFF_DURATION, CONNECTION_BACKOFF_DURATION)
                .expireIn(connectionDelayInMillisecs)
                .onFailure().transform(t -> {
                    LOG.warn("OIDC Server is not available:", t.getCause() != null ? t.getCause() : t);
                    // don't wrap it to avoid information leak
                    return new RuntimeException("OIDC Server is not available");
                });
    }

    public static String getDiscoveryUri(String authServerUrl) {
        return authServerUrl + OidcConstants.WELL_KNOWN_CONFIGURATION;
    }

    private static byte[] getFileContent(Path path) throws IOException {
        byte[] data;
        final InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(ClassPathUtils.toResourceName(path));
        if (resource != null) {
            try (InputStream is = resource) {
                data = doRead(is);
            }
        } else {
            try (InputStream is = Files.newInputStream(path)) {
                data = doRead(is);
            }
        }
        return data;
    }

    private static byte[] doRead(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    public static Map<OidcEndpoint.Type, List<OidcRequestFilter>> getOidcRequestFilters() {
        ArcContainer container = Arc.container();
        if (container != null) {
            Map<OidcEndpoint.Type, List<OidcRequestFilter>> map = new HashMap<>();
            for (OidcRequestFilter filter : container.listAll(OidcRequestFilter.class).stream().map(handle -> handle.get())
                    .collect(Collectors.toList())) {
                OidcEndpoint endpoint = ClientProxy.unwrap(filter).getClass().getAnnotation(OidcEndpoint.class);
                if (endpoint != null) {
                    for (OidcEndpoint.Type type : endpoint.value()) {
                        map.computeIfAbsent(type, k -> new ArrayList<OidcRequestFilter>()).add(filter);
                    }
                } else {
                    map.computeIfAbsent(OidcEndpoint.Type.ALL, k -> new ArrayList<OidcRequestFilter>()).add(filter);
                }
            }
            return map;
        }
        return Map.of();
    }

    public static List<OidcRequestFilter> getMatchingOidcRequestFilters(Map<OidcEndpoint.Type, List<OidcRequestFilter>> filters,
            OidcEndpoint.Type type) {
        List<OidcRequestFilter> typeSpecific = filters.get(type);
        List<OidcRequestFilter> all = filters.get(OidcEndpoint.Type.ALL);
        if (typeSpecific == null && all == null) {
            return List.of();
        }
        if (typeSpecific != null && all == null) {
            return typeSpecific;
        } else if (typeSpecific == null && all != null) {
            return all;
        } else {
            List<OidcRequestFilter> combined = new ArrayList<>(typeSpecific.size() + all.size());
            combined.addAll(typeSpecific);
            combined.addAll(all);
            return combined;
        }

    }
}
