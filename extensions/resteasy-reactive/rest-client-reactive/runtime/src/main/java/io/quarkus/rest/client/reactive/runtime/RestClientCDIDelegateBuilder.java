package io.quarkus.rest.client.reactive.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;

import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;

public class RestClientCDIDelegateBuilder<T> {

    private static final String REST_URL_FORMAT = "quarkus.rest-client.%s.url";
    private static final String REST_URI_FORMAT = "quarkus.rest-client.%s.uri";

    private final Class<T> jaxrsInterface;
    private final String baseUriFromAnnotation;
    private final String configKey;
    private final RestClientsConfig configRoot;

    public static <T> T createDelegate(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey) {
        return new RestClientCDIDelegateBuilder<T>(jaxrsInterface, baseUriFromAnnotation, configKey).build();
    }

    private RestClientCDIDelegateBuilder(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey) {
        this(jaxrsInterface, baseUriFromAnnotation, configKey, getConfigRoot());
    }

    RestClientCDIDelegateBuilder(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey,
            RestClientsConfig configRoot) {
        this.jaxrsInterface = jaxrsInterface;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.configKey = configKey;
        this.configRoot = configRoot;
    }

    private T build() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        return build(builder);
    }

    T build(RestClientBuilder builder) {
        configureBaseUrl(builder);
        configureTimeouts(builder);
        configureProviders(builder);
        configureSsl(builder);
        configureRedirects(builder);
        configureQueryParamStyle(builder);
        configureProxy(builder);
        configureCustomProperties(builder);
        return builder.build(jaxrsInterface);
    }

    private void configureCustomProperties(RestClientBuilder builder) {
        Optional<String> encoder = configRoot.multipartPostEncoderMode;
        if (encoder.isPresent()) {
            HttpPostRequestEncoder.EncoderMode mode = HttpPostRequestEncoder.EncoderMode
                    .valueOf(encoder.get().toUpperCase(Locale.ROOT));
            builder.property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE, mode);
        }

        Optional<Integer> poolSize = oneOf(clientConfigByClassName().connectionPoolSize,
                clientConfigByConfigKey().connectionPoolSize);
        if (poolSize.isPresent()) {
            builder.property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, poolSize.get());
        }

        Optional<Integer> connectionTTL = oneOf(clientConfigByClassName().connectionTTL,
                clientConfigByConfigKey().connectionTTL);
        if (connectionTTL.isPresent()) {
            // configuration bean contains value in milliseconds
            int connectionTTLSeconds = connectionTTL.get() / 1000;
            builder.property(QuarkusRestClientProperties.CONNECTION_TTL, connectionTTLSeconds);
        }
    }

    private void configureProxy(RestClientBuilder builder) {
        Optional<String> maybeProxy = oneOf(clientConfigByClassName().proxyAddress,
                clientConfigByConfigKey().proxyAddress);
        if (maybeProxy.isPresent()) {
            String proxyString = maybeProxy.get();

            int lastColonIndex = proxyString.lastIndexOf(':');

            if (lastColonIndex <= 0 || lastColonIndex == proxyString.length() - 1) {
                throw new RuntimeException("Invalid proxy string. Expected <hostname>:<port>, found '" + proxyString + "'");
            }

            String host = proxyString.substring(0, lastColonIndex);
            int port;
            try {
                port = Integer.parseInt(proxyString.substring(lastColonIndex + 1));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid proxy setting. The port is not a number in '" + proxyString + "'", e);
            }

            builder.proxyAddress(host, port);
        }
    }

    private void configureQueryParamStyle(RestClientBuilder builder) {
        Optional<QueryParamStyle> maybeQueryParamStyle = oneOf(clientConfigByClassName().queryParamStyle,
                clientConfigByConfigKey().queryParamStyle);
        if (maybeQueryParamStyle.isPresent()) {
            QueryParamStyle queryParamStyle = maybeQueryParamStyle.get();
            builder.queryParamStyle(queryParamStyle);
        }
    }

    private void configureRedirects(RestClientBuilder builder) {
        Optional<Integer> maxRedirects = oneOf(clientConfigByClassName().maxRedirects,
                clientConfigByConfigKey().maxRedirects);
        if (maxRedirects.isPresent()) {
            builder.property(QuarkusRestClientProperties.MAX_REDIRECTS, maxRedirects.get());
        }

        Optional<Boolean> maybeFollowRedirects = oneOf(clientConfigByClassName().followRedirects,
                clientConfigByConfigKey().followRedirects);
        if (maybeFollowRedirects.isPresent()) {
            builder.followRedirects(maybeFollowRedirects.get());
        }
    }

    private void configureSsl(RestClientBuilder builder) {

        Optional<String> maybeTrustStore = oneOf(clientConfigByClassName().trustStore,
                clientConfigByConfigKey().trustStore);
        if (maybeTrustStore.isPresent()) {
            registerTrustStore(maybeTrustStore.get(), builder);
        }

        Optional<String> maybeKeyStore = oneOf(clientConfigByClassName().keyStore,
                clientConfigByConfigKey().keyStore);
        if (maybeKeyStore.isPresent()) {
            registerKeyStore(maybeKeyStore.get(), builder);
        }

        Optional<String> maybeHostnameVerifier = oneOf(clientConfigByClassName().hostnameVerifier,
                clientConfigByConfigKey().hostnameVerifier);
        if (maybeHostnameVerifier.isPresent()) {
            registerHostnameVerifier(maybeHostnameVerifier.get(), builder);
        }
    }

    private void registerHostnameVerifier(String verifier, RestClientBuilder builder) {
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

    private void registerKeyStore(String keyStorePath, RestClientBuilder builder) {
        Optional<String> keyStorePassword = oneOf(clientConfigByClassName().keyStorePassword,
                clientConfigByConfigKey().keyStorePassword);
        Optional<String> keyStoreType = oneOf(clientConfigByClassName().keyStoreType,
                clientConfigByConfigKey().keyStoreType);

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

    private void registerTrustStore(String trustStorePath, RestClientBuilder builder) {
        Optional<String> maybeTrustStorePassword = oneOf(clientConfigByClassName().trustStorePassword,
                clientConfigByConfigKey().trustStorePassword);
        Optional<String> maybeTrustStoreType = oneOf(clientConfigByClassName().trustStoreType,
                clientConfigByConfigKey().trustStoreType);

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

            builder.trustStore(trustStore);
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

    private void configureProviders(RestClientBuilder builder) {
        Optional<String> maybeProviders = oneOf(clientConfigByClassName().providers,
                clientConfigByConfigKey().providers);
        if (maybeProviders.isPresent()) {
            registerProviders(builder, maybeProviders.get());
        }
    }

    private void registerProviders(RestClientBuilder builder, String providersAsString) {
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

    private void configureTimeouts(RestClientBuilder builder) {
        Optional<Long> connectTimeout = oneOf(clientConfigByClassName().connectTimeout,
                clientConfigByConfigKey().connectTimeout);
        if (connectTimeout.isPresent()) {
            builder.connectTimeout(connectTimeout.get(), TimeUnit.MILLISECONDS);
        }

        Optional<Long> readTimeout = oneOf(clientConfigByClassName().readTimeout,
                clientConfigByConfigKey().readTimeout);
        if (readTimeout.isPresent()) {
            builder.readTimeout(readTimeout.get(), TimeUnit.MILLISECONDS);
        }
    }

    private void configureBaseUrl(RestClientBuilder builder) {
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
            builder.baseUrl(new URL(baseUrl));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The value of URL was invalid " + baseUrl, e);
        } catch (Exception e) {
            if ("com.oracle.svm.core.jdk.UnsupportedFeatureError".equals(e.getClass().getCanonicalName())) {
                throw new IllegalArgumentException(baseUrl
                        + " requires SSL support but it is disabled. You probably have set quarkus.ssl.native to false.");
            }
            throw e;
        }
    }

    private static RestClientsConfig getConfigRoot() {
        InstanceHandle<RestClientsConfig> configHandle = Arc.container()
                .instance(RestClientsConfig.class);
        if (!configHandle.isAvailable()) {
            throw new IllegalStateException("Unable to find the RestClientsConfig");
        }
        return configHandle.get();
    }

    private RestClientConfig clientConfigByConfigKey() {
        if (configKey != null) {
            return this.configRoot.configs.getOrDefault(configKey, RestClientConfig.EMPTY);
        }
        return RestClientConfig.EMPTY;
    }

    private RestClientConfig clientConfigByClassName() {
        if (this.configRoot.configs.containsKey(jaxrsInterface.getName())) {
            return this.configRoot.configs.get(jaxrsInterface.getName());
        }
        if (this.configRoot.configs.containsKey(jaxrsInterface.getSimpleName())) {
            return this.configRoot.configs.get(jaxrsInterface.getSimpleName());
        }
        return RestClientConfig.EMPTY;
    }

    private static <T> Optional<T> oneOf(Optional<T> o1, Optional<T> o2) {
        if (o1.isPresent()) {
            return o1;
        }
        return o2;
    }
}
