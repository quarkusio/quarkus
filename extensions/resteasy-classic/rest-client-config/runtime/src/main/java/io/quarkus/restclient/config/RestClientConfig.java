package io.quarkus.restclient.config;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.SmallRyeConfig;

@ConfigGroup
public class RestClientConfig {

    public static final RestClientConfig EMPTY;

    static {
        EMPTY = new RestClientConfig();
        EMPTY.url = Optional.empty();
        EMPTY.uri = Optional.empty();
        EMPTY.providers = Optional.empty();
        EMPTY.connectTimeout = Optional.empty();
        EMPTY.readTimeout = Optional.empty();
        EMPTY.followRedirects = Optional.empty();
        EMPTY.proxyAddress = Optional.empty();
        EMPTY.proxyUser = Optional.empty();
        EMPTY.proxyPassword = Optional.empty();
        EMPTY.nonProxyHosts = Optional.empty();
        EMPTY.queryParamStyle = Optional.empty();
        EMPTY.verifyHost = Optional.empty();
        EMPTY.trustStore = Optional.empty();
        EMPTY.trustStorePassword = Optional.empty();
        EMPTY.trustStoreType = Optional.empty();
        EMPTY.keyStore = Optional.empty();
        EMPTY.keyStorePassword = Optional.empty();
        EMPTY.keyStoreType = Optional.empty();
        EMPTY.hostnameVerifier = Optional.empty();
        EMPTY.connectionTTL = Optional.empty();
        EMPTY.connectionPoolSize = Optional.empty();
        EMPTY.keepAliveEnabled = Optional.empty();
        EMPTY.maxRedirects = Optional.empty();
        EMPTY.multipart = new RestClientMultipartConfig();
        EMPTY.multipart.maxChunkSize = Optional.empty();
        EMPTY.headers = Collections.emptyMap();
        EMPTY.shared = Optional.empty();
        EMPTY.name = Optional.empty();
        EMPTY.userAgent = Optional.empty();
        EMPTY.http2 = Optional.empty();
        EMPTY.maxChunkSize = Optional.empty();
        EMPTY.alpn = Optional.empty();
        EMPTY.captureStacktrace = Optional.empty();
    }

    public RestClientMultipartConfig multipart;

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
     *
     * Use `none` to disable proxy
     */
    @ConfigItem
    public Optional<String> proxyAddress;

    /**
     * Proxy username.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyUser;

    /**
     * Proxy password.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyPassword;

    /**
     * Hosts to access without proxy
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> nonProxyHosts;

    /**
     * An enumerated type string value with possible values of "MULTI_PAIRS" (default), "COMMA_SEPARATED",
     * or "ARRAY_PAIRS" that specifies the format in which multiple values for the same query parameter is used.
     */
    @ConfigItem
    public Optional<QueryParamStyle> queryParamStyle;

    /**
     * Set whether hostname verification is enabled. Default is enabled.
     * This setting should not be disabled in production as it makes the client vulnerable to MITM attacks.
     */
    @ConfigItem
    public Optional<Boolean> verifyHost;

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
     * If set to false disables the keep alive completely.
     */
    @ConfigItem
    public Optional<Boolean> keepAliveEnabled;

    /**
     * The maximum number of redirection a request can follow.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<Integer> maxRedirects;

    /**
     * The HTTP headers that should be applied to all requests of the rest client.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Map<String, String> headers;

    /**
     * Set to true to share the HTTP client between REST clients.
     * There can be multiple shared clients distinguished by <em>name</em>, when no specific name is set,
     * the name <code>__vertx.DEFAULT</code> is used.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<Boolean> shared;

    /**
     * Set the HTTP client name, used when the client is shared, otherwise ignored.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> name;

    /**
     * Configure the HTTP user-agent header to use.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> userAgent;

    /**
     * If this is true then HTTP/2 will be enabled.
     */
    @ConfigItem
    public Optional<Boolean> http2;

    /**
     * The max HTTP chunk size (8096 bytes by default).
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    @ConfigDocDefault("8K")
    public Optional<MemorySize> maxChunkSize;

    /**
     * If the Application-Layer Protocol Negotiation is enabled, the client will negotiate which protocol to use over the
     * protocols exposed by the server. By default, it will try to use HTTP/2 first and if it's not enabled, it will
     * use HTTP/1.1.
     * When the property `http2` is enabled, this flag will be automatically enabled.
     */
    @ConfigItem
    public Optional<Boolean> alpn;

    /**
     * If {@code true}, the stacktrace of the invocation of the REST Client method is captured.
     * This stacktrace will be used if the invocation throws an exception
     */
    @ConfigItem
    public Optional<Boolean> captureStacktrace;

    public static RestClientConfig load(String configKey) {
        final RestClientConfig instance = new RestClientConfig();

        instance.url = getConfigValue(configKey, "url", String.class);
        instance.uri = getConfigValue(configKey, "uri", String.class);
        instance.providers = getConfigValue(configKey, "providers", String.class);
        instance.connectTimeout = getConfigValue(configKey, "connect-timeout", Long.class);
        instance.readTimeout = getConfigValue(configKey, "read-timeout", Long.class);
        instance.followRedirects = getConfigValue(configKey, "follow-redirects", Boolean.class);
        instance.proxyAddress = getConfigValue(configKey, "proxy-address", String.class);
        instance.proxyUser = getConfigValue(configKey, "proxy-user", String.class);
        instance.proxyPassword = getConfigValue(configKey, "proxy-password", String.class);
        instance.nonProxyHosts = getConfigValue(configKey, "non-proxy-hosts", String.class);
        instance.queryParamStyle = getConfigValue(configKey, "query-param-style", QueryParamStyle.class);
        instance.verifyHost = getConfigValue(configKey, "verify-host", Boolean.class);
        instance.trustStore = getConfigValue(configKey, "trust-store", String.class);
        instance.trustStorePassword = getConfigValue(configKey, "trust-store-password", String.class);
        instance.trustStoreType = getConfigValue(configKey, "trust-store-type", String.class);
        instance.keyStore = getConfigValue(configKey, "key-store", String.class);
        instance.keyStorePassword = getConfigValue(configKey, "key-store-password", String.class);
        instance.keyStoreType = getConfigValue(configKey, "key-store-type", String.class);
        instance.hostnameVerifier = getConfigValue(configKey, "hostname-verifier", String.class);
        instance.connectionTTL = getConfigValue(configKey, "connection-ttl", Integer.class);
        instance.connectionPoolSize = getConfigValue(configKey, "connection-pool-size", Integer.class);
        instance.keepAliveEnabled = getConfigValue(configKey, "keep-alive-enabled", Boolean.class);
        instance.maxRedirects = getConfigValue(configKey, "max-redirects", Integer.class);
        instance.headers = getConfigValues(configKey, "headers", String.class, String.class);
        instance.shared = getConfigValue(configKey, "shared", Boolean.class);
        instance.name = getConfigValue(configKey, "name", String.class);
        instance.userAgent = getConfigValue(configKey, "user-agent", String.class);
        instance.http2 = getConfigValue(configKey, "http2", Boolean.class);
        instance.maxChunkSize = getConfigValue(configKey, "max-chunk-size", MemorySize.class);
        instance.alpn = getConfigValue(configKey, "alpn", Boolean.class);
        instance.captureStacktrace = getConfigValue(configKey, "capture-stacktrace", Boolean.class);

        instance.multipart = new RestClientMultipartConfig();
        instance.multipart.maxChunkSize = getConfigValue(configKey, "multipart.max-chunk-size", Integer.class);

        return instance;
    }

    public static RestClientConfig load(Class<?> interfaceClass) {
        final RestClientConfig instance = new RestClientConfig();

        instance.url = getConfigValue(interfaceClass, "url", String.class);
        instance.uri = getConfigValue(interfaceClass, "uri", String.class);
        instance.providers = getConfigValue(interfaceClass, "providers", String.class);
        instance.connectTimeout = getConfigValue(interfaceClass, "connect-timeout", Long.class);
        instance.readTimeout = getConfigValue(interfaceClass, "read-timeout", Long.class);
        instance.followRedirects = getConfigValue(interfaceClass, "follow-redirects", Boolean.class);
        instance.proxyAddress = getConfigValue(interfaceClass, "proxy-address", String.class);
        instance.proxyUser = getConfigValue(interfaceClass, "proxy-user", String.class);
        instance.proxyPassword = getConfigValue(interfaceClass, "proxy-password", String.class);
        instance.nonProxyHosts = getConfigValue(interfaceClass, "non-proxy-hosts", String.class);
        instance.queryParamStyle = getConfigValue(interfaceClass, "query-param-style", QueryParamStyle.class);
        instance.verifyHost = getConfigValue(interfaceClass, "verify-host", Boolean.class);
        instance.trustStore = getConfigValue(interfaceClass, "trust-store", String.class);
        instance.trustStorePassword = getConfigValue(interfaceClass, "trust-store-password", String.class);
        instance.trustStoreType = getConfigValue(interfaceClass, "trust-store-type", String.class);
        instance.keyStore = getConfigValue(interfaceClass, "key-store", String.class);
        instance.keyStorePassword = getConfigValue(interfaceClass, "key-store-password", String.class);
        instance.keyStoreType = getConfigValue(interfaceClass, "key-store-type", String.class);
        instance.hostnameVerifier = getConfigValue(interfaceClass, "hostname-verifier", String.class);
        instance.connectionTTL = getConfigValue(interfaceClass, "connection-ttl", Integer.class);
        instance.connectionPoolSize = getConfigValue(interfaceClass, "connection-pool-size", Integer.class);
        instance.keepAliveEnabled = getConfigValue(interfaceClass, "keep-alive-enabled", Boolean.class);
        instance.maxRedirects = getConfigValue(interfaceClass, "max-redirects", Integer.class);
        instance.headers = getConfigValues(interfaceClass, "headers", String.class, String.class);
        instance.shared = getConfigValue(interfaceClass, "shared", Boolean.class);
        instance.name = getConfigValue(interfaceClass, "name", String.class);
        instance.userAgent = getConfigValue(interfaceClass, "user-agent", String.class);
        instance.http2 = getConfigValue(interfaceClass, "http2", Boolean.class);
        instance.maxChunkSize = getConfigValue(interfaceClass, "max-chunk-size", MemorySize.class);
        instance.alpn = getConfigValue(interfaceClass, "alpn", Boolean.class);
        instance.captureStacktrace = getConfigValue(interfaceClass, "capture-stacktrace", Boolean.class);

        instance.multipart = new RestClientMultipartConfig();
        instance.multipart.maxChunkSize = getConfigValue(interfaceClass, "multipart.max-chunk-size", Integer.class);

        return instance;
    }

    private static <T> Optional<T> getConfigValue(String configKey, String fieldName, Class<T> type) {
        final Config config = ConfigProvider.getConfig();
        Optional<T> optional = config.getOptionalValue(composePropertyKey(configKey, fieldName), type);
        if (optional.isEmpty()) { // try to find property with quoted configKey
            optional = config.getOptionalValue(composePropertyKey('"' + configKey + '"', fieldName), type);
        }
        return optional;
    }

    private static <T> Optional<T> getConfigValue(Class<?> clientInterface, String fieldName, Class<T> type) {
        final Config config = ConfigProvider.getConfig();
        // first try interface full name
        Optional<T> optional = config.getOptionalValue(composePropertyKey('"' + clientInterface.getName() + '"', fieldName),
                type);
        if (optional.isEmpty()) { // then interface simple name
            optional = config.getOptionalValue(composePropertyKey(clientInterface.getSimpleName(), fieldName), type);
        }
        if (optional.isEmpty()) { // lastly quoted interface simple name
            optional = config.getOptionalValue(composePropertyKey('"' + clientInterface.getSimpleName() + '"', fieldName),
                    type);
        }
        return optional;
    }

    private static <K, V> Map<K, V> getConfigValues(String configKey, String fieldName, Class<K> keyType, Class<V> valueType) {
        final SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        Optional<Map<K, V>> optional = config.getOptionalValues(composePropertyKey(configKey, fieldName), keyType, valueType);
        if (optional.isEmpty()) { // try to find property with quoted configKey
            optional = config.getOptionalValues(composePropertyKey('"' + configKey + '"', fieldName), keyType, valueType);
        }
        return optional.isPresent() ? optional.get() : Collections.emptyMap();
    }

    private static <K, V> Map<K, V> getConfigValues(Class<?> clientInterface, String fieldName, Class<K> keyType,
            Class<V> valueType) {
        final SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        // first try interface full name
        Optional<Map<K, V>> optional = config.getOptionalValues(
                composePropertyKey('"' + clientInterface.getName() + '"', fieldName),
                keyType, valueType);
        if (optional.isEmpty()) { // then interface simple name
            optional = config.getOptionalValues(composePropertyKey(clientInterface.getSimpleName(), fieldName), keyType,
                    valueType);
        }
        if (optional.isEmpty()) { // lastly quoted interface simple name
            optional = config.getOptionalValues(composePropertyKey('"' + clientInterface.getSimpleName() + '"', fieldName),
                    keyType, valueType);
        }
        return optional.isPresent() ? optional.get() : Collections.emptyMap();
    }

    private static String composePropertyKey(String key, String fieldName) {
        return Constants.QUARKUS_CONFIG_PREFIX + key + "." + fieldName;
    }
}
