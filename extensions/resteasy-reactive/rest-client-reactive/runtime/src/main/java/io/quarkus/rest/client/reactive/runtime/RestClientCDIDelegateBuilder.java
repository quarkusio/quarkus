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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;

import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;

public class RestClientCDIDelegateBuilder<T> {

    private static final String MP_REST = "mp-rest";
    private static final String REST_FOLLOW_REDIRECTS = "%s/" + MP_REST + "/followRedirects";
    private static final String REST_HOSTNAME_VERIFIER = "%s/" + MP_REST + "/hostnameVerifier";
    private static final String REST_KEY_STORE = "%s/" + MP_REST + "/keyStore";
    private static final String REST_KEY_STORE_PASSWORD = "%s/" + MP_REST + "/keyStorePassword";
    private static final String REST_KEY_STORE_TYPE = "%s/" + MP_REST + "/keyStoreType";
    private static final String REST_PROVIDERS = "%s/" + MP_REST + "/providers";
    private static final String REST_PROXY_ADDRESS = "%s/" + MP_REST + "/proxyAddress";
    private static final String REST_QUERY_PARAM_STYLE = "%s/" + MP_REST + "/queryParamStyle";
    public static final String REST_SCOPE_FORMAT = "%s/" + MP_REST + "/scope";
    private static final String REST_TIMEOUT_CONNECT = "%s/" + MP_REST + "/connectTimeout";
    private static final String REST_TIMEOUT_READ = "%s/" + MP_REST + "/readTimeout";
    private static final String REST_TRUST_STORE = "%s/" + MP_REST + "/trustStore";
    private static final String REST_TRUST_STORE_PASSWORD = "%s/" + MP_REST + "/trustStorePassword";
    private static final String REST_TRUST_STORE_TYPE = "%s/" + MP_REST + "/trustStoreType";
    private static final String REST_URL_FORMAT = "%s/" + MP_REST + "/url";
    private static final String REST_URI_FORMAT = "%s/" + MP_REST + "/uri";

    private static final String MAX_REDIRECTS = "quarkus.rest.client.max-redirects";
    private static final String MULTIPART_POST_ENCODER_MODE = "quarkus.rest.client.multipart-post-encoder-mode";

    private final Class<T> jaxrsInterface;
    private final String baseUriFromAnnotation;
    private final String propertyPrefix;

    public static <T> T createDelegate(Class<T> jaxrsInterface, String baseUriFromAnnotation, String propertyPrefix) {
        return new RestClientCDIDelegateBuilder<T>(jaxrsInterface, baseUriFromAnnotation, propertyPrefix).build();
    }

    private RestClientCDIDelegateBuilder(Class<T> jaxrsInterface, String baseUriFromAnnotation, String propertyPrefix) {
        this.jaxrsInterface = jaxrsInterface;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.propertyPrefix = propertyPrefix;
    }

    private T build() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
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
        Optional<String> encoder = getOptionalProperty(MULTIPART_POST_ENCODER_MODE, String.class);
        if (encoder.isPresent()) {
            HttpPostRequestEncoder.EncoderMode mode = HttpPostRequestEncoder.EncoderMode
                    .valueOf(encoder.get().toUpperCase(Locale.ROOT));
            builder.property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE, mode);
        }
    }

    private void configureProxy(RestClientBuilder builder) {
        Optional<String> maybeProxy = getOptionalDynamicProperty(REST_PROXY_ADDRESS, String.class);
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
        Optional<QueryParamStyle> maybeQueryParamStyle = getOptionalDynamicProperty(REST_QUERY_PARAM_STYLE,
                QueryParamStyle.class);
        if (maybeQueryParamStyle.isPresent()) {
            QueryParamStyle queryParamStyle = maybeQueryParamStyle.get();
            builder.queryParamStyle(queryParamStyle);
        }
    }

    private void configureRedirects(RestClientBuilder builder) {
        Optional<Integer> maxRedirects = getOptionalProperty(MAX_REDIRECTS, Integer.class);
        if (maxRedirects.isPresent()) {
            builder.property(QuarkusRestClientProperties.MAX_REDIRECTS, maxRedirects.get());
        }

        Optional<Boolean> maybeFollowRedirects = getOptionalDynamicProperty(REST_FOLLOW_REDIRECTS, Boolean.class);
        if (maybeFollowRedirects.isPresent()) {
            builder.followRedirects(maybeFollowRedirects.get());
        }
    }

    private void configureSsl(RestClientBuilder builder) {

        Optional<String> maybeTrustStore = getOptionalDynamicProperty(REST_TRUST_STORE, String.class);
        if (maybeTrustStore.isPresent()) {
            registerTrustStore(maybeTrustStore.get(), builder);
        }

        Optional<String> maybeKeyStore = getOptionalDynamicProperty(REST_KEY_STORE, String.class);
        if (maybeKeyStore.isPresent()) {
            registerKeyStore(maybeKeyStore.get(), builder);
        }

        Optional<String> maybeHostnameVerifier = getOptionalDynamicProperty(REST_HOSTNAME_VERIFIER, String.class);
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
        Optional<String> keyStorePassword = getOptionalDynamicProperty(REST_KEY_STORE_PASSWORD, String.class);
        Optional<String> keyStoreType = getOptionalDynamicProperty(REST_KEY_STORE_TYPE, String.class);

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
        Optional<String> maybeTrustStorePassword = getOptionalDynamicProperty(REST_TRUST_STORE_PASSWORD, String.class);
        Optional<String> maybeTrustStoreType = getOptionalDynamicProperty(REST_TRUST_STORE_TYPE, String.class);

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
        Optional<String> maybeProviders = getOptionalDynamicProperty(REST_PROVIDERS, String.class);
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
        Optional<Long> connectTimeout = getOptionalDynamicProperty(REST_TIMEOUT_CONNECT, Long.class);
        if (connectTimeout.isPresent()) {
            builder.connectTimeout(connectTimeout.get(), TimeUnit.MILLISECONDS);
        }

        Optional<Long> readTimeout = getOptionalDynamicProperty(REST_TIMEOUT_READ, Long.class);
        if (readTimeout.isPresent()) {
            builder.readTimeout(readTimeout.get(), TimeUnit.MILLISECONDS);
        }
    }

    private void configureBaseUrl(RestClientBuilder builder) {
        Optional<String> propertyOptional = getOptionalDynamicProperty(REST_URI_FORMAT, String.class);
        if (!propertyOptional.isPresent()) {
            propertyOptional = getOptionalDynamicProperty(REST_URL_FORMAT, String.class);
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

    private <PropertyType> Optional<PropertyType> getOptionalDynamicProperty(String propertyFormat, Class<PropertyType> type) {
        final Config config = ConfigProvider.getConfig();
        Optional<PropertyType> interfaceNameValue = config
                .getOptionalValue(String.format(propertyFormat, jaxrsInterface.getName()), type);
        return interfaceNameValue.isPresent() ? interfaceNameValue
                : config.getOptionalValue(String.format(propertyFormat, propertyPrefix), type);
    }

    private <PropertyType> Optional<PropertyType> getOptionalProperty(String propertyName, Class<PropertyType> type) {
        final Config config = ConfigProvider.getConfig();
        return config.getOptionalValue(propertyName, type);
    }

}
