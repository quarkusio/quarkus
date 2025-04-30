package io.quarkus.oidc.common.runtime.config;

import java.nio.file.Path;
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
     * The base URL of the OpenID Connect (OIDC) server, for example, `https://host:port/auth`.
     * Do not set this property if you use 'quarkus-oidc' and the public key verification ({@link #publicKey})
     * or certificate chain verification only ({@link #certificateChain}) is required.
     * The OIDC discovery endpoint is called by default by appending a `.well-known/openid-configuration` path to this URL.
     * For Keycloak, use `https://host:port/realms/{realm}`, replacing `{realm}` with the Keycloak realm name.
     */
    Optional<String> authServerUrl();

    /**
     * Discovery of the OIDC endpoints.
     * If not enabled, you must configure the OIDC endpoint URLs individually.
     */
    @ConfigDocDefault("true")
    Optional<Boolean> discoveryEnabled();

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

        enum Verification {
            /**
             * Certificates are validated and hostname verification is enabled. This is the default value.
             */
            REQUIRED,

            /**
             * Certificates are validated but hostname verification is disabled.
             */
            CERTIFICATE_VALIDATION,

            /**
             * All certificates are trusted and hostname verification is disabled.
             */
            NONE
        }

        /**
         * Certificate validation and hostname verification, which can be one of the following {@link Verification}
         * values.
         * Default is `required`.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<Verification> verification();

        /**
         * An optional keystore that holds the certificate information instead of specifying separate files.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<Path> keyStoreFile();

        /**
         * The type of the keystore file. If not given, the type is automatically detected based on the file name.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> keyStoreFileType();

        /**
         * The provider of the keystore file. If not given, the provider is automatically detected based on the
         * keystore file type.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> keyStoreProvider();

        /**
         * The password of the keystore file. If not given, the default value, `password`, is used.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> keyStorePassword();

        /**
         * The alias of a specific key in the keystore.
         * When SNI is disabled, if the keystore contains multiple
         * keys and no alias is specified, the behavior is undefined.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> keyStoreKeyAlias();

        /**
         * The password of the key, if it is different from the {@link #keyStorePassword}.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> keyStoreKeyPassword();

        /**
         * The truststore that holds the certificate information of the certificates to trust.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<Path> trustStoreFile();

        /**
         * The password of the truststore file.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> trustStorePassword();

        /**
         * The alias of the truststore certificate.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> trustStoreCertAlias();

        /**
         * The type of the truststore file.
         * If not given, the type is automatically detected
         * based on the file name.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> trustStoreFileType();

        /**
         * The provider of the truststore file.
         * If not given, the provider is automatically detected
         * based on the truststore file type.
         *
         * @deprecated Use the TLS registry instead.
         */
        @Deprecated
        Optional<String> trustStoreProvider();

    }

    interface Proxy {

        /**
         * The host name or IP address of the Proxy.<br/>
         * Note: If the OIDC adapter requires a Proxy to talk with the OIDC server (Provider),
         * set this value to enable the usage of a Proxy.
         */
        Optional<String> host();

        /**
         * The port number of the Proxy. The default value is `80`.
         */
        @WithDefault("80")
        int port();

        /**
         * The username, if the Proxy needs authentication.
         */
        Optional<String> username();

        /**
         * The password, if the Proxy needs authentication.
         */
        Optional<String> password();

    }
}
