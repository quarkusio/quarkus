package io.quarkus.rest.client.reactive.runtime;

import static io.quarkus.rest.client.reactive.runtime.Constants.DEFAULT_MAX_CHUNK_SIZE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.HostnameVerifier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;

import io.quarkus.arc.Arc;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.restclient.config.RestClientsConfig.RestClientConfig;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.config.SmallRyeConfig;

public class RestClientCDIDelegateBuilder<T> {

    private static final String REST_URL_FORMAT = "quarkus.rest-client.%s.url";
    private static final String REST_URI_FORMAT = "quarkus.rest-client.%s.uri";
    private static final String NONE = "none";

    private final Class<T> jaxrsInterface;
    private final String baseUriFromAnnotation;
    private final String configKey;
    private final RestClientsConfig configRoot;
    private final RestClientConfig restClientConfig;

    public static <T> T createDelegate(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey) {
        return new RestClientCDIDelegateBuilder<T>(jaxrsInterface, baseUriFromAnnotation, configKey).build();
    }

    private RestClientCDIDelegateBuilder(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey) {
        this(jaxrsInterface, baseUriFromAnnotation, configKey,
                ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getConfigMapping(RestClientsConfig.class));
    }

    RestClientCDIDelegateBuilder(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey,
            RestClientsConfig configRoot) {
        this.jaxrsInterface = jaxrsInterface;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.configKey = configKey;
        this.configRoot = configRoot;
        this.restClientConfig = configRoot.getClient(jaxrsInterface);
    }

    private T build() {
        QuarkusRestClientBuilder builder = QuarkusRestClientBuilder.newBuilder();
        configureBuilder(builder);
        return builder.build(jaxrsInterface);
    }

    void configureBuilder(QuarkusRestClientBuilder builder) {
        configureBaseUrl(builder);
        configureTimeouts(builder);
        configureProviders(builder);
        configureTLS(builder);
        configureRedirects(builder);
        configureQueryParamStyle(builder);
        configureProxy(builder);
        configureShared(builder);
        configureLogging(builder);
        configureCustomProperties(builder);
    }

    private void configureLogging(QuarkusRestClientBuilder builder) {
        if (restClientConfig.logging().isPresent()) {
            RestClientsConfig.RestClientLoggingConfig loggingConfig = restClientConfig.logging().get();
            builder.property(QuarkusRestClientProperties.LOGGING_SCOPE,
                    loggingConfig.scope().isPresent() ? LoggingScope.forName(loggingConfig.scope().get()) : LoggingScope.NONE);
            builder.property(QuarkusRestClientProperties.LOGGING_BODY_LIMIT, loggingConfig.bodyLimit());
        }
    }

    private void configureCustomProperties(QuarkusRestClientBuilder builder) {
        Optional<String> encoder = oneOf(restClientConfig.multipartPostEncoderMode(), configRoot.multipartPostEncoderMode());
        if (encoder != null && encoder.isPresent()) {
            PausableHttpPostRequestEncoder.EncoderMode mode = PausableHttpPostRequestEncoder.EncoderMode
                    .valueOf(encoder.get().toUpperCase(Locale.ROOT));
            builder.property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE, mode);
        }

        OptionalInt poolSize = oneOf(restClientConfig.connectionPoolSize(), configRoot.connectionPoolSize());
        if (poolSize.isPresent()) {
            builder.property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, poolSize.getAsInt());
        }

        OptionalInt connectionTTL = oneOf(restClientConfig.connectionTTL(), configRoot.connectionTTL());
        if (connectionTTL.isPresent()) {
            // configuration bean contains value in milliseconds
            int connectionTTLSeconds = connectionTTL.getAsInt() / 1000;
            builder.property(QuarkusRestClientProperties.CONNECTION_TTL, connectionTTLSeconds);
        }

        Optional<Boolean> keepAliveEnabled = oneOf(restClientConfig.keepAliveEnabled(), configRoot.keepAliveEnabled());
        if (keepAliveEnabled.isPresent()) {
            builder.property(QuarkusRestClientProperties.KEEP_ALIVE_ENABLED, keepAliveEnabled.get());
        }

        Map<String, String> headers = restClientConfig.headers();
        if (headers == null || headers.isEmpty()) {
            headers = configRoot.headers();
        }
        if (headers != null && !headers.isEmpty()) {
            builder.property(QuarkusRestClientProperties.STATIC_HEADERS, headers);
        }

        builder.property(QuarkusRestClientProperties.DISABLE_CONTEXTUAL_ERROR_MESSAGES,
                configRoot.disableContextualErrorMessages());

        Optional<String> userAgent = oneOf(restClientConfig.userAgent(), configRoot.userAgent());
        if (userAgent.isPresent()) {
            builder.userAgent(userAgent.get());
        }

        Optional<Integer> maxChunkSize = oneOf(
                restClientConfig.maxChunkSize().map(intChunkSize()),
                restClientConfig.multipart().maxChunkSize().isPresent()
                        ? Optional.of(restClientConfig.multipart().maxChunkSize().getAsInt())
                        : Optional.empty(),
                configRoot.maxChunkSize().map(intChunkSize()),
                configRoot.multipart().maxChunkSize().isPresent()
                        ? Optional.of(restClientConfig.multipart().maxChunkSize().getAsInt())
                        : Optional.empty());
        builder.property(QuarkusRestClientProperties.MAX_CHUNK_SIZE, maxChunkSize.orElse(DEFAULT_MAX_CHUNK_SIZE));

        Optional<Boolean> enableCompressions = oneOf(restClientConfig.enableCompression(), configRoot.enableCompression());
        if (enableCompressions.isPresent()) {
            builder.enableCompression(enableCompressions.get());
        }

        Boolean http2 = oneOf(restClientConfig.http2()).orElse(configRoot.http2());
        builder.property(QuarkusRestClientProperties.HTTP2, http2);

        Optional<MemorySize> http2UpgradeMaxContentLength = oneOf(restClientConfig.http2UpgradeMaxContentLength(),
                configRoot.http2UpgradeMaxContentLength());
        if (http2UpgradeMaxContentLength.isPresent()) {
            builder.property(QuarkusRestClientProperties.HTTP2_UPGRADE_MAX_CONTENT_LENGTH,
                    (int) http2UpgradeMaxContentLength.get().asLongValue());
        }

        Optional<Boolean> alpn = oneOf(restClientConfig.alpn(), configRoot.alpn());
        if (alpn.isPresent()) {
            builder.property(QuarkusRestClientProperties.ALPN, alpn.get());
        }

        Boolean captureStacktrace = oneOf(restClientConfig.captureStacktrace()).orElse(configRoot.captureStacktrace());
        builder.property(QuarkusRestClientProperties.CAPTURE_STACKTRACE, captureStacktrace);

        builder.disableDefaultMapper(restClientConfig.disableDefaultMapper());
    }

    private static Function<MemorySize, Integer> intChunkSize() {
        return m -> (int) m.asLongValue();
    }

    private void configureProxy(QuarkusRestClientBuilder builder) {
        Optional<String> maybeProxy = oneOf(restClientConfig.proxyAddress(), configRoot.proxyAddress());
        if (maybeProxy.isEmpty()) {
            return;
        }

        String proxyAddress = maybeProxy.get();
        if (proxyAddress.equals("none")) {
            builder.proxyAddress("none", 0);
        } else {
            ProxyAddressUtil.HostAndPort hostAndPort = ProxyAddressUtil.parseAddress(proxyAddress);
            builder.proxyAddress(hostAndPort.host, hostAndPort.port);

            oneOf(restClientConfig.proxyUser(), configRoot.proxyUser()).ifPresent(builder::proxyUser);
            oneOf(restClientConfig.proxyPassword(), configRoot.proxyPassword()).ifPresent(builder::proxyPassword);
            oneOf(restClientConfig.nonProxyHosts(), configRoot.nonProxyHosts()).ifPresent(builder::nonProxyHosts);
        }
    }

    private void configureQueryParamStyle(QuarkusRestClientBuilder builder) {
        Optional<QueryParamStyle> maybeQueryParamStyle = oneOf(restClientConfig.queryParamStyle(),
                configRoot.queryParamStyle());
        if (maybeQueryParamStyle.isPresent()) {
            QueryParamStyle queryParamStyle = maybeQueryParamStyle.get();
            builder.queryParamStyle(queryParamStyle);
        }
    }

    private void configureRedirects(QuarkusRestClientBuilder builder) {
        OptionalInt maxRedirects = oneOf(restClientConfig.maxRedirects(), configRoot.maxRedirects());
        if (maxRedirects.isPresent()) {
            builder.property(QuarkusRestClientProperties.MAX_REDIRECTS, maxRedirects.getAsInt());
        }

        Optional<Boolean> maybeFollowRedirects = oneOf(restClientConfig.followRedirects(), configRoot.followRedirects());
        if (maybeFollowRedirects.isPresent()) {
            builder.followRedirects(maybeFollowRedirects.get());
        }
    }

    private void configureShared(QuarkusRestClientBuilder builder) {
        Optional<Boolean> shared = restClientConfig.shared();
        if (shared.isPresent()) {
            builder.property(QuarkusRestClientProperties.SHARED, shared.get());

            if (shared.get()) {
                // Name is only used if shared = true
                Optional<String> name = restClientConfig.name();
                if (name.isPresent()) {
                    builder.property(QuarkusRestClientProperties.NAME, name.get());
                }
            }
        }
    }

    private void configureTLS(QuarkusRestClientBuilder builder) {
        Optional<TlsConfiguration> maybeConfiguration = resolveTlsConfigurationForRegistry();
        if (maybeConfiguration.isPresent()) {
            builder.tlsConfiguration(maybeConfiguration.get());
        } else {
            configureTLSFromProperties(builder);
        }
    }

    private Optional<TlsConfiguration> resolveTlsConfigurationForRegistry() {
        if (Arc.container() != null) {
            var registry = Arc.container().select(TlsConfigurationRegistry.class).orNull();
            if (registry != null) {
                Optional<String> maybeTlsConfigurationName = oneOf(restClientConfig.tlsConfigurationName(),
                        configRoot.tlsConfigurationName());
                return TlsConfiguration.from(registry, maybeTlsConfigurationName);
            }
        }
        return Optional.empty();
    }

    private void configureTLSFromProperties(QuarkusRestClientBuilder builder) {
        Optional<String> maybeTrustStore = oneOf(restClientConfig.trustStore(), configRoot.trustStore());
        if (maybeTrustStore.isPresent() && !maybeTrustStore.get().isBlank() && !NONE.equals(maybeTrustStore.get())) {
            registerTrustStore(maybeTrustStore.get(), builder);
        }

        Optional<String> maybeKeyStore = oneOf(restClientConfig.keyStore(), configRoot.keyStore());
        if (maybeKeyStore.isPresent() && !maybeKeyStore.get().isBlank() && !NONE.equals(maybeKeyStore.get())) {
            registerKeyStore(maybeKeyStore.get(), builder);
        }

        Optional<String> maybeHostnameVerifier = oneOf(restClientConfig.hostnameVerifier(), configRoot.hostnameVerifier());
        if (maybeHostnameVerifier.isPresent()) {
            registerHostnameVerifier(maybeHostnameVerifier.get(), builder);
        }

        oneOf(restClientConfig.verifyHost(), configRoot.verifyHost()).ifPresent(builder::verifyHost);
    }

    private void registerHostnameVerifier(String verifier, QuarkusRestClientBuilder builder) {
        try {
            Class<?> verifierClass = Thread.currentThread().getContextClassLoader().loadClass(verifier);
            builder.hostnameVerifier((HostnameVerifier) verifierClass.getDeclaredConstructor().newInstance());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Could not find a public, no-argument constructor for the hostname verifier class " + verifier, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find hostname verifier class " + verifier, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(
                    "Failed to instantiate hostname verifier class " + verifier
                            + ". Make sure it has a public, no-argument constructor",
                    e);
        } catch (ClassCastException e) {
            throw new RuntimeException("The provided hostname verifier " + verifier + " is not an instance of HostnameVerifier",
                    e);
        }
    }

    private void registerKeyStore(String keyStorePath, QuarkusRestClientBuilder builder) {
        Optional<String> keyStorePassword = oneOf(restClientConfig.keyStorePassword(), configRoot.keyStorePassword());
        Optional<String> keyStoreType = oneOf(restClientConfig.keyStoreType(), configRoot.keyStoreType());

        try {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType.orElse("JKS"));
            if (keyStorePassword.isEmpty()) {
                throw new IllegalArgumentException("No password provided for keystore");
            }
            String password = keyStorePassword.get();

            try (InputStream input = locateStream(keyStorePath)) {
                keyStore.load(input, password.toCharArray());
            } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("Failed to initialize trust store from classpath resource " + keyStorePath,
                        e);
            }

            builder.keyStore(keyStore, password);
        } catch (KeyStoreException e) {
            throw new IllegalArgumentException("Failed to initialize trust store from " + keyStorePath, e);
        }
    }

    private void registerTrustStore(String trustStorePath, QuarkusRestClientBuilder builder) {
        Optional<String> maybeTrustStorePassword = oneOf(restClientConfig.trustStorePassword(),
                configRoot.trustStorePassword());
        Optional<String> maybeTrustStoreType = oneOf(restClientConfig.trustStoreType(), configRoot.trustStoreType());

        try {
            KeyStore trustStore = KeyStore.getInstance(maybeTrustStoreType.orElse("JKS"));
            if (maybeTrustStorePassword.isEmpty()) {
                throw new IllegalArgumentException("No password provided for truststore");
            }
            String password = maybeTrustStorePassword.get();

            try (InputStream input = locateStream(trustStorePath)) {
                trustStore.load(input, password.toCharArray());
            } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("Failed to initialize trust store from classpath resource " + trustStorePath,
                        e);
            }

            builder.trustStore(trustStore, password);
        } catch (KeyStoreException e) {
            throw new IllegalArgumentException("Failed to initialize trust store from " + trustStorePath, e);
        }
    }

    private InputStream locateStream(String path) throws FileNotFoundException {
        if (path.startsWith("classpath:")) {
            path = path.replaceFirst("classpath:", "");
            InputStream resultStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (resultStream == null) {
                resultStream = getClass().getResourceAsStream(path);
            }
            if (resultStream == null) {
                throw new IllegalArgumentException(
                        "Classpath resource " + path + " not found for MicroProfile Rest Client SSL configuration");
            }
            return resultStream;
        } else {
            if (path.startsWith("file:")) {
                path = path.replaceFirst("file:", "");
            }
            File certificateFile = new File(path);
            if (!certificateFile.isFile()) {
                throw new IllegalArgumentException(
                        "Certificate file: " + path + " not found for MicroProfile Rest Client SSL configuration");
            }
            return new FileInputStream(certificateFile);
        }
    }

    private void configureProviders(QuarkusRestClientBuilder builder) {
        Optional<String> maybeProviders = oneOf(restClientConfig.providers(), configRoot.providers());
        if (maybeProviders.isPresent()) {
            registerProviders(builder, maybeProviders.get());
        }
    }

    private void registerProviders(QuarkusRestClientBuilder builder, String providersAsString) {
        for (String s : providersAsString.split(",")) {
            builder.register(providerClassForName(s.trim()));
        }
    }

    private Class<?> providerClassForName(String name) {
        try {
            return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find provider class: " + name);
        }
    }

    private void configureTimeouts(QuarkusRestClientBuilder builder) {
        Long connectTimeout = restClientConfig.connectTimeout().orElse(this.configRoot.connectTimeout());
        if (connectTimeout != null) {
            builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        }

        Long readTimeout = restClientConfig.readTimeout().orElse(this.configRoot.readTimeout());
        if (readTimeout != null) {
            builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        }
    }

    private void configureBaseUrl(QuarkusRestClientBuilder builder) {
        Optional<String> propertyOptional = oneOf(restClientConfig.uriReload(), restClientConfig.urlReload());
        if (((baseUriFromAnnotation == null) || baseUriFromAnnotation.isEmpty())
                && propertyOptional.isEmpty()) {
            String propertyPrefix = configKey != null ? configKey : "\"" + jaxrsInterface.getName() + "\"";
            throw new IllegalArgumentException(
                    String.format(
                            "Unable to determine the proper baseUrl/baseUri. " +
                                    "Consider registering using @RegisterRestClient(baseUri=\"someuri\"), @RegisterRestClient(configKey=\"orkey\"), "
                                    +
                                    "or by adding '%s' or '%s' to your Quarkus configuration",
                            String.format(REST_URL_FORMAT, propertyPrefix), String.format(REST_URI_FORMAT, propertyPrefix)));
        }
        String baseUrl = propertyOptional.orElse(baseUriFromAnnotation);

        try {
            builder.baseUri(new URI(baseUrl));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("The value of URL was invalid " + baseUrl, e);
        }
    }

    @SafeVarargs
    private static <T> Optional<T> oneOf(Optional<T>... optionals) {
        for (Optional<T> o : optionals) {
            if (o != null && o.isPresent()) {
                return o;
            }
        }
        return Optional.empty();
    }

    private static OptionalInt oneOf(OptionalInt... optionals) {
        for (OptionalInt o : optionals) {
            if (o != null && o.isPresent()) {
                return o;
            }
        }
        return OptionalInt.empty();
    }
}
