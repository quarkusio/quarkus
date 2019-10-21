package io.quarkus.restclient.runtime;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

public class RestClientBase {

    public static final String MP_REST = "mp-rest";
    public static final String REST_URL_FORMAT = "%s/" + MP_REST + "/url";
    public static final String REST_URI_FORMAT = "%s/" + MP_REST + "/uri";
    public static final String REST_CONNECT_TIMEOUT_FORMAT = "%s/" + MP_REST + "/connectTimeout";
    public static final String REST_READ_TIMEOUT_FORMAT = "%s/" + MP_REST + "/readTimeout";

    private final Class<?> proxyType;
    private final String baseUriFromAnnotation;
    private final String propertyPrefixFromAnnotation;

    private final Config config;

    private final String prefix; // calculated based on propertyPrefixFromAnnotation and proxyType

    public RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String propertyPrefixFromAnnotation) {
        this.proxyType = proxyType;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.propertyPrefixFromAnnotation = propertyPrefixFromAnnotation;
        this.config = ConfigProvider.getConfig();
        this.prefix = getPrefix();
    }

    public Object create() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        configureBaseUrl(builder);
        configureTimeouts(builder);

        return builder.build(proxyType);
    }

    private void configureBaseUrl(RestClientBuilder builder) {
        Optional<String> propertyOptional = getOptionalProperty(String.class, REST_URI_FORMAT, REST_URL_FORMAT);
        String baseUrl = propertyOptional.orElse(baseUriFromAnnotation);

        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Unable to determine the proper baseUrl/baseUri. Consider registering using @RegisterRestClient(baseUri=\"someuri\", configKey=\"orkey\"), or by adding '%s' to your Quarkus configuration",
                            prefix));
        }

        try {
            builder.baseUrl(new URL(baseUrl)).build(proxyType);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The value of URL was invalid: " + baseUrl, e);
        } catch (Exception e) {
            if ("com.oracle.svm.core.jdk.UnsupportedFeatureError".equals(e.getClass().getCanonicalName())) {
                throw new IllegalArgumentException(baseUrl
                        + " requires SSL support but it is disabled. You probably have set quarkus.ssl.native to false.");
            }
            throw e;
        }
    }

    private void configureTimeouts(RestClientBuilder builder) {
        getOptionalProperty(Long.class, REST_CONNECT_TIMEOUT_FORMAT)
                .ifPresent(timeout -> builder.connectTimeout(timeout, TimeUnit.MILLISECONDS));

        getOptionalProperty(Long.class, REST_READ_TIMEOUT_FORMAT)
                .ifPresent(timeout -> builder.readTimeout(timeout, TimeUnit.MILLISECONDS));
    }

    private <T> Optional<T> getOptionalProperty(Class<T> clazz, String... propertiesFormat) {
        Optional<T> value = Optional.empty();
        for (int i = 0; i < propertiesFormat.length && !value.isPresent(); i++) {
            value = config.getOptionalValue(String.format(propertiesFormat[i], prefix), clazz);
        }
        return value;
    }

    private String getPrefix() {
        return Optional.ofNullable(propertyPrefixFromAnnotation)
                .filter(p -> !p.isEmpty())
                .orElse(proxyType.getName());
    }
}
