package io.quarkus.restclient.config;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.CreationException;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;

@ConfigRoot(name = "rest-client", phase = ConfigPhase.RUN_TIME)
public class RestClientsConfig {

    /**
     * Configurations of REST client instances.
     * <p>
     * The key can be either the value of the configKey parameter of a `@RegisterRestClient` annotation, or the name of
     * a class bearing that annotation, in which case it is possible to use the short name, as well as fully qualified
     * name.
     */
    // This variable is only here to avoid warnings about unrecognized configuration keys. The map is otherwise ignored,
    // and the RestClientConfig instances are loaded via `RestClientConfig#load()` methods instead.
    @ConfigItem(name = ConfigItem.PARENT)
    Map<String, RestClientConfig> configKey;

    @SuppressWarnings("DeprecatedIsStillUsed")
    // The @Deprecated annotation prevents this field from being included in generated docs. We only want the `configKey` field
    // above to be included.
    @Deprecated
    private final Map<String, RestClientConfig> configs = new ConcurrentHashMap<>();

    /**
     * By default, REST Client Reactive uses text/plain content type for String values
     * and application/json for everything else.
     * <p>
     * MicroProfile Rest Client spec requires the implementations to always default to application/json.
     * This build item disables the "smart" behavior of RESTEasy Reactive to comply to the spec.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem(defaultValue = "false")
    public Optional<Boolean> disableSmartProduces;

    /**
     * Mode in which the form data are encoded. Possible values are `HTML5`, `RFC1738` and `RFC3986`.
     * The modes are described in the
     * <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/multipart/HttpPostRequestEncoder.EncoderMode.html">Netty
     * documentation</a>
     * <p>
     * By default, Rest Client Reactive uses RFC1738.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> multipartPostEncoderMode;

    /**
     * A string value in the form of `<proxyHost>:<proxyPort>` that specifies the HTTP proxy server hostname
     * (or IP address) and port for requests of clients to use.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> proxyAddress;

    /**
     * Proxy username, equivalent to the http.proxy or https.proxy JVM settings.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyUser;

    /**
     * Proxy password, equivalent to the http.proxyPassword or https.proxyPassword JVM settings.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyPassword;

    /**
     * Hosts to access without proxy, similar to the http.nonProxyHosts or https.nonProxyHosts JVM settings.
     * Please note that unlike the JVM settings, this property is empty by default.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> nonProxyHosts;

    public RestClientLoggingConfig logging;

    public RestClientMultipartConfig multipart;

    /**
     * A timeout in milliseconds that REST clients should wait to connect to the remote endpoint.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem(defaultValue = "15000", defaultValueDocumentation = "15000 ms")
    public Long connectTimeout;

    /**
     * A timeout in milliseconds that REST clients should wait for a response from the remote endpoint.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem(defaultValue = "30000", defaultValueDocumentation = "30000 ms")
    public Long readTimeout;

    /**
     * If true, the REST clients will not provide additional contextual information (like REST client class and method
     * names) when exception occurs during a client invocation.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableContextualErrorMessages;

    /**
     * Default configuration for the HTTP user-agent header to use in all REST clients.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> userAgent;

    /**
     * The HTTP headers that should be applied to all requests of the rest client.
     */
    @ConfigItem
    public Map<String, String> headers;

    /**
     * The class name of the host name verifier. The class must have a public no-argument constructor.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> hostnameVerifier;

    /**
     * The time in ms for which a connection remains unused in the connection pool before being evicted and closed.
     * A timeout of {@code 0} means there is no timeout.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<Integer> connectionTTL;

    /**
     * The size of the connection pool for this client.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<Integer> connectionPoolSize;

    /**
     * If set to false disables the keep alive completely.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem(defaultValue = "true")
    public Optional<Boolean> keepAliveEnabled;

    /**
     * The maximum number of redirection a request can follow.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<Integer> maxRedirects;

    /**
     * A boolean value used to determine whether the client should follow HTTP redirect responses.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<Boolean> followRedirects;

    /**
     * Map where keys are fully-qualified provider classnames to include in the client, and values are their integer
     * priorities. The equivalent of the `@RegisterProvider` annotation.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> providers;

    /**
     * The CDI scope to use for injections of REST client instances. Value can be either a fully qualified class name of a CDI
     * scope annotation (such as "jakarta.enterprise.context.ApplicationScoped") or its simple name (such
     * as"ApplicationScoped").
     * <p>
     * Default scope for the rest-client extension is "Dependent" (which is the spec-compliant behavior).
     * <p>
     * Default scope for the rest-client-reactive extension is "ApplicationScoped".
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> scope;

    /**
     * An enumerated type string value with possible values of "MULTI_PAIRS" (default), "COMMA_SEPARATED",
     * or "ARRAY_PAIRS" that specifies the format in which multiple values for the same query parameter is used.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<QueryParamStyle> queryParamStyle;

    /**
     * Set whether hostname verification is enabled. Default is enabled.
     * This setting should not be disabled in production as it makes the client vulnerable to MITM attacks.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<Boolean> verifyHost;

    /**
     * The trust store location. Can point to either a classpath resource or a file.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> trustStore;

    /**
     * The trust store password.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> trustStorePassword;

    /**
     * The type of the trust store. Defaults to "JKS".
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> trustStoreType;

    /**
     * The key store location. Can point to either a classpath resource or a file.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> keyStore;

    /**
     * The key store password.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> keyStorePassword;

    /**
     * The type of the key store. Defaults to "JKS".
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> keyStoreType;

    /**
     * If this is true then HTTP/2 will be enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean http2;

    /**
     * The max HTTP chunk size (8096 bytes by default).
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    @ConfigDocDefault("8k")
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
    @ConfigItem(defaultValue = "true")
    public boolean captureStacktrace;

    public RestClientConfig getClientConfig(String configKey) {
        if (configKey == null) {
            return RestClientConfig.EMPTY;
        }
        return configs.computeIfAbsent(configKey, RestClientConfig::load);
    }

    public RestClientConfig getClientConfig(Class<?> clientInterface) {
        return configs.computeIfAbsent(clientInterface.getName(), name -> RestClientConfig.load(clientInterface));
    }

    public void putClientConfig(String configKey, RestClientConfig clientConfig) {
        configs.put(configKey, clientConfig);
    }

    public void putClientConfig(Class<?> clientInterface, RestClientConfig clientConfig) {
        configs.put(clientInterface.getName(), clientConfig);
    }

    public Set<String> getConfigKeys() {
        return configs.keySet();
    }

    public static RestClientsConfig getInstance() {
        InstanceHandle<RestClientsConfig> configHandle;
        try {
            configHandle = Arc.container().instance(RestClientsConfig.class);
        } catch (CreationException e) {
            String message = "The Rest Client configuration cannot be initialized at this stage. "
                    + "Try to wrap your Rest Client injection in the Provider<> interface:\n\n"
                    + "  @Inject\n"
                    + "  @RestClient\n"
                    + "  Provider<MyRestClientInterface> myRestClient;\n";
            throw new RuntimeException(message, e);
        }
        if (!configHandle.isAvailable()) {
            throw new IllegalStateException("Unable to find the RestClientConfigs");
        }
        return configHandle.get();
    }
}
