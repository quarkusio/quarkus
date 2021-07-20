package io.quarkus.restclient.config;

import java.util.Optional;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RestClientConfig {

    /**
     * The base URL to use for this service. This property or the `uri` property is considered required, unless
     * the `baseUri` attribute is configured in the `@RegisterRestClient` annotation.
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * The base URI to use for this service. This property or the `url` property is considered required, unless
     * the `baseUri` attribute is configured in the `@RegisterRestClient` annotation.
     */
    @ConfigItem
    public Optional<String> uri;

    /**
     * The CDI scope to use for injection. This property can contain either a fully qualified class name of a CDI scope
     * annotation (such as "javax.enterprise.context.ApplicationScoped") or its' simple name (such as
     * "ApplicationScoped").
     */
    @ConfigItem(defaultValue = "javax.enterprise.context.ApplicationScoped")
    public Optional<String> scope;

    /**
     * Map where keys are fully-qualified provider classnames to include in the client, and values are their integer
     * priorities. The equivalent of the `@RegisterProvider` annotation.
     */
    @ConfigItem
    public Optional<String> providers;

    /**
     * Timeout specified in milliseconds to wait to connect to the remote endpoint.
     */
    @ConfigItem
    public Optional<Long> connectTimeout;

    /**
     * Timeout specified in milliseconds to wait for a response from the remote endpoint.
     */
    @ConfigItem
    public Optional<Long> readTimeout;

    /**
     * A boolean value used to determine whether the client should follow HTTP redirect responses.
     */
    @ConfigItem
    public Optional<Boolean> followRedirects;

    /**
     * A string value in the form of `<proxyHost>:<proxyPort>` that specifies the HTTP proxy server hostname
     * (or IP address) and port for requests of this client to use.
     */
    @ConfigItem
    public Optional<String> proxyAddress;

    /**
     * An enumerated type string value with possible values of "MULTI_PAIRS" (default), "COMMA_SEPARATED",
     * or "ARRAY_PAIRS" that specifies the format in which multiple values for the same query parameter is used.
     */
    @ConfigItem
    public Optional<QueryParamStyle> queryParamStyle;

    /**
     * The trust store location. Can point to either a classpath resource or a file.
     */
    @ConfigItem
    public Optional<String> trustStore;

    /**
     * The trust store password.
     */
    @ConfigItem
    public Optional<String> trustStorePassword;

    /**
     * The type of the trust store. Defaults to "JKS".
     */
    @ConfigItem
    public Optional<String> trustStoreType;

    /**
     * The key store location. Can point to either a classpath resource or a file.
     */
    @ConfigItem
    public Optional<String> keyStore;

    /**
     * The key store password.
     */
    @ConfigItem
    public Optional<String> keyStorePassword;

    /**
     * The type of the key store. Defaults to "JKS".
     */
    @ConfigItem
    public Optional<String> keyStoreType;

    /**
     * The class name of the host name verifier. The class must have a public no-argument constructor.
     */
    @ConfigItem
    public Optional<String> hostnameVerifier;

    /**
     * The time in ms for which a connection remains unused in the connection pool before being evicted and closed.
     * A timeout of {@code 0} means there is no timeout.
     */
    @ConfigItem
    public Optional<Integer> connectionTTL;

    /**
     * The size of the connection pool for this client.
     */
    @ConfigItem
    public Optional<Integer> connectionPoolSize;

    /**
     * The maximum number of redirection a request can follow.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<Integer> maxRedirects;

    public Optional<String> getUrl() {
        return url;
    }

    public Optional<String> getUri() {
        return uri;
    }

    public Optional<String> getScope() {
        return scope;
    }

    public Optional<String> getProviders() {
        return providers;
    }

    public Optional<Long> getConnectTimeout() {
        return connectTimeout;
    }

    public Optional<Long> getReadTimeout() {
        return readTimeout;
    }

    public Optional<Boolean> getFollowRedirects() {
        return followRedirects;
    }

    public Optional<String> getProxyAddress() {
        return proxyAddress;
    }

    public Optional<QueryParamStyle> getQueryParamStyle() {
        return queryParamStyle;
    }

    public Optional<String> getTrustStore() {
        return trustStore;
    }

    public Optional<String> getTrustStorePassword() {
        return trustStorePassword;
    }

    public Optional<String> getTrustStoreType() {
        return trustStoreType;
    }

    public Optional<String> getKeyStore() {
        return keyStore;
    }

    public Optional<String> getKeyStorePassword() {
        return keyStorePassword;
    }

    public Optional<String> getKeyStoreType() {
        return keyStoreType;
    }

    public Optional<String> getHostnameVerifier() {
        return hostnameVerifier;
    }

    public Optional<Integer> getConnectionTTL() {
        return connectionTTL;
    }

    public Optional<Integer> getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public Optional<Integer> getMaxRedirects() {
        return maxRedirects;
    }
}
