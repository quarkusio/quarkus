package io.quarkus.restclient.config;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
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
        EMPTY.overrideUri = Optional.empty();
        EMPTY.providers = Optional.empty();
        EMPTY.connectTimeout = Optional.empty();
        EMPTY.readTimeout = Optional.empty();
        EMPTY.followRedirects = Optional.empty();
        EMPTY.multipartPostEncoderMode = Optional.empty();
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
        EMPTY.tlsConfigurationName = Optional.empty();
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
     * This property is only meant to be set by advanced configurations to override whatever value was set for the uri or url.
     * The override is done using the REST Client class name configuration syntax.
     * <p>
     * This property is not applicable to the RESTEasy Client, only the Quarkus Rest client (formerly RESTEasy Reactive client).
     */
    @ConfigItem
    public Optional<String> overrideUri;

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
     * Mode in which the form data are encoded. Possible values are `HTML5`, `RFC1738` and `RFC3986`.
     * The modes are described in the
     * <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/multipart/HttpPostRequestEncoder.EncoderMode.html">Netty
     * documentation</a>
     * <p>
     * By default, Rest Client Reactive uses RFC1738.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @ConfigItem
    public Optional<String> multipartPostEncoderMode;

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
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @ConfigItem
    public Optional<String> proxyUser;

    /**
     * Proxy password.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @ConfigItem
    public Optional<String> proxyPassword;

    /**
     * Hosts to access without proxy
     * <p>
     * This property is not applicable to the RESTEasy Client.
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
     * The name of the TLS configuration to use.
     * <p>
     * If not set and the default TLS configuration is configured ({@code quarkus.tls.*}) then that will be used.
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * If no TLS configuration is set, then the keys-tore, trust-store, etc. properties will be used.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @ConfigItem
    public Optional<String> tlsConfigurationName;

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
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @ConfigItem
    public Optional<Integer> maxRedirects;

    /**
     * The HTTP headers that should be applied to all requests of the rest client.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @ConfigItem
    @ConfigDocMapKey("header-name")
    public Map<String, String> headers;

    /**
     * Set to true to share the HTTP client between REST clients.
     * There can be multiple shared clients distinguished by <em>name</em>, when no specific name is set,
     * the name <code>__vertx.DEFAULT</code> is used.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @ConfigItem
    public Optional<Boolean> shared;

    /**
     * Set the HTTP client name, used when the client is shared, otherwise ignored.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @ConfigItem
    public Optional<String> name;

    /**
     * Configure the HTTP user-agent header to use.
     * <p>
     * This property is not applicable to the RESTEasy Client.
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
     * This property is not applicable to the RESTEasy Client.
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
        var config = (SmallRyeConfig) ConfigProvider.getConfig();
        if (!atLeastOnPropertyExists(configKey, config)) {
            // there is no reason to query Smallrye Config a ton of times if we know there are no properties...
            return EMPTY;
        }

        var instance = new RestClientConfig();
        var configKeyPrefixes = new ConfigKeyPrefixes(composePropertyPrefix(configKey),
                composePropertyPrefix('"' + configKey + '"'));

        instance.url = getConfigValue(configKeyPrefixes, "url", String.class, config);
        instance.uri = getConfigValue(configKeyPrefixes, "uri", String.class, config);
        instance.overrideUri = getConfigValue(configKeyPrefixes, "override-uri", String.class, config);
        instance.providers = getConfigValue(configKeyPrefixes, "providers", String.class, config);
        instance.connectTimeout = getConfigValue(configKeyPrefixes, "connect-timeout", Long.class, config);
        instance.readTimeout = getConfigValue(configKeyPrefixes, "read-timeout", Long.class, config);
        instance.followRedirects = getConfigValue(configKeyPrefixes, "follow-redirects", Boolean.class, config);
        instance.multipartPostEncoderMode = getConfigValue(configKeyPrefixes, "multipart-post-encoder-mode", String.class,
                config);
        instance.proxyAddress = getConfigValue(configKeyPrefixes, "proxy-address", String.class, config);
        instance.proxyUser = getConfigValue(configKeyPrefixes, "proxy-user", String.class, config);
        instance.proxyPassword = getConfigValue(configKeyPrefixes, "proxy-password", String.class, config);
        instance.nonProxyHosts = getConfigValue(configKeyPrefixes, "non-proxy-hosts", String.class, config);
        instance.queryParamStyle = getConfigValue(configKeyPrefixes, "query-param-style", QueryParamStyle.class, config);
        instance.verifyHost = getConfigValue(configKeyPrefixes, "verify-host", Boolean.class, config);
        instance.trustStore = getConfigValue(configKeyPrefixes, "trust-store", String.class, config);
        instance.trustStorePassword = getConfigValue(configKeyPrefixes, "trust-store-password", String.class, config);
        instance.trustStoreType = getConfigValue(configKeyPrefixes, "trust-store-type", String.class, config);
        instance.keyStore = getConfigValue(configKeyPrefixes, "key-store", String.class, config);
        instance.keyStorePassword = getConfigValue(configKeyPrefixes, "key-store-password", String.class, config);
        instance.keyStoreType = getConfigValue(configKeyPrefixes, "key-store-type", String.class, config);
        instance.hostnameVerifier = getConfigValue(configKeyPrefixes, "hostname-verifier", String.class, config);
        instance.tlsConfigurationName = getConfigValue(configKeyPrefixes, "tls-configuration-name", String.class, config);
        instance.connectionTTL = getConfigValue(configKeyPrefixes, "connection-ttl", Integer.class, config);
        instance.connectionPoolSize = getConfigValue(configKeyPrefixes, "connection-pool-size", Integer.class, config);
        instance.keepAliveEnabled = getConfigValue(configKeyPrefixes, "keep-alive-enabled", Boolean.class, config);
        instance.maxRedirects = getConfigValue(configKeyPrefixes, "max-redirects", Integer.class, config);
        instance.headers = getMapConfigValue(configKeyPrefixes, "headers", String.class, String.class, config);
        instance.shared = getConfigValue(configKeyPrefixes, "shared", Boolean.class, config);
        instance.name = getConfigValue(configKeyPrefixes, "name", String.class, config);
        instance.userAgent = getConfigValue(configKeyPrefixes, "user-agent", String.class, config);
        instance.http2 = getConfigValue(configKeyPrefixes, "http2", Boolean.class, config);
        instance.maxChunkSize = getConfigValue(configKeyPrefixes, "max-chunk-size", MemorySize.class, config);
        instance.alpn = getConfigValue(configKeyPrefixes, "alpn", Boolean.class, config);
        instance.captureStacktrace = getConfigValue(configKeyPrefixes, "capture-stacktrace", Boolean.class, config);

        instance.multipart = new RestClientMultipartConfig();
        instance.multipart.maxChunkSize = getConfigValue(configKeyPrefixes, "multipart.max-chunk-size", Integer.class, config);

        return instance;
    }

    public static RestClientConfig load(Class<?> interfaceClass) {
        var config = (SmallRyeConfig) ConfigProvider.getConfig();
        if (!atLeastOnPropertyExists(interfaceClass, config)) {
            // there is no reason to query Smallrye Config a ton of times if we know there are no properties...
            return EMPTY;
        }

        var instance = new RestClientConfig();
        var prefixes = new InterfaceNamePrefixes(
                composePropertyPrefix('"' + interfaceClass.getName() + '"'),
                composePropertyPrefix(interfaceClass.getSimpleName()),
                composePropertyPrefix('"' + interfaceClass.getSimpleName() + '"'));

        instance.url = getConfigValue(prefixes, "url", String.class, config);
        instance.uri = getConfigValue(prefixes, "uri", String.class, config);
        instance.overrideUri = getConfigValue(prefixes, "override-uri", String.class, config);
        instance.providers = getConfigValue(prefixes, "providers", String.class, config);
        instance.connectTimeout = getConfigValue(prefixes, "connect-timeout", Long.class, config);
        instance.readTimeout = getConfigValue(prefixes, "read-timeout", Long.class, config);
        instance.followRedirects = getConfigValue(prefixes, "follow-redirects", Boolean.class, config);
        instance.proxyAddress = getConfigValue(prefixes, "proxy-address", String.class, config);
        instance.proxyUser = getConfigValue(prefixes, "proxy-user", String.class, config);
        instance.proxyPassword = getConfigValue(prefixes, "proxy-password", String.class, config);
        instance.nonProxyHosts = getConfigValue(prefixes, "non-proxy-hosts", String.class, config);
        instance.queryParamStyle = getConfigValue(prefixes, "query-param-style", QueryParamStyle.class, config);
        instance.verifyHost = getConfigValue(prefixes, "verify-host", Boolean.class, config);
        instance.trustStore = getConfigValue(prefixes, "trust-store", String.class, config);
        instance.trustStorePassword = getConfigValue(prefixes, "trust-store-password", String.class, config);
        instance.trustStoreType = getConfigValue(prefixes, "trust-store-type", String.class, config);
        instance.keyStore = getConfigValue(prefixes, "key-store", String.class, config);
        instance.keyStorePassword = getConfigValue(prefixes, "key-store-password", String.class, config);
        instance.keyStoreType = getConfigValue(prefixes, "key-store-type", String.class, config);
        instance.hostnameVerifier = getConfigValue(prefixes, "hostname-verifier", String.class, config);
        instance.tlsConfigurationName = getConfigValue(prefixes, "tls-configuration-name", String.class, config);
        instance.connectionTTL = getConfigValue(prefixes, "connection-ttl", Integer.class, config);
        instance.connectionPoolSize = getConfigValue(prefixes, "connection-pool-size", Integer.class, config);
        instance.keepAliveEnabled = getConfigValue(prefixes, "keep-alive-enabled", Boolean.class, config);
        instance.maxRedirects = getConfigValue(prefixes, "max-redirects", Integer.class, config);
        instance.headers = getMapConfigValue(prefixes, "headers", String.class, String.class, config);
        instance.shared = getConfigValue(prefixes, "shared", Boolean.class, config);
        instance.name = getConfigValue(prefixes, "name", String.class, config);
        instance.userAgent = getConfigValue(prefixes, "user-agent", String.class, config);
        instance.http2 = getConfigValue(prefixes, "http2", Boolean.class, config);
        instance.maxChunkSize = getConfigValue(prefixes, "max-chunk-size", MemorySize.class, config);
        instance.alpn = getConfigValue(prefixes, "alpn", Boolean.class, config);
        instance.captureStacktrace = getConfigValue(prefixes, "capture-stacktrace", Boolean.class, config);

        instance.multipart = new RestClientMultipartConfig();
        instance.multipart.maxChunkSize = getConfigValue(prefixes, "multipart.max-chunk-size", Integer.class, config);

        return instance;
    }

    public static <T> Optional<T> getConfigValue(String configKey, String fieldName, Class<T> type) {
        var config = (SmallRyeConfig) ConfigProvider.getConfig();
        var configKeyPrefixes = new ConfigKeyPrefixes(composePropertyPrefix(configKey),
                composePropertyPrefix('"' + configKey + '"'));
        return getConfigValue(configKeyPrefixes, fieldName, type, config);
    }

    public static <T> Optional<T> getConfigValue(Class<?> clientInterface, String fieldName, Class<T> type) {
        var config = (SmallRyeConfig) ConfigProvider.getConfig();
        var prefixes = new InterfaceNamePrefixes(
                composePropertyPrefix('"' + clientInterface.getName() + '"'),
                composePropertyPrefix(clientInterface.getSimpleName()),
                composePropertyPrefix('"' + clientInterface.getSimpleName() + '"'));
        return getConfigValue(prefixes, fieldName, type, config);
    }

    private static boolean atLeastOnPropertyExists(String configKeyPart, SmallRyeConfig config) {
        var allPropertyNames = config.getPropertyNames();
        for (String propertyName : allPropertyNames) {
            if ((propertyName.startsWith(Constants.QUARKUS_CONFIG_PREFIX) && propertyName.contains(configKeyPart))
                    || (propertyName.startsWith(configKeyPart) && propertyName.contains(Constants.MP_REST))) {
                return true;
            }
        }
        return false;
    }

    private static boolean atLeastOnPropertyExists(Class<?> interfaceClass, SmallRyeConfig config) {
        var allPropertyNames = config.getPropertyNames();
        for (String propertyName : allPropertyNames) {
            if ((propertyName.startsWith(interfaceClass.getName()) && propertyName.contains(Constants.MP_REST))
                    || (propertyName.startsWith(Constants.QUARKUS_CONFIG_PREFIX)
                            && propertyName.contains(interfaceClass.getSimpleName()))) {
                return true;
            }
        }
        return false;
    }

    private static <T> Optional<T> getConfigValue(ConfigKeyPrefixes prefixes, String fieldName, Class<T> type,
            SmallRyeConfig config) {
        var optional = config.getOptionalValue(prefixes.normal + fieldName, type);
        if (optional.isEmpty()) { // try to find property with quoted configKey
            optional = config.getOptionalValue(prefixes.quoted + fieldName, type);
        }
        return optional;
    }

    private static <T> Optional<T> getConfigValue(InterfaceNamePrefixes interfaceNamePrefixes, String fieldName, Class<T> type,
            SmallRyeConfig config) {
        // first try interface full name
        var optional = config.getOptionalValue(interfaceNamePrefixes.quotedFullName + fieldName, type);
        if (optional.isEmpty()) { // then interface simple name
            optional = config.getOptionalValue(interfaceNamePrefixes.unquotedSimpleName + fieldName, type);
        }
        if (optional.isEmpty()) { // lastly quoted interface simple name
            optional = config.getOptionalValue(interfaceNamePrefixes.quotedSimpleName + fieldName, type);
        }
        return optional;
    }

    private static <K, V> Map<K, V> getMapConfigValue(ConfigKeyPrefixes prefixes, String fieldName, Class<K> keyType,
            Class<V> valueType, SmallRyeConfig config) {
        var optional = config.getOptionalValues(prefixes.normal + fieldName, keyType, valueType);
        if (optional.isEmpty()) { // try to find property with quoted configKey
            optional = config.getOptionalValues(prefixes.quoted + fieldName, keyType,
                    valueType);
        }
        return optional.isPresent() ? optional.get() : Collections.emptyMap();
    }

    private static <K, V> Map<K, V> getMapConfigValue(InterfaceNamePrefixes namePrefixes, String fieldName, Class<K> keyType,
            Class<V> valueType, SmallRyeConfig config) {
        // first try interface full name
        Optional<Map<K, V>> optional = config.getOptionalValues(namePrefixes.quotedFullName + fieldName, keyType, valueType);
        if (optional.isEmpty()) { // then interface simple name
            optional = config.getOptionalValues(namePrefixes.unquotedSimpleName + fieldName, keyType, valueType);
        }
        if (optional.isEmpty()) { // lastly quoted interface simple name
            optional = config.getOptionalValues(namePrefixes.quotedSimpleName + fieldName, keyType, valueType);
        }
        return optional.isPresent() ? optional.get() : Collections.emptyMap();
    }

    private static String composePropertyPrefix(String key) {
        return Constants.QUARKUS_CONFIG_PREFIX + key + ".";
    }

    private record ConfigKeyPrefixes(String normal, String quoted) {

    }

    private record InterfaceNamePrefixes(String quotedFullName, String unquotedSimpleName, String quotedSimpleName) {

    }
}
