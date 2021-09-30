package io.quarkus.restclient.runtime;

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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;

public class RestClientBase {

    public static final String QUARKUS_CONFIG_REST_URL_FORMAT = "quarkus.rest-config.\"%s\".url";
    public static final String QUARKUS_CONFIG_REST_URI_FORMAT = "quarkus.rest-config.\"%s\".uri";

    private final Class<?> proxyType;
    private final String baseUriFromAnnotation;
    private final Class<?>[] annotationProviders;
    private final RestClientsConfig configRoot;
    private final String configKey;

    public RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String configKey,
            Class<?>[] annotationProviders) {
        this(proxyType, baseUriFromAnnotation, configKey, annotationProviders,
                getConfigRoot());
    }

    RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String configKey,
            Class<?>[] annotationProviders, RestClientsConfig configRoot) {
        this.proxyType = proxyType;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.configKey = configKey;
        this.annotationProviders = annotationProviders;
        this.configRoot = configRoot;
    }

    public Object create() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        configureBaseUrl(builder);
        configureTimeouts(builder);
        configureProviders(builder);
        configureSsl(builder);
        configureProxy(builder);
        configureRedirects(builder);
        configureQueryParamStyle(builder);
        configureCustomProperties(builder);
        // If we have context propagation, then propagate context to the async client threads
        InstanceHandle<ManagedExecutor> managedExecutor = Arc.container().instance(ManagedExecutor.class);
        if (managedExecutor.isAvailable()) {
            builder.executorService(managedExecutor.get());
        }

        return builder.build(proxyType);
    }

    void configureCustomProperties(RestClientBuilder builder) {
        Optional<Integer> connectionPoolSize = oneOf(clientConfigByClassName().connectionPoolSize,
                clientConfigByConfigKey().connectionPoolSize);
        if (connectionPoolSize.isPresent()) {
            builder.property("resteasy.connectionPoolSize", connectionPoolSize.get());
        }

        Optional<Integer> connectionTTL = oneOf(clientConfigByClassName().connectionTTL,
                clientConfigByConfigKey().connectionTTL);
        if (connectionTTL.isPresent()) {
            builder.property("resteasy.connectionTTL",
                    Arrays.asList(connectionTTL.get(), TimeUnit.MILLISECONDS));
        }
    }

    void configureProxy(RestClientBuilder builder) {
        Optional<String> proxyAddress = oneOf(clientConfigByClassName().proxyAddress, clientConfigByConfigKey().proxyAddress);
        if (proxyAddress.isPresent()) {
            String proxyString = proxyAddress.get();

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

    void configureRedirects(RestClientBuilder builder) {
        Optional<Boolean> followRedirects = oneOf(clientConfigByClassName().followRedirects,
                clientConfigByConfigKey().followRedirects);
        if (followRedirects.isPresent()) {
            builder.followRedirects(followRedirects.get());
        }
    }

    void configureQueryParamStyle(RestClientBuilder builder) {
        Optional<QueryParamStyle> queryParamStyle = oneOf(clientConfigByClassName().queryParamStyle,
                clientConfigByConfigKey().queryParamStyle);
        if (queryParamStyle.isPresent()) {
            builder.queryParamStyle(queryParamStyle.get());
        }
    }

    void configureSsl(RestClientBuilder builder) {
        Optional<String> trustStore = oneOf(clientConfigByClassName().trustStore, clientConfigByConfigKey().trustStore);
        if (trustStore.isPresent()) {
            registerTrustStore(trustStore.get(), builder);
        }

        Optional<String> keyStore = oneOf(clientConfigByClassName().keyStore, clientConfigByConfigKey().keyStore);
        if (keyStore.isPresent()) {
            registerKeyStore(keyStore.get(), builder);
        }

        Optional<String> hostnameVerifier = oneOf(clientConfigByClassName().hostnameVerifier,
                clientConfigByConfigKey().hostnameVerifier);
        if (hostnameVerifier.isPresent()) {
            registerHostnameVerifier(hostnameVerifier.get(), builder);
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
        try {
            Optional<String> keyStoreType = oneOf(clientConfigByClassName().keyStoreType,
                    clientConfigByConfigKey().keyStoreType);
            KeyStore keyStore = KeyStore.getInstance(keyStoreType.orElse("JKS"));

            Optional<String> keyStorePassword = oneOf(clientConfigByClassName().keyStorePassword,
                    clientConfigByConfigKey().keyStorePassword);
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
        try {
            Optional<String> trustStoreType = oneOf(clientConfigByClassName().trustStoreType,
                    clientConfigByConfigKey().trustStoreType);
            KeyStore trustStore = KeyStore.getInstance(trustStoreType.orElse("JKS"));

            Optional<String> trustStorePassword = oneOf(clientConfigByClassName().trustStorePassword,
                    clientConfigByConfigKey().trustStorePassword);
            if (trustStorePassword.isEmpty()) {
                throw new IllegalArgumentException("No password provided for truststore");
            }
            String password = trustStorePassword.get();

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

    void configureProviders(RestClientBuilder builder) {
        Optional<String> providers = oneOf(clientConfigByClassName().providers, clientConfigByConfigKey().providers);

        if (providers.isPresent()) {
            registerProviders(builder, providers.get());
        }
        if (annotationProviders != null) {
            for (Class<?> annotationProvider : annotationProviders) {
                builder.register(annotationProvider);
            }
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

    void configureTimeouts(RestClientBuilder builder) {
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

    void configureBaseUrl(RestClientBuilder builder) {
        Optional<String> baseUrlOptional = oneOf(clientConfigByClassName().uri, clientConfigByConfigKey().uri);
        if (baseUrlOptional.isEmpty()) {
            baseUrlOptional = oneOf(clientConfigByClassName().url, clientConfigByConfigKey().url);
        }
        if (((baseUriFromAnnotation == null) || baseUriFromAnnotation.isEmpty())
                && baseUrlOptional.isEmpty()) {
            String propertyPrefix = configKey != null ? configKey : proxyType.getName();
            throw new IllegalArgumentException(
                    String.format(
                            "Unable to determine the proper baseUrl/baseUri. " +
                                    "Consider registering using @RegisterRestClient(baseUri=\"someuri\"), @RegisterRestClient(configKey=\"orkey\"), "
                                    +
                                    "or by adding '%s' or '%s' to your Quarkus configuration",
                            String.format(QUARKUS_CONFIG_REST_URL_FORMAT, propertyPrefix),
                            String.format(QUARKUS_CONFIG_REST_URI_FORMAT, propertyPrefix)));
        }
        String baseUrl = baseUrlOptional.orElse(baseUriFromAnnotation);

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
            throw new IllegalStateException("Unable to find the RestClientConfigRootProvider");
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
        if (this.configRoot.configs.containsKey(proxyType.getName())) {
            return this.configRoot.configs.get(proxyType.getName());
        }
        if (this.configRoot.configs.containsKey(proxyType.getSimpleName())) {
            return this.configRoot.configs.get(proxyType.getSimpleName());
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
