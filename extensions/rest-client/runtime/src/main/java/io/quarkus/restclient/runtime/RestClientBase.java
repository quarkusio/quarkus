package io.quarkus.restclient.runtime;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

public class RestClientBase {

    public static final String MP_REST = "mp-rest";
    public static final String REST_URL_FORMAT = "%s/" + MP_REST + "/url";
    public static final String REST_URI_FORMAT = "%s/" + MP_REST + "/uri";

    private final Class<?> proxyType;
    private final String baseUriFromAnnotation;
    private final String propertyPrefixFromAnnotation;

    private final Config config;

    public RestClientBase(Class<?> proxyType, String baseUriFromAnnotation, String propertyPrefixFromAnnotation) {
        this.proxyType = proxyType;
        this.baseUriFromAnnotation = baseUriFromAnnotation;
        this.propertyPrefixFromAnnotation = propertyPrefixFromAnnotation;
        this.config = ConfigProvider.getConfig();
    }

    public Object create() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        String baseUrl = getBaseUrl();
        try {
            return builder.baseUrl(new URL(baseUrl)).build(proxyType);
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

    private String getBaseUrl() {
        String prefix = proxyType.getName();
        if (propertyPrefixFromAnnotation != null && !propertyPrefixFromAnnotation.isEmpty()) {
            prefix = propertyPrefixFromAnnotation;
        }
        String propertyName = String.format(REST_URI_FORMAT, prefix);
        Optional<String> propertyOptional = config.getOptionalValue(propertyName, String.class);
        if (!propertyOptional.isPresent()) {
            propertyName = String.format(REST_URL_FORMAT, prefix);
            propertyOptional = config.getOptionalValue(propertyName, String.class);
        }
        if (((baseUriFromAnnotation == null) || baseUriFromAnnotation.isEmpty())
                && !propertyOptional.isPresent()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Unable to determine the proper baseUrl/baseUri. Consider registering using @RegisterRestClient(baseUri=\"someuri\", configKey=\"orkey\"), or by adding '%s' to your Quarkus configuration",
                            propertyName));
        }
        return propertyOptional.orElse(baseUriFromAnnotation);
    }
}
