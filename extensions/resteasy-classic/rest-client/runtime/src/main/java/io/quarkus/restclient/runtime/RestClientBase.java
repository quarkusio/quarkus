package io.quarkus.restclient.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.resteasy.client.jaxrs.engines.PassthroughTrustManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientConfigRoot;
import io.quarkus.restclient.config.RestClientConfigRootProvider;
import io.quarkus.runtime.graal.DisabledSSLContext;
import io.quarkus.runtime.ssl.SslContextConfiguration;

public class RestClientBase {
    public static final String MP_REST = "mp-rest";
    public static final String REST_URL_FORMAT = "%s/" + MP_REST + "/url";
    public static final String REST_URI_FORMAT = "%s/" + MP_REST + "/uri";
    public static final String REST_CONNECT_TIMEOUT_FORMAT = "%s/" + MP_REST + "/connectTimeout";
    public static final String REST_READ_TIMEOUT_FORMAT = "%s/" + MP_REST + "/readTimeout";
    public static final String REST_SCOPE_FORMAT = "%s/" + MP_REST + "/scope";
    public static final String REST_PROVIDERS = "%s/" + MP_REST + "/providers";
    public static final String REST_TRUST_STORE = "%s/" + MP_REST + "/trustStore";
    public static final String REST_TRUST_STORE_PASSWORD = "%s/" + MP_REST + "/trustStorePassword";
    public static final String REST_TRUST_STORE_TYPE = "%s/" + MP_REST + "/trustStoreType";
    public static final String REST_KEY_STORE = "%s/" + MP_REST + "/keyStore";
    public static final String REST_KEY_STORE_PASSWORD = "%s/" + MP_REST + "/keyStorePassword";
    public static final String REST_KEY_STORE_TYPE = "%s/" + MP_REST + "/keyStoreType";
    public static final String REST_HOSTNAME_VERIFIER = "%s/" + MP_REST + "/hostnameVerifier";
    public static final String REST_FOLLOW_REDIRECTS = "%s/" + MP_REST + "/followRedirects";
    public static final String REST_QUERY_PARAM_STYLE = "%s/" + MP_REST + "/queryParamStyle";
    public static final String REST_PROXY_ADDRESS = "%s/" + MP_REST + "/proxyAddress";
    public static final String REST_NOOP_HOSTNAME_VERIFIER = "io.quarkus.restclient.NoopHostnameVerifier";
    public static final String TLS_TRUST_ALL = "quarkus.tls.trust-all";

    public static final String QUARKUS_CONFIG_REST_URL_FORMAT = "quarkus.rest-config.\"%s\".url";
    public static final String QUARKUS_CONFIG_REST_URI_FORMAT = "quarkus.rest-config.\"%s\".uri";

    private final Class<?> proxyType;
    private final String baseUriFromAnnotation;
    private final String propertyPrefix;
    private final Class<?>[] annotationProviders;
    private final RestClientConfig clientConfig;

    public RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String propertyPrefix,
            Class<?>[] annotationProviders) {
        this(proxyType, baseUriFromAnnotation, propertyPrefix, annotationProviders,
                getConfigRoot());
    }

    RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String propertyPrefix,
            Class<?>[] annotationProviders, RestClientConfigRoot configRoot) {
        this.proxyType = proxyType;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.propertyPrefix = propertyPrefix;
        this.annotationProviders = annotationProviders;
        this.clientConfig = configRoot.configs.get(propertyPrefix);
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

        Object result = builder.build(proxyType);
        return result;
    }

    void configureCustomProperties(RestClientBuilder builder) {
        if (clientConfig != null) {
            if (clientConfig.connectionPoolSize.isPresent()) {
                builder.property("resteasy.connectionPoolSize", clientConfig.connectionPoolSize.get());
            }

            if (clientConfig.connectionTTL.isPresent()) {
                builder.property("resteasy.connectionTTL",
                        Arrays.asList(clientConfig.connectionTTL.get(), TimeUnit.MILLISECONDS));
            }
        }
    }

    void configureProxy(RestClientBuilder builder) {
        Optional<String> proxyAddress = getClientConfigPropertyWithFallback(RestClientConfig::getProxyAddress,
                REST_PROXY_ADDRESS, String.class);
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
        Optional<Boolean> followRedirects = getClientConfigPropertyWithFallback(
                RestClientConfig::getFollowRedirects, REST_FOLLOW_REDIRECTS, Boolean.class);
        if (followRedirects.isPresent()) {
            builder.followRedirects(followRedirects.get());
        }
    }

    void configureQueryParamStyle(RestClientBuilder builder) {
        Optional<QueryParamStyle> queryParamStyle = getClientConfigPropertyWithFallback(RestClientConfig::getQueryParamStyle,
                REST_QUERY_PARAM_STYLE, QueryParamStyle.class);
        if (queryParamStyle.isPresent()) {
            builder.queryParamStyle(queryParamStyle.get());
        }
    }

    void configureSsl(RestClientBuilder builder) {

        Optional<Boolean> trustAll = getOptionalProperty(TLS_TRUST_ALL, Boolean.class);
        if (trustAll.isPresent() && trustAll.get()) {
            registerHostnameVerifier(REST_NOOP_HOSTNAME_VERIFIER, builder);
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[] { new PassthroughTrustManager() },
                        new SecureRandom());
                builder.sslContext(sslContext);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException("Failed to initialized SSL context", e);
            }
        }

        Optional<String> maybeTrustStore = getClientConfigPropertyWithFallback(RestClientConfig::getTrustStore,
                REST_TRUST_STORE, String.class);
        if (maybeTrustStore.isPresent()) {
            registerTrustStore(maybeTrustStore.get(), builder);
        }

        Optional<String> maybeKeyStore = getClientConfigPropertyWithFallback(RestClientConfig::getKeyStore,
                REST_KEY_STORE, String.class);
        if (maybeKeyStore.isPresent()) {
            registerKeyStore(maybeKeyStore.get(), builder);
        }

        Optional<String> maybeHostnameVerifier = getClientConfigPropertyWithFallback(RestClientConfig::getHostnameVerifier,
                REST_HOSTNAME_VERIFIER, String.class);
        if (maybeHostnameVerifier.isPresent()) {
            registerHostnameVerifier(maybeHostnameVerifier.get(), builder);
        }

        // we need to push a disabled SSL context when SSL has been disabled
        // because otherwise Apache HTTP Client will try to initialize one and will fail
        if (ImageInfo.inImageRuntimeCode() && !SslContextConfiguration.isSslNativeEnabled()) {
            builder.sslContext(new DisabledSSLContext());
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
        Optional<String> keyStorePassword = getClientConfigPropertyWithFallback(RestClientConfig::getKeyStorePassword,
                REST_KEY_STORE_PASSWORD, String.class);
        Optional<String> keyStoreType = getClientConfigPropertyWithFallback(RestClientConfig::getKeyStoreType,
                REST_KEY_STORE_TYPE, String.class);

        try {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType.orElse("JKS"));
            if (!keyStorePassword.isPresent()) {
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
        Optional<String> maybeTrustStorePassword = getClientConfigPropertyWithFallback(RestClientConfig::getTrustStorePassword,
                REST_TRUST_STORE_PASSWORD, String.class);
        Optional<String> maybeTrustStoreType = getClientConfigPropertyWithFallback(RestClientConfig::getTrustStoreType,
                REST_TRUST_STORE_TYPE, String.class);

        try {
            KeyStore trustStore = KeyStore.getInstance(maybeTrustStoreType.orElse("JKS"));
            if (!maybeTrustStorePassword.isPresent()) {
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

    void configureProviders(RestClientBuilder builder) {
        Optional<String> maybeProviders = getClientConfigPropertyWithFallback(RestClientConfig::getProviders,
                REST_PROVIDERS, String.class);
        if (maybeProviders.isPresent()) {
            registerProviders(builder, maybeProviders.get());
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
        Optional<Long> connectTimeout = getClientConfigPropertyWithFallback(RestClientConfig::getConnectTimeout,
                REST_CONNECT_TIMEOUT_FORMAT, Long.class);
        if (connectTimeout.isPresent()) {
            builder.connectTimeout(connectTimeout.get(), TimeUnit.MILLISECONDS);
        }

        Optional<Long> readTimeout = getClientConfigPropertyWithFallback(RestClientConfig::getReadTimeout,
                REST_READ_TIMEOUT_FORMAT, Long.class);
        if (readTimeout.isPresent()) {
            builder.readTimeout(readTimeout.get(), TimeUnit.MILLISECONDS);
        }
    }

    void configureBaseUrl(RestClientBuilder builder) {
        Optional<String> propertyOptional = getClientConfigPropertyWithFallback(RestClientConfig::getUri,
                REST_URI_FORMAT, String.class);
        if (!propertyOptional.isPresent()) {
            propertyOptional = getClientConfigPropertyWithFallback(RestClientConfig::getUrl, REST_URL_FORMAT, String.class);
        }
        if (((baseUriFromAnnotation == null) || baseUriFromAnnotation.isEmpty())
                && !propertyOptional.isPresent()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Unable to determine the proper baseUrl/baseUri. " +
                                    "Consider registering using @RegisterRestClient(baseUri=\"someuri\"), @RegisterRestClient(configKey=\"orkey\"), "
                                    +
                                    "or by adding '%s' or '%s' to your Quarkus configuration",
                            String.format(QUARKUS_CONFIG_REST_URL_FORMAT, propertyPrefix),
                            String.format(QUARKUS_CONFIG_REST_URI_FORMAT, propertyPrefix)));
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

    private <T> Optional<T> getOptionalDynamicProperty(String propertyFormat, Class<T> type) {
        final Config config = ConfigProvider.getConfig();
        Optional<T> interfaceNameValue = config.getOptionalValue(String.format(propertyFormat, proxyType.getName()), type);
        return interfaceNameValue.isPresent() ? interfaceNameValue
                : config.getOptionalValue(String.format(propertyFormat, propertyPrefix), type);
    }

    @SuppressWarnings("SameParameterValue")
    private <T> Optional<T> getOptionalProperty(String propertyName, Class<T> type) {
        final Config config = ConfigProvider.getConfig();
        return config.getOptionalValue(propertyName, type);
    }

    private <PropertyType> Optional<PropertyType> getClientConfigPropertyWithFallback(
            Function<RestClientConfig, Optional<PropertyType>> method, String propertyFormat, Class<PropertyType> type) {
        if (clientConfig != null) {
            Optional<PropertyType> configOptional = method.apply(clientConfig);
            if (configOptional.isPresent()) {
                return configOptional;
            }
        }
        return getOptionalDynamicProperty(propertyFormat, type);
    }

    private static RestClientConfigRoot getConfigRoot() {
        InstanceHandle<RestClientConfigRootProvider> configHandle = Arc.container()
                .instance(RestClientConfigRootProvider.class);
        if (!configHandle.isAvailable()) {
            throw new IllegalStateException("Unable to find the RestClientConfigRootProvider");
        }
        return configHandle.get().getConfigRoot();
    }
}
