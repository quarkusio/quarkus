package io.quarkus.oidc.common.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class OidcCommonConfig {
    /**
     * The base URL of the OpenID Connect (OIDC) server, for example, `https://host:port/auth`.
     * Do not set this property if the public key verification ({@link #publicKey}) or certificate chain verification only
     * ({@link #certificateChain}) is required.
     * The OIDC discovery endpoint is called by default by appending a `.well-known/openid-configuration` path to this URL.
     * For Keycloak, use `https://host:port/realms/{realm}`, replacing `{realm}` with the Keycloak realm name.
     */
    @ConfigItem
    public Optional<String> authServerUrl = Optional.empty();

    /**
     * Discovery of the OIDC endpoints.
     * If not enabled, you must configure the OIDC endpoint URLs individually.
     */
    @ConfigItem(defaultValueDocumentation = "true")
    public Optional<Boolean> discoveryEnabled = Optional.empty();

    /**
     * The OIDC token endpoint that issues access and refresh tokens;
     * specified as a relative path or absolute URL.
     * Set if {@link #discoveryEnabled} is `false` or a discovered token endpoint path must be customized.
     */
    @ConfigItem
    public Optional<String> tokenPath = Optional.empty();

    /**
     * The relative path or absolute URL of the OIDC token revocation endpoint.
     */
    @ConfigItem
    public Optional<String> revokePath = Optional.empty();

    /**
     * The client id of the application. Each application has a client id that is used to identify the application.
     * Setting the client id is not required if {@link #applicationType} is `service` and no token introspection is required.
     */
    @ConfigItem
    public Optional<String> clientId = Optional.empty();

    /**
     * The duration to attempt the initial connection to an OIDC server.
     * For example, setting the duration to `20S` allows 10 retries, each 2 seconds apart.
     * This property is only effective when the initial OIDC connection is created.
     * For dropped connections, use the `connection-retry-count` property instead.
     */
    @ConfigItem
    public Optional<Duration> connectionDelay = Optional.empty();

    /**
     * The number of times to retry re-establishing an existing OIDC connection if it is temporarily lost.
     * Different from `connection-delay`, which applies only to initial connection attempts.
     * For instance, if a request to the OIDC token endpoint fails due to a connection issue, it will be retried as per this
     * setting.
     */
    @ConfigItem(defaultValue = "3")
    public int connectionRetryCount = 3;

    /**
     * The number of seconds after which the current OIDC connection request times out.
     */
    @ConfigItem(defaultValue = "10s")
    public Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * The maximum size of the connection pool used by the WebClient.
     */
    @ConfigItem
    public OptionalInt maxPoolSize = OptionalInt.empty();

    /**
     * Credentials the OIDC adapter uses to authenticate to the OIDC server.
     */
    @ConfigItem
    public Credentials credentials = new Credentials();

    /**
     * Options to configure the proxy the OIDC adapter uses to talk with the OIDC server.
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
         * The client secret used by the `client_secret_basic` authentication method.
         * Must be set unless a secret is set in {@link #clientSecret} or {@link #jwt} client authentication is required.
         * You can use `client-secret.value` instead, but both properties are mutually exclusive.
         */
        @ConfigItem
        public Optional<String> secret = Optional.empty();

        /**
         * The client secret used by the `client_secret_basic` (default), `client_secret_post`, or `client_secret_jwt`
         * authentication methods.
         * Note that a `secret.value` property can be used instead to support the `client_secret_basic` method
         * but both properties are mutually exclusive.
         */
        @ConfigItem
        public Secret clientSecret = new Secret();

        /**
         * Client JSON Web Token (JWT) authentication methods
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
         * Supports the client authentication methods that involve sending a client secret.
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        @ConfigGroup
        public static class Secret {

            public static enum Method {
                /**
                 * `client_secret_basic` (default): The client id and secret are submitted with the HTTP Authorization Basic
                 * scheme.
                 */
                BASIC,

                /**
                 * `client_secret_post`: The client id and secret are submitted as the `client_id` and `client_secret`
                 * form parameters.
                 */
                POST,

                /**
                 * `client_secret_jwt`: The client id and generated JWT secret are submitted as the `client_id` and
                 * `client_secret`
                 * form parameters.
                 */
                POST_JWT,

                /**
                 * client id and secret are submitted as HTTP query parameters. This option is only supported for the OIDC
                 * extension.
                 */
                QUERY
            }

            /**
             * The client secret value. This value is ignored if `credentials.secret` is set.
             * Must be set unless a secret is set in {@link #clientSecret} or {@link #jwt} client authentication is required.
             */
            @ConfigItem
            public Optional<String> value = Optional.empty();

            /**
             * The Secret CredentialsProvider.
             */
            @ConfigItem
            public Provider provider = new Provider();

            /**
             * The authentication method.
             * If the `clientSecret.value` secret is set, this method is `basic` by default.
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
         * Supports the client authentication `client_secret_jwt` and `private_key_jwt` methods, which involves sending a JWT
         * token assertion signed with a client secret or private key.
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        @ConfigGroup
        public static class Jwt {
            /**
             * If provided, indicates that JWT is signed using a secret key.
             */
            @ConfigItem
            public Optional<String> secret = Optional.empty();

            /**
             * If provided, indicates that JWT is signed using a secret key provided by Secret CredentialsProvider.
             */
            @ConfigItem
            public Provider secretProvider = new Provider();

            /**
             * If provided, indicates that JWT is signed using a private key in PEM or JWK format.
             * You can use the {@link #signatureAlgorithm} property to override the default key algorithm, `RS256`.
             */
            @ConfigItem
            public Optional<String> keyFile = Optional.empty();

            /**
             * If provided, indicates that JWT is signed using a private key from a keystore.
             */
            @ConfigItem
            public Optional<String> keyStoreFile = Optional.empty();

            /**
             * A parameter to specify the password of the keystore file.
             */
            @ConfigItem
            public Optional<String> keyStorePassword;

            /**
             * The private key id or alias.
             */
            @ConfigItem
            public Optional<String> keyId = Optional.empty();

            /**
             * The private key password.
             */
            @ConfigItem
            public Optional<String> keyPassword;

            /**
             * The JWT audience (`aud`) claim value.
             * By default, the audience is set to the address of the OpenId Connect Provider's token endpoint.
             */
            @ConfigItem
            public Optional<String> audience = Optional.empty();

            /**
             * The key identifier of the signing key added as a JWT `kid` header.
             */
            @ConfigItem
            public Optional<String> tokenKeyId = Optional.empty();

            /**
             * The issuer of the signing key added as a JWT `iss` claim. The default value is the client id.
             */
            @ConfigItem
            public Optional<String> issuer = Optional.empty();

            /**
             * Subject of the signing key added as a JWT `sub` claim The default value is the client id.
             */
            @ConfigItem
            public Optional<String> subject = Optional.empty();

            /**
             * Additional claims.
             */
            @ConfigItem
            public Map<String, String> claims = new HashMap<>();

            /**
             * The signature algorithm used for the {@link #keyFile} property.
             * Supported values: `RS256` (default), `RS384`, `RS512`, `PS256`, `PS384`, `PS512`, `ES256`, `ES384`, `ES512`,
             * `HS256`, `HS384`, `HS512`.
             */
            @ConfigItem
            public Optional<String> signatureAlgorithm = Optional.empty();

            /**
             * The JWT lifespan in seconds. This value is added to the time at which the JWT was issued to calculate the
             * expiration time.
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

            public Optional<String> getAudience() {
                return audience;
            }

            public void setAudience(String audience) {
                this.audience = Optional.of(audience);
            }

            public Optional<String> getKeyFile() {
                return keyFile;
            }

            public void setKeyFile(String keyFile) {
                this.keyFile = Optional.of(keyFile);
            }

            public Map<String, String> getClaims() {
                return claims;
            }

            public void setClaims(Map<String, String> claims) {
                this.claims = claims;
            }

        }

        /**
         * CredentialsProvider, which provides a client secret.
         */
        @ConfigGroup
        public static class Provider {

            /**
             * The CredentialsProvider name, which should only be set if more than one CredentialsProvider is
             * registered
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
             * All certificates are trusted and hostname verification is disabled.
             */
            NONE
        }

        /**
         * Certificate validation and hostname verification, which can be one of the following {@link Verification}
         * values.
         * Default is `required`.
         */
        @ConfigItem
        public Optional<Verification> verification = Optional.empty();

        /**
         * An optional keystore that holds the certificate information instead of specifying separate files.
         */
        @ConfigItem
        public Optional<Path> keyStoreFile = Optional.empty();

        /**
         * The type of the keystore file. If not given, the type is automatically detected based on the file name.
         */
        @ConfigItem
        public Optional<String> keyStoreFileType = Optional.empty();

        /**
         * The provider of the keystore file. If not given, the provider is automatically detected based on the
         * keystore file type.
         */
        @ConfigItem
        public Optional<String> keyStoreProvider;

        /**
         * The password of the keystore file. If not given, the default value, `password`, is used.
         */
        @ConfigItem
        public Optional<String> keyStorePassword;

        /**
         * The alias of a specific key in the keystore.
         * When SNI is disabled, if the keystore contains multiple
         * keys and no alias is specified, the behavior is undefined.
         */
        @ConfigItem
        public Optional<String> keyStoreKeyAlias = Optional.empty();

        /**
         * The password of the key, if it is different from the {@link #keyStorePassword}.
         */
        @ConfigItem
        public Optional<String> keyStoreKeyPassword = Optional.empty();

        /**
         * The truststore that holds the certificate information of the certificates to trust.
         */
        @ConfigItem
        public Optional<Path> trustStoreFile = Optional.empty();

        /**
         * The password of the truststore file.
         */
        @ConfigItem
        public Optional<String> trustStorePassword = Optional.empty();

        /**
         * The alias of the truststore certificate.
         */
        @ConfigItem
        public Optional<String> trustStoreCertAlias = Optional.empty();

        /**
         * The type of the truststore file.
         * If not given, the type is automatically detected
         * based on the file name.
         */
        @ConfigItem
        public Optional<String> trustStoreFileType = Optional.empty();

        /**
         * The provider of the truststore file.
         * If not given, the provider is automatically detected
         * based on the truststore file type.
         */
        @ConfigItem
        public Optional<String> trustStoreProvider;

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

        public Optional<String> getKeyStoreProvider() {
            return keyStoreProvider;
        }

        public void setKeyStoreProvider(String keyStoreProvider) {
            this.keyStoreProvider = Optional.of(keyStoreProvider);
        }

        public Optional<String> getTrustStoreProvider() {
            return trustStoreProvider;
        }

        public void setTrustStoreProvider(String trustStoreProvider) {
            this.trustStoreProvider = Optional.of(trustStoreProvider);
        }

    }

    @ConfigGroup
    public static class Proxy {

        /**
         * The host name or IP address of the Proxy.<br/>
         * Note: If the OIDC adapter requires a Proxy to talk with the OIDC server (Provider),
         * set this value to enable the usage of a Proxy.
         */
        @ConfigItem
        public Optional<String> host = Optional.empty();

        /**
         * The port number of the Proxy. The default value is `80`.
         */
        @ConfigItem(defaultValue = "80")
        public int port = 80;

        /**
         * The username, if the Proxy needs authentication.
         */
        @ConfigItem
        public Optional<String> username = Optional.empty();

        /**
         * The password, if the Proxy needs authentication.
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

    public Optional<String> getRevokePath() {
        return revokePath;
    }

    public void setRevokePath(String revokePath) {
        this.revokePath = Optional.of(revokePath);
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

    public Optional<Boolean> isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public void setDiscoveryEnabled(boolean enabled) {
        this.discoveryEnabled = Optional.of(enabled);
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
