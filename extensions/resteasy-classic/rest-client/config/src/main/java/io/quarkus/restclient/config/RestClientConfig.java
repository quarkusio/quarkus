package io.quarkus.restclient.config;

import java.util.Optional;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RestClientConfig {

    public static final RestClientConfig EMPTY;

    static {
        EMPTY = new RestClientConfig();
        EMPTY.url = Optional.empty();
        EMPTY.uri = Optional.empty();
        EMPTY.scope = Optional.empty();
        EMPTY.providers = Optional.empty();
        EMPTY.connectTimeout = Optional.empty();
        EMPTY.readTimeout = Optional.empty();
        EMPTY.followRedirects = Optional.empty();
        EMPTY.proxyAddress = Optional.empty();
        EMPTY.queryParamStyle = Optional.empty();
        EMPTY.trustStore = Optional.empty();
        EMPTY.trustStorePassword = Optional.empty();
        EMPTY.trustStoreType = Optional.empty();
        EMPTY.keyStore = Optional.empty();
        EMPTY.keyStorePassword = Optional.empty();
        EMPTY.keyStoreType = Optional.empty();
        EMPTY.hostnameVerifier = Optional.empty();
        EMPTY.connectionTTL = Optional.empty();
        EMPTY.connectionPoolSize = Optional.empty();
        EMPTY.maxRedirects = Optional.empty();
    }

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
     * annotation (such as "javax.enterprise.context.ApplicationScoped") or its simple name (such as
     * "ApplicationScoped").
     */
    @ConfigItem
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

}
