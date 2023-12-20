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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.HostnameVerifier;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.runtime.configuration.MemorySize;

public class RestClientCDIDelegateBuilder<T> {

    private static final String REST_URL_FORMAT = "quarkus.rest-client.%s.url";
    private static final String REST_URI_FORMAT = "quarkus.rest-client.%s.uri";
    private static final String NONE = "none";

    private final Class<T> jaxrsInterface;
    private final String baseUriFromAnnotation;
    private final String configKey;
    private final RestClientsConfig configRoot;

    public static <T> T createDelegate(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey) {
        return new RestClientCDIDelegateBuilder<T>(jaxrsInterface, baseUriFromAnnotation, configKey).build();
    }

    private RestClientCDIDelegateBuilder(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey) {
        this(jaxrsInterface, baseUriFromAnnotation, configKey, RestClientsConfig.getInstance());
    }

    RestClientCDIDelegateBuilder(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey,
            RestClientsConfig configRoot) {
        this.jaxrsInterface = jaxrsInterface;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.configKey = configKey;
        this.configRoot = configRoot;
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
        configureSsl(builder);
        configureRedirects(builder);
        configureQueryParamStyle(builder);
        configureProxy(builder);
        configureShared(builder);
        configureCustomProperties(builder);
    }

    private void configureCustomProperties(QuarkusRestClientBuilder builder) {
        Optional<String> encoder = configRoot.multipartPostEncoderMode;
        if (encoder != null && encoder.isPresent()) {
            PausableHttpPostRequestEncoder.EncoderMode mode = PausableHttpPostRequestEncoder.EncoderMode
                    .valueOf(encoder.get().toUpperCase(Locale.ROOT));
            builder.property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE, mode);
        }

        Optional<Integer> poolSize = oneOf(clientConfigByClassName().connectionPoolSize,
                clientConfigByConfigKey().connectionPoolSize, configRoot.connectionPoolSize);
        if (poolSize.isPresent()) {
            builder.property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, poolSize.get());
        }

        Optional<Integer> connectionTTL = oneOf(clientConfigByClassName().connectionTTL,
                clientConfigByConfigKey().connectionTTL, configRoot.connectionTTL);
        if (connectionTTL.isPresent()) {
            // configuration bean contains value in milliseconds
            int connectionTTLSeconds = connectionTTL.get() / 1000;
            builder.property(QuarkusRestClientProperties.CONNECTION_TTL, connectionTTLSeconds);
        }

        Optional<Boolean> keepAliveEnabled = oneOf(clientConfigByClassName().keepAliveEnabled,
                clientConfigByConfigKey().keepAliveEnabled, configRoot.keepAliveEnabled);
        if (keepAliveEnabled.isPresent()) {
            builder.property(QuarkusRestClientProperties.KEEP_ALIVE_ENABLED, keepAliveEnabled.get());
        }

        Map<String, String> headers = clientConfigByClassName().headers;
        if (headers == null || headers.isEmpty()) {
            headers = clientConfigByConfigKey().headers;
        }
        if (headers == null || headers.isEmpty()) {
            headers = configRoot.headers;
        }
        if (headers != null && !headers.isEmpty()) {
            builder.property(QuarkusRestClientProperties.STATIC_HEADERS, headers);
        }

        builder.property(QuarkusRestClientProperties.DISABLE_CONTEXTUAL_ERROR_MESSAGES,
                configRoot.disableContextualErrorMessages);

        Optional<String> userAgent = oneOf(clientConfigByClassName().userAgent,
                clientConfigByConfigKey().userAgent, configRoot.userAgent);
        if (userAgent.isPresent()) {
            builder.property(QuarkusRestClientProperties.USER_AGENT, userAgent.get());
        }

        Optional<Integer> maxChunkSize = oneOf(
                clientConfigByClassName().maxChunkSize.map(intChunkSize()),
                clientConfigByClassName().multipart.maxChunkSize,
                clientConfigByConfigKey().maxChunkSize.map(intChunkSize()),
                clientConfigByConfigKey().multipart.maxChunkSize,
                configRoot.maxChunkSize.map(intChunkSize()),
                configRoot.multipart.maxChunkSize);
        builder.property(QuarkusRestClientProperties.MAX_CHUNK_SIZE, maxChunkSize.orElse(DEFAULT_MAX_CHUNK_SIZE));

        Boolean http2 = oneOf(clientConfigByClassName().http2,
                clientConfigByConfigKey().http2).orElse(configRoot.http2);
        builder.property(QuarkusRestClientProperties.HTTP2, http2);

        Optional<Boolean> alpn = oneOf(clientConfigByClassName().alpn,
                clientConfigByConfigKey().alpn, configRoot.alpn);
        if (alpn.isPresent()) {
            builder.property(QuarkusRestClientProperties.ALPN, alpn.get());
        }

        Boolean captureStacktrace = oneOf(clientConfigByClassName().captureStacktrace,
                clientConfigByConfigKey().captureStacktrace).orElse(configRoot.captureStacktrace);
        builder.property(QuarkusRestClientProperties.CAPTURE_STACKTRACE, captureStacktrace);
    }

    private static Function<MemorySize, Integer> intChunkSize() {
        return m -> (int) m.asLongValue();
    }

    private void configureProxy(QuarkusRestClientBuilder builder) {
        Optional<String> maybeProxy = oneOf(clientConfigByClassName().proxyAddress, clientConfigByConfigKey().proxyAddress,
                configRoot.proxyAddress);
        if (maybeProxy.isEmpty()) {
            return;
        }

        String proxyAddress = maybeProxy.get();
        if (proxyAddress.equals("none")) {
            builder.proxyAddress("none", 0);
        } else {
            ProxyAddressUtil.HostAndPort hostAndPort = ProxyAddressUtil.parseAddress(proxyAddress);
            builder.proxyAddress(hostAndPort.host, hostAndPort.port);

            oneOf(clientConfigByClassName().proxyUser, clientConfigByConfigKey().proxyUser, configRoot.proxyUser)
                    .ifPresent(builder::proxyUser);
            oneOf(clientConfigByClassName().proxyPassword, clientConfigByConfigKey().proxyPassword, configRoot.proxyPassword)
                    .ifPresent(builder::proxyPassword);
            oneOf(clientConfigByClassName().nonProxyHosts, clientConfigByConfigKey().nonProxyHosts, configRoot.nonProxyHosts)
                    .ifPresent(builder::nonProxyHosts);
        }
    }

    private void configureQueryParamStyle(QuarkusRestClientBuilder builder) {
        Optional<QueryParamStyle> maybeQueryParamStyle = oneOf(clientConfigByClassName().queryParamStyle,
                clientConfigByConfigKey().queryParamStyle, configRoot.queryParamStyle);
        if (maybeQueryParamStyle.isPresent()) {
            QueryParamStyle queryParamStyle = maybeQueryParamStyle.get();
            builder.queryParamStyle(queryParamStyle);
        }
    }

    private void configureRedirects(QuarkusRestClientBuilder builder) {
        Optional<Integer> maxRedirects = oneOf(clientConfigByClassName().maxRedirects,
                clientConfigByConfigKey().maxRedirects, configRoot.maxRedirects);
        if (maxRedirects.isPresent()) {
            builder.property(QuarkusRestClientProperties.MAX_REDIRECTS, maxRedirects.get());
        }

        Optional<Boolean> maybeFollowRedirects = oneOf(clientConfigByClassName().followRedirects,
                clientConfigByConfigKey().followRedirects, configRoot.followRedirects);
        if (maybeFollowRedirects.isPresent()) {
            builder.followRedirects(maybeFollowRedirects.get());
        }
    }

    private void configureShared(QuarkusRestClientBuilder builder) {
        Optional<Boolean> shared = oneOf(clientConfigByClassName().shared,
                clientConfigByConfigKey().shared);
        if (shared.isPresent()) {
            builder.property(QuarkusRestClientProperties.SHARED, shared.get());

            if (shared.get()) {
                // Name is only used if shared = true
                Optional<String> name = oneOf(clientConfigByClassName().name,
                        clientConfigByConfigKey().name);
                if (name.isPresent()) {
                    builder.property(QuarkusRestClientProperties.NAME, name.get());
                }
            }
        }
    }

    private void configureSsl(QuarkusRestClientBuilder builder) {

        Optional<String> maybeTrustStore = oneOf(clientConfigByClassName().trustStore, clientConfigByConfigKey().trustStore,
                configRoot.trustStore);
        if (maybeTrustStore.isPresent() && !maybeTrustStore.get().isBlank() && !NONE.equals(maybeTrustStore.get())) {
            registerTrustStore(maybeTrustStore.get(), builder);
        }

        Optional<String> maybeKeyStore = oneOf(clientConfigByClassName().keyStore, clientConfigByConfigKey().keyStore,
                configRoot.keyStore);
        if (maybeKeyStore.isPresent() && !maybeKeyStore.get().isBlank() && !NONE.equals(maybeKeyStore.get())) {
            registerKeyStore(maybeKeyStore.get(), builder);
        }

        Optional<String> maybeHostnameVerifier = oneOf(clientConfigByClassName().hostnameVerifier,
                clientConfigByConfigKey().hostnameVerifier, configRoot.hostnameVerifier);
        if (maybeHostnameVerifier.isPresent()) {
            registerHostnameVerifier(maybeHostnameVerifier.get(), builder);
        }

        oneOf(clientConfigByClassName().verifyHost, clientConfigByConfigKey().verifyHost, configRoot.verifyHost)
                .ifPresent(builder::verifyHost);
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
        Optional<String> keyStorePassword = oneOf(clientConfigByClassName().keyStorePassword,
                clientConfigByConfigKey().keyStorePassword, configRoot.keyStorePassword);
        Optional<String> keyStoreType = oneOf(clientConfigByClassName().keyStoreType,
                clientConfigByConfigKey().keyStoreType, configRoot.keyStoreType);

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
        Optional<String> maybeTrustStorePassword = oneOf(clientConfigByClassName().trustStorePassword,
                clientConfigByConfigKey().trustStorePassword, configRoot.trustStorePassword);
        Optional<String> maybeTrustStoreType = oneOf(clientConfigByClassName().trustStoreType,
                clientConfigByConfigKey().trustStoreType, configRoot.trustStoreType);

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
        Optional<String> maybeProviders = oneOf(clientConfigByClassName().providers, clientConfigByConfigKey().providers,
                configRoot.providers);
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
        Long connectTimeout = oneOf(clientConfigByClassName().connectTimeout,
                clientConfigByConfigKey().connectTimeout).orElse(this.configRoot.connectTimeout);
        if (connectTimeout != null) {
            builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        }

        Long readTimeout = oneOf(clientConfigByClassName().readTimeout,
                clientConfigByConfigKey().readTimeout).orElse(this.configRoot.readTimeout);
        if (readTimeout != null) {
            builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        }
    }

    private void configureBaseUrl(QuarkusRestClientBuilder builder) {
        Optional<String> propertyOptional = oneOf(clientConfigByClassName().uri,
                clientConfigByConfigKey().uri);

        if (propertyOptional.isEmpty()) {
            propertyOptional = oneOf(clientConfigByClassName().url,
                    clientConfigByConfigKey().url);
        }
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

    private RestClientConfig clientConfigByConfigKey() {
        return this.configRoot.getClientConfig(configKey);
    }

    private RestClientConfig clientConfigByClassName() {
        return this.configRoot.getClientConfig(jaxrsInterface);
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
}
