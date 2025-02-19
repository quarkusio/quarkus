package io.quarkus.restclient.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithKeys;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.rest-client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface RestClientsConfig {
    /**
     * Configurations of REST client instances.
     * <p>
     * The key can be either the value of the configKey parameter of a `@RegisterRestClient` annotation, or the name of
     * a class bearing that annotation, in which case it is possible to use the short name, as well as fully qualified
     * name.
     */
    @WithParentName
    @WithDefaults
    @WithKeys(RestClientKeysProvider.class)
    @ConfigDocMapKey("client")
    Map<String, RestClientConfig> clients();

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
    Optional<String> multipartPostEncoderMode();

    /**
     * A string value in the form of `<proxyHost>:<proxyPort>` that specifies the HTTP proxy server hostname
     * (or IP address) and port for requests of clients to use.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> proxyAddress();

    /**
     * Proxy username, equivalent to the http.proxy or https.proxy JVM settings.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    Optional<String> proxyUser();

    /**
     * Proxy password, equivalent to the http.proxyPassword or https.proxyPassword JVM settings.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    Optional<String> proxyPassword();

    /**
     * Hosts to access without proxy, similar to the http.nonProxyHosts or https.nonProxyHosts JVM settings.
     * Please note that unlike the JVM settings, this property is empty by default.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    Optional<String> nonProxyHosts();

    /**
     * A timeout in milliseconds that REST clients should wait to connect to the remote endpoint.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @WithDefault("15000")
    Long connectTimeout();

    /**
     * A timeout in milliseconds that REST clients should wait for a response from the remote endpoint.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @WithDefault("30000")
    Long readTimeout();

    /**
     * If true, the REST clients will not provide additional contextual information (like REST client class and method
     * names) when exception occurs during a client invocation.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    @WithDefault("false")
    boolean disableContextualErrorMessages();

    /**
     * Default configuration for the HTTP user-agent header to use in all REST clients.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    Optional<String> userAgent();

    /**
     * The HTTP headers that should be applied to all requests of the rest client.
     */
    @ConfigDocMapKey("header-name")
    Map<String, String> headers();

    /**
     * The class name of the host name verifier. The class must have a public no-argument constructor.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<String> hostnameVerifier();

    /**
     * The time in ms for which a connection remains unused in the connection pool before being evicted and closed.
     * A timeout of {@code 0} means there is no timeout.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<Integer> connectionTTL();

    /**
     * The size of the connection pool for this client.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @WithDefault("50")
    Optional<Integer> connectionPoolSize();

    /**
     * If set to false disables the keep alive completely.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @WithDefault("true")
    Optional<Boolean> keepAliveEnabled();

    /**
     * The maximum number of redirection a request can follow.
     * <p>
     * Can be overwritten by client-specific settings.
     * <p>
     * This property is not applicable to the RESTEasy Client.
     */
    Optional<Integer> maxRedirects();

    /**
     * A boolean value used to determine whether the client should follow HTTP redirect responses.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<Boolean> followRedirects();

    /**
     * Fully-qualified provider classnames to include in the client. The equivalent of the `@RegisterProvider` annotation.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<String> providers();

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
    Optional<String> scope();

    /**
     * An enumerated type string value with possible values of "MULTI_PAIRS" (default), "COMMA_SEPARATED",
     * or "ARRAY_PAIRS" that specifies the format in which multiple values for the same query parameter is used.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<QueryParamStyle> queryParamStyle();

    /**
     * Set whether hostname verification is enabled. Default is enabled.
     * This setting should not be disabled in production as it makes the client vulnerable to MITM attacks.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<Boolean> verifyHost();

    /**
     * The trust store location. Can point to either a classpath resource or a file.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<String> trustStore();

    /**
     * The trust store password.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<String> trustStorePassword();

    /**
     * The type of the trust store. Defaults to "JKS".
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<String> trustStoreType();

    /**
     * The key store location. Can point to either a classpath resource or a file.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<String> keyStore();

    /**
     * The key store password.
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<String> keyStorePassword();

    /**
     * The type of the key store. Defaults to "JKS".
     * <p>
     * Can be overwritten by client-specific settings.
     */
    Optional<String> keyStoreType();

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
    Optional<String> tlsConfigurationName();

    /**
     * If this is true then HTTP/2 will be enabled.
     */
    @WithDefault("false")
    boolean http2();

    /**
     * The max HTTP chunk size (8096 bytes by default).
     * <p>
     * Can be overwritten by client-specific settings.
     */
    @ConfigDocDefault("8k")
    Optional<MemorySize> maxChunkSize();

    /**
     * If the Application-Layer Protocol Negotiation is enabled, the client will negotiate which protocol to use over the
     * protocols exposed by the server. By default, it will try to use HTTP/2 first and if it's not enabled, it will
     * use HTTP/1.1.
     * When the property `http2` is enabled, this flag will be automatically enabled.
     */
    Optional<Boolean> alpn();

    /**
     * If {@code true}, the stacktrace of the invocation of the REST Client method is captured.
     * This stacktrace will be used if the invocation throws an exception
     */
    @WithDefault("false")
    boolean captureStacktrace();

    /**
     * Logging configuration.
     */
    RestClientLoggingConfig logging();

    /**
     * Multipart configuration.
     */
    RestClientMultipartConfig multipart();

    default RestClientConfig getClient(final Class<?> restClientInterface) {
        if (RestClientKeysProvider.KEYS.contains(restClientInterface.getName())) {
            return clients().get(restClientInterface.getName());
        }
        throw new IllegalArgumentException("Unable to lookup configuration for REST Client " + restClientInterface.getName()
                + ". Please confirm if the REST Client is annotated with @RegisterRestClient");
    }

    interface RestClientLoggingConfig {
        /**
         * Scope of logging for the client.
         * <br/>
         * WARNING: beware of logging sensitive data
         * <br/>
         * The possible values are:
         * <ul>
         * <li>{@code request-response} - enables logging request and responses, including redirect responses</li>
         * <li>{@code all} - enables logging requests and responses and lower-level logging</li>
         * <li>{@code none} - no additional logging</li>
         * </ul>
         *
         * This property is applicable to reactive REST clients only.
         */
        Optional<String> scope();

        /**
         * How many characters of the body should be logged. Message body can be large and can easily pollute the logs.
         * <p>
         * By default, set to 100.
         * <p>
         * This property is applicable to reactive REST clients only.
         */
        @WithDefault("100")
        Integer bodyLimit();
    }

    interface RestClientMultipartConfig {
        /**
         * The max HTTP chunk size (8096 bytes by default).
         * <p>
         * This property is applicable to reactive REST clients only.
         *
         * @deprecated Use {@code quarkus.rest-client.max-chunk-size} instead
         */
        @Deprecated
        Optional<Integer> maxChunkSize();
    }

    interface RestClientConfig {
        /**
         * Multipart configuration.
         */
        RestClientMultipartConfig multipart();

        /**
         * The base URL to use for this service. This property or the `uri` property is considered required, unless
         * the `baseUri` attribute is configured in the `@RegisterRestClient` annotation.
         */
        Optional<String> url();

        /**
         * Duplicate mapping of {@link RestClientConfig#url()} to keep a reference of the name used to retrieve the
         * <code>url</code>. We need this to reload the <code>url</code> configuration in case it contains an expression
         * to <code>${quarkus.http.port}</code>, which is only set after we load the config.
         */
        @ConfigDocIgnore
        @WithName("url")
        ConfigValue urlValue();

        default Optional<String> urlReload() {
            SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
            return config.getOptionalValue(urlValue().getName(), String.class);
        }

        /**
         * The base URI to use for this service. This property or the `url` property is considered required, unless
         * the `baseUri` attribute is configured in the `@RegisterRestClient` annotation.
         */
        Optional<String> uri();

        /**
         * Duplicate mapping of {@link RestClientConfig#uri()} to keep a reference of the name used to retrieve the
         * <code>uri</code>. We need this to reload the <code>uri</code> configuration in case it contains an expression
         * to <code>${quarkus.http.port}</code>, which is only set after we load the config.
         */
        @ConfigDocIgnore
        @WithName("uri")
        ConfigValue uriValue();

        default Optional<String> uriReload() {
            SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
            return config.getOptionalValue(uriValue().getName(), String.class);
        }

        /**
         * This property is only meant to be set by advanced configurations to override whatever value was set for the uri or
         * url.
         * The override is done using the REST Client class name configuration syntax.
         * <p>
         * This property is not applicable to the RESTEasy Client, only the Quarkus Rest client (formerly RESTEasy Reactive
         * client).
         */
        Optional<String> overrideUri();

        /**
         * Map where keys are fully-qualified provider classnames to include in the client, and values are their integer
         * priorities. The equivalent of the `@RegisterProvider` annotation.
         */
        Optional<String> providers();

        /**
         * Timeout specified in milliseconds to wait to connect to the remote endpoint.
         */
        Optional<Long> connectTimeout();

        /**
         * Timeout specified in milliseconds to wait for a response from the remote endpoint.
         */
        Optional<Long> readTimeout();

        /**
         * A boolean value used to determine whether the client should follow HTTP redirect responses.
         */
        Optional<Boolean> followRedirects();

        /**
         * Mode in which the form data are encoded. Possible values are `HTML5`, `RFC1738` and `RFC3986`.
         * The modes are described in the
         * <a href=
         * "https://netty.io/4.1/api/io/netty/handler/codec/http/multipart/HttpPostRequestEncoder.EncoderMode.html">Netty
         * documentation</a>
         * <p>
         * By default, Rest Client Reactive uses RFC1738.
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        Optional<String> multipartPostEncoderMode();

        /**
         * A string value in the form of `<proxyHost>:<proxyPort>` that specifies the HTTP proxy server hostname
         * (or IP address) and port for requests of this client to use.
         * <p>
         * Use `none` to disable proxy
         */
        Optional<@WithConverter(TrimmedStringConverter.class) String> proxyAddress();

        /**
         * Proxy username.
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        Optional<String> proxyUser();

        /**
         * Proxy password.
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        Optional<String> proxyPassword();

        /**
         * Hosts to access without proxy
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        Optional<String> nonProxyHosts();

        /**
         * An enumerated type string value with possible values of "MULTI_PAIRS" (default), "COMMA_SEPARATED",
         * or "ARRAY_PAIRS" that specifies the format in which multiple values for the same query parameter is used.
         */
        Optional<QueryParamStyle> queryParamStyle();

        /**
         * Set whether hostname verification is enabled. Default is enabled.
         * This setting should not be disabled in production as it makes the client vulnerable to MITM attacks.
         */
        Optional<Boolean> verifyHost();

        /**
         * The trust store location. Can point to either a classpath resource or a file.
         */
        Optional<String> trustStore();

        /**
         * The trust store password.
         */
        Optional<String> trustStorePassword();

        /**
         * The type of the trust store. Defaults to "JKS".
         */
        Optional<String> trustStoreType();

        /**
         * The key store location. Can point to either a classpath resource or a file.
         */
        Optional<String> keyStore();

        /**
         * The key store password.
         */
        Optional<String> keyStorePassword();

        /**
         * The type of the key store. Defaults to "JKS".
         */
        Optional<String> keyStoreType();

        /**
         * The class name of the host name verifier. The class must have a public no-argument constructor.
         */
        Optional<String> hostnameVerifier();

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
        Optional<String> tlsConfigurationName();

        /**
         * The time in ms for which a connection remains unused in the connection pool before being evicted and closed.
         * A timeout of {@code 0} means there is no timeout.
         */
        Optional<Integer> connectionTTL();

        /**
         * The size of the connection pool for this client.
         */
        @WithDefault("50")
        Optional<Integer> connectionPoolSize();

        /**
         * If set to false disables the keep alive completely.
         */
        Optional<Boolean> keepAliveEnabled();

        /**
         * The maximum number of redirection a request can follow.
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        Optional<Integer> maxRedirects();

        /**
         * The HTTP headers that should be applied to all requests of the rest client.
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        @ConfigDocMapKey("header-name")
        Map<String, String> headers();

        /**
         * Set to true to share the HTTP client between REST clients.
         * There can be multiple shared clients distinguished by <em>name</em>, when no specific name is set,
         * the name <code>__vertx.DEFAULT</code> is used.
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        Optional<Boolean> shared();

        /**
         * Set the HTTP client name, used when the client is shared, otherwise ignored.
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        Optional<String> name();

        /**
         * Configure the HTTP user-agent header to use.
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        Optional<String> userAgent();

        /**
         * If this is true then HTTP/2 will be enabled.
         */
        Optional<Boolean> http2();

        /**
         * The max HTTP chunk size (8096 bytes by default).
         * <p>
         * This property is not applicable to the RESTEasy Client.
         */
        @ConfigDocDefault("8K")
        Optional<MemorySize> maxChunkSize();

        /**
         * If the Application-Layer Protocol Negotiation is enabled, the client will negotiate which protocol to use over the
         * protocols exposed by the server. By default, it will try to use HTTP/2 first and if it's not enabled, it will
         * use HTTP/1.1.
         * When the property `http2` is enabled, this flag will be automatically enabled.
         */
        Optional<Boolean> alpn();

        /**
         * If {@code true}, the stacktrace of the invocation of the REST Client method is captured.
         * This stacktrace will be used if the invocation throws an exception
         */
        Optional<Boolean> captureStacktrace();

        /**
         * If set to {@code true}, then this REST Client will not the default exception mapper which
         * always throws an exception if HTTP response code >= 400.
         * This property is not applicable to the RESTEasy Client.
         */
        @WithDefault("${microprofile.rest.client.disable.default.mapper:false}")
        Boolean disableDefaultMapper();

        /**
         * Logging configuration.
         */
        Optional<RestClientLoggingConfig> logging();
    }

    class RestClientKeysProvider implements Supplier<Iterable<String>> {
        static List<String> KEYS = new ArrayList<>();

        @Override
        public Iterable<String> get() {
            return KEYS;
        }
    }
}
