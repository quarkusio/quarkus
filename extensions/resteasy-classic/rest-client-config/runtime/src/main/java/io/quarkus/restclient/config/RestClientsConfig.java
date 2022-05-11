package io.quarkus.restclient.config;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.CreationException;

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
     * (or IP address) and port for requests of clients to use. Can be overwritten by client-specific settings
     *
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyAddress;

    /**
     * Proxy username, equivalent to the http.proxy or https.proxy JVM settings.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyUser;

    /**
     * Proxy password, equivalent to the http.proxyPassword or https.proxyPassword JVM settings.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> proxyPassword;

    /**
     * Hosts to access without proxy, similar to the http.nonProxyHosts or https.nonProxyHosts JVM settings.
     * Please note that unlike the JVM settings, this property is empty by default
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> nonProxyHosts;

    public RestClientLoggingConfig logging;

    /**
     * Global default connect timeout for automatically generated REST Clients. The attribute specifies a timeout
     * in milliseconds that a client should wait to connect to the remote endpoint.
     */
    @ConfigItem(defaultValue = "15000", defaultValueDocumentation = "15000 ms")
    public Long connectTimeout;

    /**
     * Global default read timeout for automatically generated REST clients. The attribute specifies a timeout
     * in milliseconds that a client should wait for a response from the remote endpoint.
     */
    @ConfigItem(defaultValue = "30000", defaultValueDocumentation = "30000 ms")
    public Long readTimeout;

    /**
     * If true, the reactive REST clients will not provide additional contextual information (like REST client class and method
     * names) when exception occurs during a client invocation.
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableContextualErrorMessages;

    /**
     * Configure the HTTP user-agent header to use.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> userAgent;

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
