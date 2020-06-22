package io.quarkus.restclient.runtime;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.graalvm.nativeimage.ImageInfo;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
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

    private final Class<?> proxyType;
    private final String baseUriFromAnnotation;
    private final String propertyPrefix;

    public RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String propertyPrefix) {
        this.proxyType = proxyType;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.propertyPrefix = propertyPrefix;
    }

    public Object create() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        configureBaseUrl(builder);
        configureTimeouts(builder);
        configureProviders(builder);
        configureSsl(builder);
        // If we have context propagation, then propagate context to the async client threads
        InstanceHandle<ManagedExecutor> managedExecutor = Arc.container().instance(ManagedExecutor.class);
        if (managedExecutor.isAvailable()) {
            builder.executorService(managedExecutor.get());
        }

        Object result = builder.build(proxyType);
        return result;
    }

    private void configureSsl(RestClientBuilder builder) {
        Optional<String> maybeTrustStore = getOptionalProperty(REST_TRUST_STORE, String.class);
        if (maybeTrustStore.isPresent()) {
            registerTrustStore(maybeTrustStore.get(), builder);
        }

        Optional<String> maybeKeyStore = getOptionalProperty(REST_KEY_STORE, String.class);
        if (maybeKeyStore.isPresent()) {
            registerKeyStore(maybeKeyStore.get(), builder);
        }

        Optional<String> maybeHostnameVerifier = getOptionalProperty(REST_HOSTNAME_VERIFIER, String.class);
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
            Class<?> verifierClass = Class.forName(verifier, true, Thread.currentThread().getContextClassLoader());
            builder.hostnameVerifier((HostnameVerifier) verifierClass.newInstance());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find hostname verifier class" + verifier, e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Failed to instantiate hostname verifier class. Make sure it has a public, no-argument constructor", e);
        } catch (ClassCastException e) {
            throw new RuntimeException("The provided hostname verifier " + verifier + " is not an instance of HostnameVerifier",
                    e);
        }
    }

    private void registerKeyStore(String keyStorePath, RestClientBuilder builder) {
        Optional<String> keyStorePassword = getOptionalProperty(REST_KEY_STORE_PASSWORD, String.class);
        Optional<String> keyStoreType = getOptionalProperty(REST_KEY_STORE_TYPE, String.class);

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
        Optional<String> maybeTrustStorePassword = getOptionalProperty(REST_TRUST_STORE_PASSWORD, String.class);
        Optional<String> maybeTrustStoreType = getOptionalProperty(REST_TRUST_STORE_TYPE, String.class);

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

    private void configureProviders(RestClientBuilder builder) {
        Optional<String> maybeProviders = getOptionalProperty(REST_PROVIDERS, String.class);
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
        Optional<Long> connectTimeout = getOptionalProperty(REST_CONNECT_TIMEOUT_FORMAT, Long.class);
        if (connectTimeout.isPresent()) {
            builder.connectTimeout(connectTimeout.get(), TimeUnit.MILLISECONDS);
        }

        Optional<Long> readTimeout = getOptionalProperty(REST_READ_TIMEOUT_FORMAT, Long.class);
        if (readTimeout.isPresent()) {
            builder.readTimeout(readTimeout.get(), TimeUnit.MILLISECONDS);
        }
    }

    private void configureBaseUrl(RestClientBuilder builder) {
        Optional<String> propertyOptional = getOptionalProperty(REST_URI_FORMAT, String.class);
        if (!propertyOptional.isPresent()) {
            propertyOptional = getOptionalProperty(REST_URL_FORMAT, String.class);
        }
        if (((baseUriFromAnnotation == null) || baseUriFromAnnotation.isEmpty())
                && !propertyOptional.isPresent()) {
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

    private <T> Optional<T> getOptionalProperty(String propertyFormat, Class<T> type) {
        final Config config = ConfigProvider.getConfig();
        Optional<T> interfaceNameValue = config.getOptionalValue(String.format(propertyFormat, proxyType.getName()), type);
        return interfaceNameValue.isPresent() ? interfaceNameValue
                : config.getOptionalValue(String.format(propertyFormat, propertyPrefix), type);
    }
}
