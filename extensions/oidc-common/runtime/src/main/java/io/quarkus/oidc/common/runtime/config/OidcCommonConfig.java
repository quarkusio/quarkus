package io.quarkus.oidc.common.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface OidcCommonConfig {
    /**
     * The base URL of an OpenID Connect (OIDC) server, for example, `https://host:port/auth`.
     * Do not set this property if you use the public key verification ({@link #publicKey})
     * or certificate chain verification only ({@link #certificateChain}).
     * <p>
     * By default, when an OIDC configuration metadata discovery is enabled with the {@link #discoveryEnabled()} property,
     * it is retrieved from a well known provider endpoint with its URL calculated by appending a value
     * of the {@link #discoveryPath()} path such as `.well-known/openid-configuration` to this URL.
     * <p>
     * For Keycloak, use `https://host:port/realms/{realm}`, replacing `{realm}` with the Keycloak realm name.
     */
    Optional<String> authServerUrl();

    /**
     * Enable discovery of the OIDC endpoints.
     * If not enabled, you must configure the OIDC endpoint URLs individually.
     */
    @ConfigDocDefault("true")
    Optional<Boolean> discoveryEnabled();

    /**
     * The relative path of the OIDC discovery endpoint.
     */
    @WithDefault(".well-known/openid-configuration")
    String discoveryPath();

    /**
     * The relative path or absolute URL of the OIDC dynamic client registration endpoint.
     * Set if {@link #discoveryEnabled} is `false` or a discovered token endpoint path must be customized.
     */
    Optional<String> registrationPath();

    /**
     * The duration to attempt the initial connection to an OIDC server.
     * For example, setting the duration to `20S` allows 10 retries, each 2 seconds apart.
     * This property is only effective when the initial OIDC connection is created.
     * For dropped connections, use the `connection-retry-count` property instead.
     */
    Optional<Duration> connectionDelay();

    /**
     * The number of times to retry re-establishing an existing OIDC connection if it is temporarily lost.
     * Different from `connection-delay`, which applies only to initial connection attempts.
     * For instance, if a request to the OIDC token endpoint fails due to a connection issue, it will be retried as per this
     * setting.
     */
    @WithDefault("3")
    int connectionRetryCount();

    /**
     * The number of seconds after which the current OIDC connection request times out.
     */
    @WithDefault("10s")
    Duration connectionTimeout();

    /**
     * Whether DNS lookup should be performed on the worker thread.
     * Use this option when you can see logged warnings about blocked Vert.x event loop by HTTP requests to OIDC server.
     */
    @WithDefault("false")
    boolean useBlockingDnsLookup();

    /**
     * The maximum size of the connection pool used by the WebClient.
     */
    OptionalInt maxPoolSize();

    /**
     * Follow redirects automatically when WebClient gets HTTP 302.
     * When this property is disabled only a single redirect to exactly the same original URI
     * is allowed but only if one or more cookies were set during the redirect request.
     */
    @WithDefault("true")
    boolean followRedirects();

    /**
     * HTTP proxy configuration.
     */
    @ConfigDocSection
    Proxy proxy();

    /**
     * TLS configuration.
     */
    @ConfigDocSection
    Tls tls();

    interface Tls {

        /**
         * The name of the TLS configuration to use.
         * <p>
         * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
         * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
         * <p>
         * The default TLS configuration is <strong>not</strong> used by default.
         */
        Optional<String> tlsConfigurationName();
    }

    interface Proxy {

        /**
         * The name of the proxy configuration to use.
         * <p>
         * If a name is configured, it uses the configuration from {@code quarkus.proxy.<name>.*}.
         * Please note that the 'non-proxy-hosts' option is currently not supported.
         * If a name is configured, but no proxy configuration is found with that name then an error will be thrown.
         * <p>
         * The default proxy configuration is <strong>not</strong> used by default.
         */
        Optional<String> proxyConfigurationName();
    }
}
