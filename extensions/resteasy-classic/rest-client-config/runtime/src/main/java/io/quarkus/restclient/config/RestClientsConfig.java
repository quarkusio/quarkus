package io.quarkus.restclient.config;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.CreationException;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rest-client", phase = ConfigPhase.RUN_TIME)
public class RestClientsConfig {

    /**
     * Configurations of REST client instances.
     *
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
     *
     * MicroProfile Rest Client spec requires the implementations to always default to application/json.
     * This build item disables the "smart" behavior of RESTEasy Reactive to comply to the spec.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem(defaultValue = "false")
    public Optional<Boolean> disableSmartProduces;

    /**
     * Mode in which the form data are encoded. Possible values are `HTML5`, `RFC1738` and `RFC3986`.
     * The modes are described in the
     * <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/multipart/HttpPostRequestEncoder.EncoderMode.html">Netty
     * documentation</a>
     *
     * By default, Rest Client Reactive uses RFC1738.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> multipartPostEncoderMode;

    /**
     * A string value in the form of `<proxyHost>:<proxyPort>` that specifies the HTTP proxy server hostname
     * (or IP address) and port for requests of clients to use.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> proxyAddress;

    /**
     * Proxy username, equivalent to the http.proxy or https.proxy JVM settings.
     *
     * Can be overwritten by client-specific settings.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyUser;

    /**
     * Proxy password, equivalent to the http.proxyPassword or https.proxyPassword JVM settings.
     *
     * Can be overwritten by client-specific settings.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyPassword;

    /**
     * Hosts to access without proxy, similar to the http.nonProxyHosts or https.nonProxyHosts JVM settings.
     * Please note that unlike the JVM settings, this property is empty by default.
     *
     * Can be overwritten by client-specific settings.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> nonProxyHosts;

    public RestClientLoggingConfig logging;

    /**
     * A timeout in milliseconds that REST clients should wait to connect to the remote endpoint.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem(defaultValue = "15000", defaultValueDocumentation = "15000 ms")
    public Long connectTimeout;

    /**
     * A timeout in milliseconds that REST clients should wait for a response from the remote endpoint.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem(defaultValue = "30000", defaultValueDocumentation = "30000 ms")
    public Long readTimeout;

    /**
     * If true, the REST clients will not provide additional contextual information (like REST client class and method
     * names) when exception occurs during a client invocation.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableContextualErrorMessages;

    /**
     * Default configuration for the HTTP user-agent header to use in all REST clients.
     *
     * Can be overwritten by client-specific settings.
     *
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
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> hostnameVerifier;

    /**
     * The time in ms for which a connection remains unused in the connection pool before being evicted and closed.
     * A timeout of {@code 0} means there is no timeout.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<Integer> connectionTTL;

    /**
     * The size of the connection pool for this client.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<Integer> connectionPoolSize;

    /**
     * The maximum number of redirection a request can follow.
     *
     * Can be overwritten by client-specific settings.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<Integer> maxRedirects;

    /**
     * A boolean value used to determine whether the client should follow HTTP redirect responses.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<Boolean> followRedirects;

    /**
     * Map where keys are fully-qualified provider classnames to include in the client, and values are their integer
     * priorities. The equivalent of the `@RegisterProvider` annotation.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> providers;

    /**
     * The CDI scope to use for injections of REST client instances. Value can be either a fully qualified class name of a CDI
     * scope annotation (such as "jakarta.enterprise.context.ApplicationScoped") or its simple name (such
     * as"ApplicationScoped").
     *
     * Default scope for the rest-client extension is "Dependent" (which is the spec-compliant behavior).
     *
     * Default scope for the rest-client-reactive extension is "ApplicationScoped".
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> scope;

    /**
     * An enumerated type string value with possible values of "MULTI_PAIRS" (default), "COMMA_SEPARATED",
     * or "ARRAY_PAIRS" that specifies the format in which multiple values for the same query parameter is used.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<QueryParamStyle> queryParamStyle;

    /**
     * The trust store location. Can point to either a classpath resource or a file.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> trustStore;

    /**
     * The trust store password.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> trustStorePassword;

    /**
     * The type of the trust store. Defaults to "JKS".
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> trustStoreType;

    /**
     * The key store location. Can point to either a classpath resource or a file.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> keyStore;

    /**
     * The key store password.
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> keyStorePassword;

    /**
     * The type of the key store. Defaults to "JKS".
     *
     * Can be overwritten by client-specific settings.
     */
    @ConfigItem
    public Optional<String> keyStoreType;

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
