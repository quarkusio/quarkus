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
import io.quarkus.restclient.NoopHostnameVerifier;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;

public class RestClientBase {

    public static final String QUARKUS_CONFIG_REST_URL_FORMAT = "quarkus.rest-client.\"%s\".url";
    public static final String QUARKUS_CONFIG_REST_URI_FORMAT = "quarkus.rest-client.\"%s\".uri";
    private static final String NONE = "none";

    private final Class<?> proxyType;
    private final String baseUriFromAnnotation;
    private final Class<?>[] clientProviders;
    private final RestClientsConfig configRoot;
    private final String configKey;

    public RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String configKey,
            Class<?>[] clientProviders) {
        this(proxyType, baseUriFromAnnotation, configKey, clientProviders,
                RestClientsConfig.getInstance());
    }

    RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String configKey,
            Class<?>[] clientProviders, RestClientsConfig configRoot) {
        this.proxyType = proxyType;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.configKey = configKey;
        this.clientProviders = clientProviders;
        this.configRoot = configRoot;
    }

    public Object create() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        configureBuilder(builder);
        // If we have context propagation, then propagate context to the async client threads
        InstanceHandle<ManagedExecutor> managedExecutor = Arc.container().instance(ManagedExecutor.class);
        if (managedExecutor.isAvailable()) {
            builder.executorService(managedExecutor.get());
        }

        return builder.build(proxyType);
    }

    protected void configureBuilder(RestClientBuilder builder) {
        configureBaseUrl(builder);
        configureTimeouts(builder);
        configureProviders(builder);
        configureSsl(builder);
        configureProxy(builder);
        configureRedirects(builder);
        configureQueryParamStyle(builder);
        configureCustomProperties(builder);
    }

    protected void configureCustomProperties(RestClientBuilder builder) {
        Optional<Integer> connectionPoolSize = oneOf(clientConfigByClassName().connectionPoolSize,
                clientConfigByConfigKey().connectionPoolSize, configRoot.connectionPoolSize);
        if (connectionPoolSize.isPresent()) {
            builder.property("resteasy.connectionPoolSize", connectionPoolSize.get());
        }

        Optional<Integer> connectionTTL = oneOf(clientConfigByClassName().connectionTTL,
                clientConfigByConfigKey().connectionTTL, configRoot.connectionTTL);
        if (connectionTTL.isPresent()) {
            builder.property("resteasy.connectionTTL",
                    Arrays.asList(connectionTTL.get(), TimeUnit.MILLISECONDS));
        }
    }

    protected void configureProxy(RestClientBuilder builder) {
        Optional<String> proxyAddress = oneOf(clientConfigByClassName().proxyAddress, clientConfigByConfigKey().proxyAddress,
                configRoot.proxyAddress);
        if (proxyAddress.isPresent() && !NONE.equals(proxyAddress.get())) {
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

    protected void configureRedirects(RestClientBuilder builder) {
        Optional<Boolean> followRedirects = oneOf(clientConfigByClassName().followRedirects,
                clientConfigByConfigKey().followRedirects, configRoot.followRedirects);
        if (followRedirects.isPresent()) {
            builder.followRedirects(followRedirects.get());
        }
    }

    protected void configureQueryParamStyle(RestClientBuilder builder) {
        Optional<QueryParamStyle> queryParamStyle = oneOf(clientConfigByClassName().queryParamStyle,
                clientConfigByConfigKey().queryParamStyle, configRoot.queryParamStyle);
        if (queryParamStyle.isPresent()) {
            builder.queryParamStyle(queryParamStyle.get());
        }
    }

    protected void configureSsl(RestClientBuilder builder) {
        Optional<String> trustStore = oneOf(clientConfigByClassName().trustStore, clientConfigByConfigKey().trustStore,
                configRoot.trustStore);
        if (trustStore.isPresent() && !trustStore.get().isBlank() && !NONE.equals(trustStore.get())) {
            registerTrustStore(trustStore.get(), builder);
        }

        Optional<String> keyStore = oneOf(clientConfigByClassName().keyStore, clientConfigByConfigKey().keyStore,
                configRoot.keyStore);
        if (keyStore.isPresent() && !keyStore.get().isBlank() && !NONE.equals(keyStore.get())) {
            registerKeyStore(keyStore.get(), builder);
        }

        Optional<String> hostnameVerifier = oneOf(clientConfigByClassName().hostnameVerifier,
                clientConfigByConfigKey().hostnameVerifier, configRoot.hostnameVerifier);
        if (hostnameVerifier.isPresent()) {
            registerHostnameVerifier(hostnameVerifier.get(), builder);
        } else {
            // If `verify-host` is disabled, we configure the client using the `NoopHostnameVerifier` verifier.
            Optional<Boolean> verifyHost = oneOf(clientConfigByClassName().verifyHost, clientConfigByConfigKey().verifyHost,
                    configRoot.verifyHost);
            if (verifyHost.isPresent() && !verifyHost.get()) {
                registerHostnameVerifier(NoopHostnameVerifier.class.getName(), builder);
            }
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
                    clientConfigByConfigKey().keyStoreType, configRoot.keyStoreType);
            KeyStore keyStore = KeyStore.getInstance(keyStoreType.orElse("JKS"));

            Optional<String> keyStorePassword = oneOf(clientConfigByClassName().keyStorePassword,
                    clientConfigByConfigKey().keyStorePassword, configRoot.keyStorePassword);
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
                    clientConfigByConfigKey().trustStoreType, configRoot.trustStoreType);
            KeyStore trustStore = KeyStore.getInstance(trustStoreType.orElse("JKS"));

            Optional<String> trustStorePassword = oneOf(clientConfigByClassName().trustStorePassword,
                    clientConfigByConfigKey().trustStorePassword, configRoot.trustStorePassword);
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

    protected void configureProviders(RestClientBuilder builder) {
        Optional<String> providers = oneOf(clientConfigByClassName().providers, clientConfigByConfigKey().providers,
                configRoot.providers);

        if (providers.isPresent()) {
            registerProviders(builder, providers.get());
        }
        if (clientProviders != null) {
            for (Class<?> annotationProvider : clientProviders) {
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

    protected void configureTimeouts(RestClientBuilder builder) {
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

    protected void configureBaseUrl(RestClientBuilder builder) {
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
            if (e.getMessage().contains(
                    "It must be enabled by adding the --enable-url-protocols=https option to the native-image command")) {
                throw new IllegalArgumentException(baseUrl
                        + " requires SSL support but it is disabled. You probably have set quarkus.ssl.native to false.");
            }
            throw new IllegalArgumentException("The value of URL was invalid " + baseUrl, e);
        }
    }

    private RestClientConfig clientConfigByConfigKey() {
        return this.configRoot.getClientConfig(this.configKey);
    }

    private RestClientConfig clientConfigByClassName() {
        return this.configRoot.getClientConfig(this.proxyType);
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
