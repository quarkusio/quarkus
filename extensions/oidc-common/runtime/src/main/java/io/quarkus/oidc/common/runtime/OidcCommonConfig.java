package io.quarkus.oidc.common.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class OidcCommonConfig {
    /**
     * The base URL of the OpenID Connect (OIDC) server, for example, `https://host:port/auth`.
     * OIDC discovery endpoint will be called by default by appending a '.well-known/openid-configuration' path to this URL.
     * Note if you work with Keycloak OIDC server, make sure the base URL is in the following format:
     * `https://host:port/auth/realms/{realm}` where `{realm}` has to be replaced by the name of the Keycloak realm.
     */
    @ConfigItem
    public Optional<String> authServerUrl = Optional.empty();

    /**
     * Enables OIDC discovery.
     * If the discovery is disabled then the 'token-path' property must be configured.
     */
    @ConfigItem(defaultValue = "true")
    public boolean discoveryEnabled = true;

    /**
     * Relative path of the OIDC token endpoint which issues access and refresh tokens
     * using either 'client_credentials' or 'password' grants
     */
    @ConfigItem
    public Optional<String> tokenPath = Optional.empty();

    /**
     * The client-id of the application. Each application has a client-id that is used to identify the application
     */
    @ConfigItem
    public Optional<String> clientId = Optional.empty();

    /**
     * The maximum amount of time connecting to the currently unavailable OIDC server will be attempted for.
     * The number of times the connection request will be repeated is calculated by dividing the value of this property by 2.
     * For example, setting it to `20S` will allow for requesting the connection up to 10 times with a 2 seconds delay between
     * the retries.
     * Note the `connection-timeout` property does not affect this amount of time.
     */
    @ConfigItem
    public Optional<Duration> connectionDelay = Optional.empty();

    /**
     * The amount of time after which the connection request to the currently unavailable OIDC server will time out.
     */
    @ConfigItem(defaultValue = "10s")
    public Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * Credentials which the OIDC adapter will use to authenticate to the OIDC server.
     */
    @ConfigItem
    public Credentials credentials = new Credentials();

    /**
     * Options to configure a proxy that OIDC adapter will use for talking with OIDC server.
     */
    @ConfigItem
    public Proxy proxy = new Proxy();

    /**
     * TLS configurations
     */
    @ConfigItem
    public Tls tls = new Tls();

    @ConfigGroup
    public static class Credentials {

        /**
         * Client secret which is used for a `client_secret_basic` authentication method.
         * Note that a 'client-secret.value' can be used instead but both properties are mutually exclusive.
         */
        @ConfigItem
        public Optional<String> secret = Optional.empty();

        /**
         * Client secret which can be used for the `client_secret_basic` (default) and `client_secret_post`
         * and 'client_secret_jwt' authentication methods.
         * Note that a `secret.value` property can be used instead to support the `client_secret_basic` method
         * but both properties are mutually exclusive.
         */
        @ConfigItem
        public Secret clientSecret = new Secret();

        /**
         * Client JWT authentication methods
         */
        @ConfigItem
        public Jwt jwt = new Jwt();

        public Optional<String> getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = Optional.of(secret);
        }

        public Secret getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(Secret clientSecret) {
            this.clientSecret = clientSecret;
        }

        /**
         * Supports the client authentication methods which involve sending a client secret.
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        @ConfigGroup
        public static class Secret {

            public static enum Method {
                /**
                 * client_secret_basic (default): client id and secret are submitted with the HTTP Authorization Basic scheme
                 */
                BASIC,

                /**
                 * client_secret_post: client id and secret are submitted as the 'client_id' and 'client_secret' form
                 * parameters.
                 */
                POST
            }

            /**
             * The client secret
             */
            @ConfigItem
            public Optional<String> value = Optional.empty();

            /**
             * Authentication method.
             */
            @ConfigItem
            public Optional<Method> method = Optional.empty();

            public Optional<String> getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = Optional.of(value);
            }

            public Optional<Method> getMethod() {
                return method;
            }

            public void setMethod(Method method) {
                this.method = Optional.of(method);
            }
        }

        /**
         * Supports the client authentication 'client_secret_jwt' and 'private_key_jwt' methods which involve sending a JWT
         * token
         * assertion signed with either a client secret or private key.
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        @ConfigGroup
        public static class Jwt {
            /**
             * If provided, indicates that JWT is signed using a secret key
             */
            @ConfigItem
            public Optional<String> secret = Optional.empty();

            /**
             * If provided, indicates that JWT is signed using a private key in PEM or JWK format
             */
            @ConfigItem
            public Optional<String> keyFile = Optional.empty();

            /**
             * If provided, indicates that JWT is signed using a private key from a key store
             */
            @ConfigItem
            public Optional<String> keyStoreFile = Optional.empty();

            /**
             * A parameter to specify the password of the key store file. If not given, the default ("password") is used.
             */
            @ConfigItem(defaultValue = "password")
            public String keyStorePassword;

            /**
             * The private key id/alias
             */
            @ConfigItem
            public Optional<String> keyId = Optional.empty();

            /**
             * The private key password
             */
            @ConfigItem(defaultValue = "password")
            public String keyPassword;

            /**
             * JWT life-span in seconds. It will be added to the time it was issued at to calculate the expiration time.
             */
            @ConfigItem(defaultValue = "10")
            public int lifespan = 10;

            public Optional<String> getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = Optional.of(secret);
            }

            public int getLifespan() {
                return lifespan;
            }

            public void setLifespan(int lifespan) {
                this.lifespan = lifespan;
            }
        }
    }

    @ConfigGroup
    public static class Tls {
        public enum Verification {
            /**
             * Certificates are validated and hostname verification is enabled. This is the default value.
             */
            REQUIRED,
            /**
             * All certificated are trusted and hostname verification is disabled.
             */
            NONE
        }

        /**
         * Certificate validation and hostname verification, which can be one of the following values from enum
         * {@link Verification}. Default is required.
         */
        @ConfigItem
        public Optional<Verification> verification = Optional.empty();

        public Optional<Verification> getVerification() {
            return verification;
        }

        public void setVerification(Verification verification) {
            this.verification = Optional.of(verification);
        }

    }

    @ConfigGroup
    public static class Proxy {

        /**
         * The host (name or IP address) of the Proxy.<br/>
         * Note: If OIDC adapter needs to use a Proxy to talk with OIDC server (Provider),
         * then at least the "host" config item must be configured to enable the usage of a Proxy.
         */
        @ConfigItem
        public Optional<String> host = Optional.empty();

        /**
         * The port number of the Proxy. Default value is 80.
         */
        @ConfigItem(defaultValue = "80")
        public int port = 80;

        /**
         * The username, if Proxy needs authentication.
         */
        @ConfigItem
        public Optional<String> username = Optional.empty();

        /**
         * The password, if Proxy needs authentication.
         */
        @ConfigItem
        public Optional<String> password = Optional.empty();

    }

    public Optional<Duration> getConnectionDelay() {
        return connectionDelay;
    }

    public void setConnectionDelay(Duration connectionDelay) {
        this.connectionDelay = Optional.of(connectionDelay);
    }

    public Optional<String> getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = Optional.of(authServerUrl);
    }

    public Optional<String> getTokenPath() {
        return tokenPath;
    }

    public void setTokenPath(String tokenPath) {
        this.tokenPath = Optional.of(tokenPath);
    }

    public Optional<String> getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = Optional.of(clientId);
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public boolean isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public void setDiscoveryEnabled(boolean enabled) {
        this.discoveryEnabled = enabled;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
