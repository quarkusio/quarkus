package io.quarkus.oidc.common.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

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
     * Note this property is only effective when the initial OIDC connection is created,
     * for example, when requesting a well-known OIDC configuration.
     * Use the 'connection-retry-count' property to support trying to re-establish an already available connection which may
     * have been
     * dropped.
     */
    @ConfigItem
    public Optional<Duration> connectionDelay = Optional.empty();

    /**
     * The number of times an attempt to re-establish an already available connection will be repeated for.
     * Note this property is different to the `connection-delay` property which is only effective during the initial OIDC
     * connection creation.
     * This property is used to try to recover the existing connection which may have been temporarily lost.
     * For example, if a request to the OIDC token endpoint fails due to a connection exception then the request will be retried
     * for
     * a number of times configured by this property.
     */
    @ConfigItem(defaultValue = "3")
    public int connectionRetryCount = 3;

    /**
     * The amount of time after which the current OIDC connection request will time out.
     */
    @ConfigItem(defaultValue = "10s")
    public Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * The maximum size of the connection pool used by the WebClient
     */
    @ConfigItem
    public OptionalInt maxPoolSize = OptionalInt.empty();

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

        public Jwt getJwt() {
            return jwt;
        }

        public void setJwt(Jwt jwt) {
            this.jwt = jwt;
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
             * The client secret value - it will be ignored if 'secret.key' is set
             */
            @ConfigItem
            public Optional<String> value = Optional.empty();

            /**
             * The Secret CredentialsProvider
             */
            @ConfigItem
            public Provider provider = new Provider();

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

            public Provider getSecretProvider() {
                return provider;
            }

            public void setSecretProvider(Provider secretProvider) {
                this.provider = secretProvider;
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
             * If provided, indicates that JWT is signed using a secret key provided by Secret CredentialsProvider
             */
            @ConfigItem
            public Provider secretProvider = new Provider();

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
             * Key identifier of the signing key added as a JWT 'kid' header
             */
            @ConfigItem
            public Optional<String> tokenKeyId = Optional.empty();

            /**
             * Signature algorithm.
             * Supported values: RS256, RS384, RS512, PS256, PS384, PS512, ES256, ES384, ES512, HS256, HS384, HS512.
             */
            @ConfigItem
            public Optional<String> signatureAlgorithm = Optional.empty();

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

            public Optional<String> getTokenKeyId() {
                return tokenKeyId;
            }

            public void setTokenKeyId(String tokenKeyId) {
                this.tokenKeyId = Optional.of(tokenKeyId);
            }

            public Provider getSecretProvider() {
                return secretProvider;
            }

            public void setSecretProvider(Provider secretProvider) {
                this.secretProvider = secretProvider;
            }

            public Optional<String> getSignatureAlgorithm() {
                return signatureAlgorithm;
            }

            public void setSignatureAlgorithm(String signatureAlgorithm) {
                this.signatureAlgorithm = Optional.of(signatureAlgorithm);
            }

        }

        /**
         * CredentialsProvider which provides a client secret
         */
        @ConfigGroup
        public static class Provider {

            /**
             * The CredentialsProvider name which should only be set if more than one CredentialsProvider is registered
             */
            @ConfigItem
            public Optional<String> name = Optional.empty();

            /**
             * The CredentialsProvider client secret key
             */
            @ConfigItem
            public Optional<String> key = Optional.empty();

            public Optional<String> getName() {
                return name;
            }

            public void setName(String name) {
                this.name = Optional.of(name);
            }

            public Optional<String> getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = Optional.of(key);
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
             * Certificates are validated but hostname verification is disabled.
             */
            CERTIFICATE_VALIDATION,

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

        /**
         * An optional trust store which holds the certificate information of the certificates to trust
         */
        @ConfigItem
        public Optional<Path> trustStoreFile = Optional.empty();

        /**
         * A parameter to specify the password of the trust store file.
         */
        @ConfigItem
        public Optional<String> trustStorePassword = Optional.empty();

        /**
         * A parameter to specify the alias of the trust store certificate.
         */
        @ConfigItem
        public Optional<String> trustStoreCertAlias = Optional.empty();

        public Optional<Verification> getVerification() {
            return verification;
        }

        public void setVerification(Verification verification) {
            this.verification = Optional.of(verification);
        }

        public Optional<Path> getTrustStoreFile() {
            return trustStoreFile;
        }

        public void setTrustStoreFile(Path trustStoreFile) {
            this.trustStoreFile = Optional.of(trustStoreFile);
        }

        public Optional<String> getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = Optional.of(trustStorePassword);
        }

        public Optional<String> getTrustStoreCertAlias() {
            return trustStoreCertAlias;
        }

        public void setTrustStoreCertAlias(String trustStoreCertAlias) {
            this.trustStoreCertAlias = Optional.of(trustStoreCertAlias);
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

    public OptionalInt getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = OptionalInt.of(maxPoolSize);
    }
}
