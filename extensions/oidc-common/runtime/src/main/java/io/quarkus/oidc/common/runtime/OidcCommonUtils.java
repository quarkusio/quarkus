package io.quarkus.oidc.common.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcRequestFilter.OidcRequestContext;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.OidcResponseFilter.OidcResponseContext;
import io.quarkus.oidc.common.runtime.OidcTlsSupport.TlsConfigSupport;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Provider;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret;
import io.quarkus.oidc.common.runtime.config.OidcCommonConfig;
import io.quarkus.oidc.common.runtime.config.OidcCommonConfig.Tls.Verification;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtSignatureBuilder;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.jwt.util.ResourceUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcCommonUtils {
    public static final Duration CONNECTION_BACKOFF_DURATION = Duration.ofSeconds(2);
    public static final String LOCATION_RESPONSE_HEADER = String.valueOf(HttpHeaders.LOCATION);
    public static final String COOKIE_REQUEST_HEADER = String.valueOf(HttpHeaders.COOKIE);

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

    public static void verifyCommonConfiguration(OidcClientCommonConfig oidcConfig, boolean clientIdOptional,
            boolean isServerConfig) {
        final String configPrefix = isServerConfig ? "quarkus.oidc." : "quarkus.oidc-client.";
        if (!clientIdOptional && !oidcConfig.clientId().isPresent()) {
            throw new ConfigurationException(
                    String.format("'%sclient-id' property must be configured", configPrefix));
        }

        Credentials creds = oidcConfig.credentials();
        if (creds.secret().isPresent() && creds.clientSecret().value().isPresent()) {
            throw new ConfigurationException(
                    String.format(
                            "'%1$scredentials.secret' and '%1$scredentials.client-secret' properties are mutually exclusive",
                            configPrefix));
        }
        if ((creds.secret().isPresent() || creds.clientSecret().value().isPresent()) && creds.jwt().secret().isPresent()) {
            throw new ConfigurationException(
                    String.format(
                            "Use only '%1$scredentials.secret' or '%1$scredentials.client-secret' or '%1$scredentials.jwt.secret' property",
                            configPrefix));
        }
        Credentials.Jwt jwt = creds.jwt();
        if (jwt.source() == Credentials.Jwt.Source.BEARER) {
            if (isServerConfig && jwt.tokenPath().isEmpty()) {
                throw new ConfigurationException("Bearer token path must be set when the JWT source is a bearer token");
            }
        } else if (jwt.tokenPath().isPresent()) {
            throw new ConfigurationException("Bearer token path can only be set when the JWT source is a bearer token");
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
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void setHttpClientOptions(OidcCommonConfig oidcConfig, HttpClientOptions options,
            TlsConfigSupport tlsSupport) {

        Optional<ProxyOptions> proxyOpt = toProxyOptions(oidcConfig.proxy());
        if (proxyOpt.isPresent()) {
            options.setProxyOptions(proxyOpt.get());
        }

        OptionalInt maxPoolSize = oidcConfig.maxPoolSize();
        if (maxPoolSize.isPresent()) {
            options.setMaxPoolSize(maxPoolSize.getAsInt());
        }

        options.setConnectTimeout((int) oidcConfig.connectionTimeout().toMillis());

        if (tlsSupport.useTlsRegistry()) {
            TlsConfigUtils.configure(options, tlsSupport.getTlsConfig());
            return;
        }

        boolean trustAll = oidcConfig.tls().verification().isPresent()
                ? oidcConfig.tls().verification().get() == Verification.NONE
                : tlsSupport.isGlobalTrustAll();
        if (trustAll) {
            options.setTrustAll(true);
            options.setVerifyHost(false);
        } else if (oidcConfig.tls().trustStoreFile().isPresent()) {
            try {
                byte[] trustStoreData = getFileContent(oidcConfig.tls().trustStoreFile().get());
                io.vertx.core.net.KeyStoreOptions trustStoreOptions = new KeyStoreOptions()
                        .setPassword(oidcConfig.tls().trustStorePassword().orElse("password"))
                        .setAlias(oidcConfig.tls().trustStoreCertAlias().orElse(null))
                        .setValue(io.vertx.core.buffer.Buffer.buffer(trustStoreData))
                        .setType(
                                getKeyStoreType(oidcConfig.tls().trustStoreFileType(), oidcConfig.tls().trustStoreFile().get()))
                        .setProvider(oidcConfig.tls().trustStoreProvider().orElse(null));
                options.setTrustOptions(trustStoreOptions);
                if (Verification.CERTIFICATE_VALIDATION == oidcConfig.tls().verification().orElse(Verification.REQUIRED)) {
                    options.setVerifyHost(false);
                }
            } catch (IOException ex) {
                throw new ConfigurationException(String.format(
                        "OIDC truststore file %s does not exist or can not be read",
                        oidcConfig.tls().trustStoreFile().get()), ex);
            }
        }
        if (oidcConfig.tls().keyStoreFile().isPresent()) {
            try {
                byte[] keyStoreData = getFileContent(oidcConfig.tls().keyStoreFile().get());
                io.vertx.core.net.KeyStoreOptions keyStoreOptions = new KeyStoreOptions()
                        .setAlias(oidcConfig.tls().keyStoreKeyAlias().orElse(null))
                        .setAliasPassword(oidcConfig.tls().keyStoreKeyPassword().orElse(null))
                        .setValue(io.vertx.core.buffer.Buffer.buffer(keyStoreData))
                        .setType(getKeyStoreType(oidcConfig.tls().keyStoreFileType(), oidcConfig.tls().keyStoreFile().get()))
                        .setProvider(oidcConfig.tls().keyStoreProvider().orElse(null));

                if (oidcConfig.tls().keyStorePassword().isPresent()) {
                    keyStoreOptions.setPassword(oidcConfig.tls().keyStorePassword().get());
                }

                options.setKeyCertOptions(keyStoreOptions);

            } catch (IOException ex) {
                throw new ConfigurationException(String.format(
                        "OIDC keystore file %s does not exist or can not be read",
                        oidcConfig.tls().keyStoreFile().get()), ex);
            }
        }
    }

    public static String getKeyStoreType(Optional<String> fileType, Path storePath) {
        if (fileType.isPresent()) {
            return fileType.get().toUpperCase();
        }
        return inferKeyStoreTypeFromFileExtension(storePath.toString());
    }

    private static String inferKeyStoreTypeFromFileExtension(String pathName) {
        if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
            return "PKCS12";
        } else {
            // assume jks
            return "JKS";
        }
    }

    public static String getAuthServerUrl(OidcCommonConfig oidcConfig) {
        return removeLastPathSeparator(oidcConfig.authServerUrl().get());
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
        return oidcConfig.connectionDelay().isPresent()
                ? oidcConfig.connectionDelay().get().getSeconds()
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
        if (!proxyConfig.host().isPresent()) {
            return Optional.empty();
        }
        JsonObject jsonOptions = new JsonObject();
        // Vert.x Client currently does not expect a host having a scheme but keycloak-authorization expects scheme and host.
        // Having a dedicated scheme property is probably better, but since it is property is not taken into account in Vertx Client
        // it does not really make sense as it can send a misleading message that users can choose between `http` and `https`.
        String host = URI.create(proxyConfig.host().get()).getHost();
        if (host == null) {
            host = proxyConfig.host().get();
        }
        jsonOptions.put("host", host);
        jsonOptions.put("port", proxyConfig.port());
        if (proxyConfig.username().isPresent()) {
            jsonOptions.put("username", proxyConfig.username().get());
        }
        if (proxyConfig.password().isPresent()) {
            jsonOptions.put("password", proxyConfig.password().get());
        }
        return Optional.of(new ProxyOptions(jsonOptions));
    }

    public static String formatConnectionErrorMessage(String authServerUrlString) {
        return String.format("OIDC server is not available at the '%s' URL. "
                + "Please make sure it is correct. Note it has to end with a realm value if you work with Keycloak, for example:"
                + " 'https://localhost:8180/auth/realms/quarkus'", authServerUrlString);
    }

    public static boolean isClientSecretBasicAuthRequired(Credentials creds) {
        return creds.secret().isPresent() ||
                ((creds.clientSecret().value().isPresent() || creds.clientSecret().provider().key().isPresent())
                        && clientSecretMethod(creds) == Secret.Method.BASIC);
    }

    public static boolean isClientJwtAuthRequired(Credentials creds, boolean server) {
        Set<String> props = new HashSet<>();
        if (creds.jwt().secret().isPresent()) {
            props.add(".credentials.jwt.secret");
        }
        if (creds.jwt().secretProvider().key().isPresent()) {
            props.add(".credentials.jwt.secret-provider.key");
        }
        if (creds.jwt().key().isPresent()) {
            props.add(".credentials.jwt.key");
        }
        if (creds.jwt().keyFile().isPresent()) {
            props.add(".credentials.jwt.key-file");
        }
        if (creds.jwt().keyStoreFile().isPresent()) {
            props.add(".credentials.jwt.key-store-file");
        }
        if (props.size() > 1) {
            final String prefix = server ? "quarkus.oidc" : "quarkus.oidc-client";
            throw new ConfigurationException("""
                    Only a single OIDC JWT credential key property can be configured, but you have configured: %s"""
                    .formatted(props.stream().map(p -> (prefix + p)).collect(Collectors.joining(","))));
        }
        return props.size() == 1;
    }

    public static boolean isClientSecretPostAuthRequired(Credentials creds) {
        return (creds.clientSecret().value().isPresent() || creds.clientSecret().provider().key().isPresent())
                && clientSecretMethod(creds) == Secret.Method.POST;
    }

    public static boolean isClientSecretPostJwtAuthRequired(Credentials creds) {
        return clientSecretMethod(creds) == Secret.Method.POST_JWT;
    }

    public static boolean isJwtAssertion(Credentials creds) {
        return creds.jwt().assertion();
    }

    public static String clientSecret(Credentials creds) {
        return creds.secret()
                .orElse(creds.clientSecret().value().orElseGet(fromCredentialsProvider(creds.clientSecret().provider())));
    }

    public static String jwtSecret(Credentials creds) {
        return creds.jwt().secret().orElseGet(fromCredentialsProvider(creds.jwt().secretProvider()));
    }

    public static String getClientOrJwtSecret(Credentials creds) {
        LOG.debug("Trying to get the configured client secret");
        String encSecret = clientSecret(creds);
        if (encSecret == null) {
            LOG.debug("Client secret is not configured, "
                    + "trying to get the configured 'client_jwt_secret' secret");
            encSecret = jwtSecret(creds);
        }
        return encSecret;
    }

    public static SecretKey generateSecretKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    public static Secret.Method clientSecretMethod(Credentials creds) {
        return creds.clientSecret().method().orElseGet(() -> Secret.Method.BASIC);
    }

    private static Supplier<? extends String> fromCredentialsProvider(Provider provider) {
        return new Supplier<String>() {

            @Override
            public String get() {
                if (provider.key().isPresent()) {
                    String providerName = provider.name().orElse(null);
                    String keyringName = provider.keyringName().orElse(null);
                    CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(providerName);
                    return credentialsProvider.getCredentials(keyringName).get(provider.key().get());
                }
                return null;
            }
        };
    }

    public static Key clientJwtKey(Credentials creds) {
        if (creds.jwt().secret().isPresent() || creds.jwt().secretProvider().key().isPresent()) {
            return KeyUtils
                    .createSecretKeyFromSecret(jwtSecret(creds));
        } else {
            Key key = null;
            try {
                if (creds.jwt().key().isPresent()) {
                    key = KeyUtils.tryAsPemSigningPrivateKey(creds.jwt().key().get(),
                            getSignatureAlgorithm(creds, SignatureAlgorithm.RS256));
                } else if (creds.jwt().keyFile().isPresent()) {
                    key = KeyUtils.readSigningKey(creds.jwt().keyFile().get(), creds.jwt().keyId().orElse(null),
                            getSignatureAlgorithm(creds, SignatureAlgorithm.RS256));
                } else if (creds.jwt().keyStoreFile().isPresent()) {
                    var keyStoreFile = creds.jwt().keyStoreFile().get();
                    KeyStore ks = KeyStore.getInstance(inferKeyStoreTypeFromFileExtension(keyStoreFile));
                    InputStream is = ResourceUtils.getResourceStream(keyStoreFile);

                    if (creds.jwt().keyStorePassword().isPresent()) {
                        ks.load(is, creds.jwt().keyStorePassword().get().toCharArray());
                    } else {
                        ks.load(is, null);
                    }

                    if (creds.jwt().keyPassword().isPresent()) {
                        key = ks.getKey(creds.jwt().keyId().get(), creds.jwt().keyPassword().get().toCharArray());
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

    public static String signJwtWithKey(OidcClientCommonConfig oidcConfig, String tokenRequestUri, Key key) {
        // 'jti' and 'iat' claims are created by default, 'iat' - is set to the current time
        JwtSignatureBuilder jwtSignatureBuilder = Jwt
                .claims(additionalClaims(oidcConfig.credentials().jwt().claims()))
                .issuer(oidcConfig.credentials().jwt().issuer().orElse(oidcConfig.clientId().get()))
                .subject(oidcConfig.credentials().jwt().subject().orElse(oidcConfig.clientId().get()))
                .audience(oidcConfig.credentials().jwt().audience().isPresent()
                        ? removeLastPathSeparator(oidcConfig.credentials().jwt().audience().get())
                        : tokenRequestUri)
                .expiresIn(oidcConfig.credentials().jwt().lifespan()).jws();
        if (oidcConfig.credentials().jwt().tokenKeyId().isPresent()) {
            jwtSignatureBuilder.keyId(oidcConfig.credentials().jwt().tokenKeyId().get());
        }
        SignatureAlgorithm signatureAlgorithm = getSignatureAlgorithm(oidcConfig.credentials(), null);
        if (signatureAlgorithm != null) {
            jwtSignatureBuilder.algorithm(signatureAlgorithm);
        }

        if (key instanceof SecretKey) {
            return jwtSignatureBuilder.sign((SecretKey) key);
        } else {
            return jwtSignatureBuilder.sign((PrivateKey) key);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String, Object> additionalClaims(Map<String, String> claims) {
        return (Map) claims;
    }

    private static SignatureAlgorithm getSignatureAlgorithm(Credentials credentials, SignatureAlgorithm defaultAlgorithm) {
        if (credentials.jwt().signatureAlgorithm().isPresent()) {
            try {
                return SignatureAlgorithm.fromAlgorithm(credentials.jwt().signatureAlgorithm().get());
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

    public static String initClientSecretBasicAuth(OidcClientCommonConfig oidcConfig) {
        if (isClientSecretBasicAuthRequired(oidcConfig.credentials())) {
            return basicSchemeValue(oidcConfig.clientId().get(), clientSecret(oidcConfig.credentials()));
        }
        return null;
    }

    public static String basicSchemeValue(String name, String secret) {
        return OidcConstants.BASIC_SCHEME + " "
                + Base64.getEncoder().encodeToString((name + ":" + secret).getBytes(StandardCharsets.UTF_8));

    }

    public static Key initClientJwtKey(OidcClientCommonConfig oidcConfig, boolean server) {
        if (isClientJwtAuthRequired(oidcConfig.credentials(), server)) {
            return clientJwtKey(oidcConfig.credentials());
        }
        return null;
    }

    public static Predicate<? super Throwable> oidcEndpointNotAvailable() {
        return t -> (t instanceof SocketException
                || (t instanceof OidcEndpointAccessException && ((OidcEndpointAccessException) t).getErrorStatus() == 404));
    }

    public static Predicate<? super Throwable> validOidcClientRedirect(String originalUri) {
        return t -> (t instanceof OidcClientRedirectException
                && isValidOidcClientRedirectRequest((OidcClientRedirectException) t, originalUri));
    }

    private static boolean isValidOidcClientRedirectRequest(OidcClientRedirectException ex,
            String originalUrl) {
        if (!originalUrl.equals(ex.getLocation())) {
            LOG.warnf("Redirect is only allowed to %s but redirect to %s is requested",
                    originalUrl, ex.getLocation());
            return false;
        }
        if (ex.getCookies().isEmpty()) {
            LOG.warnf("Redirect is requested to %s but no cookies are set", originalUrl);
            return false;
        }
        LOG.debugf("Single redirect to %s with cookies is approved", originalUrl);
        return true;
    }

    public static Uni<JsonObject> discoverMetadata(WebClient client, OidcRequestContextProperties contextProperties,
            String authServerUrl, long connectionDelayInMillisecs, Vertx vertx,
            boolean blockingDnsLookup, OidcFilterStorage oidcFilterStorage) {
        final String discoveryUrl = getDiscoveryUri(authServerUrl);
        final OidcRequestContextProperties requestProps = oidcFilterStorage.isEmpty() ? null
                : getDiscoveryRequestProps(contextProperties, discoveryUrl);

        return doDiscoverMetadata(client, contextProperties, discoveryUrl,
                connectionDelayInMillisecs, vertx, blockingDnsLookup, List.of(), oidcFilterStorage)
                .onFailure(validOidcClientRedirect(discoveryUrl))
                .recoverWithUni(
                        new Function<Throwable, Uni<? extends JsonObject>>() {
                            @Override
                            public Uni<JsonObject> apply(Throwable t) {
                                OidcClientRedirectException ex = (OidcClientRedirectException) t;
                                return doDiscoverMetadata(client, requestProps, discoveryUrl, connectionDelayInMillisecs, vertx,
                                        blockingDnsLookup, ex.getCookies(), oidcFilterStorage);
                            }
                        })
                .onFailure().transform(t -> {
                    LOG.warn("OIDC Server is not available:", t.getCause() != null ? t.getCause() : t);
                    // don't wrap it to avoid information leak
                    return new RuntimeException("OIDC Server is not available");
                });

    }

    public static Uni<JsonObject> doDiscoverMetadata(WebClient client, OidcRequestContextProperties requestProps,
            String discoveryUrl, long connectionDelayInMillisecs, Vertx vertx,
            boolean blockingDnsLookup, List<String> cookies, OidcFilterStorage oidcFilterStorage) {
        HttpRequest<Buffer> request = client.getAbs(discoveryUrl);
        if (!cookies.isEmpty()) {
            request.putHeader(COOKIE_REQUEST_HEADER, cookies);
        }
        if (!oidcFilterStorage.isEmpty()) {
            OidcRequestContext context = new OidcRequestContext(request, null, requestProps);
            for (OidcRequestFilter filter : oidcFilterStorage.getOidcRequestFilters(Type.DISCOVERY, context)) {
                filter.filter(context);
            }
        }
        return sendRequest(vertx, request, blockingDnsLookup).onItem().transform(resp -> {

            Buffer buffer = filterHttpResponse(requestProps, resp, OidcEndpoint.Type.DISCOVERY, oidcFilterStorage);

            if (resp.statusCode() == 200) {
                return buffer.toJsonObject();
            } else if (resp.statusCode() == 302) {
                throw createOidcClientRedirectException(resp);
            } else {
                String errorMessage = buffer != null ? buffer.toString() : null;
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
                .expireIn(connectionDelayInMillisecs);
    }

    public static OidcClientRedirectException createOidcClientRedirectException(HttpResponse<Buffer> resp) {
        LOG.debug("OIDC client redirect is requested");
        return new OidcClientRedirectException(resp.getHeader(LOCATION_RESPONSE_HEADER), resp.cookies());
    }

    private static OidcRequestContextProperties getDiscoveryRequestProps(
            OidcRequestContextProperties contextProperties, String discoveryUrl) {
        Map<String, Object> newProperties = contextProperties == null ? new HashMap<>()
                : new HashMap<>(contextProperties.getAll());
        newProperties.put(OidcRequestContextProperties.DISCOVERY_ENDPOINT, discoveryUrl);
        return new OidcRequestContextProperties(newProperties);
    }

    public static Buffer filterHttpResponse(OidcRequestContextProperties requestProps, HttpResponse<Buffer> resp,
            OidcEndpoint.Type type, OidcFilterStorage oidcFilterStorage) {
        Buffer responseBody = resp.body();
        if (!oidcFilterStorage.isEmpty()) {
            OidcResponseContext context = new OidcResponseContext(requestProps, resp.statusCode(), resp.headers(),
                    responseBody);
            for (OidcResponseFilter filter : oidcFilterStorage.getOidcResponseFilters(type, context)) {
                filter.filter(context);
            }
            return getResponseBuffer(requestProps, responseBody);
        }
        return responseBody;
    }

    public static Buffer getRequestBuffer(OidcRequestContextProperties requestProps, Buffer buffer) {
        if (requestProps == null) {
            return buffer;
        }
        Buffer updatedRequestBody = requestProps.get(OidcRequestContextProperties.REQUEST_BODY);
        return updatedRequestBody == null ? buffer : updatedRequestBody;
    }

    public static Buffer getResponseBuffer(OidcRequestContextProperties requestProps, Buffer buffer) {
        if (requestProps == null) {
            return buffer;
        }
        Buffer updatedResponseBody = requestProps.get(OidcRequestContextProperties.RESPONSE_BODY);
        return updatedResponseBody == null ? buffer : updatedResponseBody;
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

    public static Uni<HttpResponse<Buffer>> sendRequest(io.vertx.core.Vertx vertx, HttpRequest<Buffer> request,
            boolean blockingDnsLookup) {
        if (blockingDnsLookup) {
            return sendRequest(new Vertx(vertx), request, true);
        } else {
            return request.send();
        }
    }

    public static Uni<HttpResponse<Buffer>> sendRequest(Vertx vertx, HttpRequest<Buffer> request, boolean blockingDnsLookup) {
        if (blockingDnsLookup) {
            return vertx.executeBlocking(new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        // cache DNS lookup
                        InetAddress.getByName(request.host());
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            }).flatMap(new Function<Void, Uni<? extends HttpResponse<Buffer>>>() {
                @Override
                public Uni<? extends HttpResponse<Buffer>> apply(Void unused) {
                    return request.send();
                }
            });
        } else {
            return request.send();
        }
    }

    public static JsonObject decodeJwtContent(String jwt) {
        String encodedContent = getJwtContentPart(jwt);
        if (encodedContent == null) {
            return null;
        }
        return decodeAsJsonObject(encodedContent);
    }

    public static String getJwtContentPart(String jwt) {
        StringTokenizer tokens = new StringTokenizer(jwt, ".");
        // part 1: skip the token headers
        tokens.nextToken();
        if (!tokens.hasMoreTokens()) {
            return null;
        }
        // part 2: token content
        String encodedContent = tokens.nextToken();

        // let's check only 1 more signature part is available
        if (tokens.countTokens() != 1) {
            return null;
        }
        return encodedContent;
    }

    public static String base64UrlDecode(String encodedContent) {
        return new String(Base64.getUrlDecoder().decode(encodedContent), StandardCharsets.UTF_8);
    }

    public static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static JsonObject decodeAsJsonObject(String encodedContent) {
        String json = null;
        try {
            json = base64UrlDecode(encodedContent);
        } catch (IllegalArgumentException ex) {
            LOG.debugf("Invalid Base64URL content: %s", encodedContent);
            return null;
        }

        try {
            return new JsonObject(json);
        } catch (DecodeException ex) {
            LOG.debugf("Invalid JSON content: %s", json);
            return null;
        }
    }
}
