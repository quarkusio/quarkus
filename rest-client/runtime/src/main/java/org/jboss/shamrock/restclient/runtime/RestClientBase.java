package org.jboss.shamrock.restclient.runtime;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

public class RestClientBase {

    public static final String REST_URL_FORMAT = "%s/mp-rest/url";

    private final Class<?> proxyType;

    private final Config config;

    public RestClientBase(Class<?> proxyType) {
        this.proxyType = proxyType;
        this.config = ConfigProvider.getConfig();
    }

    public Object create() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        String baseUrl = getBaseUrl();
        try {
            return builder.baseUrl(new URL(baseUrl)).build(proxyType);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The value of URL was invalid " + baseUrl);
        }
    }

    private String getBaseUrl() {
        String property = String.format(REST_URL_FORMAT, proxyType.getName());
        return config.getValue(property, String.class);
    }
}
