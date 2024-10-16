package io.quarkus.oidc.common.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class OidcClientCommonConfig extends OidcCommonConfig {
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
     * The client name of the application. It is meant to represent a human readable description of the application which you
     * may provide when an application (client) is registered in an OpenId Connect provider's dashboard.
     * For example, you can set this property to have more informative log messages which record an activity of the given
     * client.
     */
    @ConfigItem
    public Optional<String> clientName = Optional.empty();

    /**
     * Credentials the OIDC adapter uses to authenticate to the OIDC server.
     */
    @ConfigItem
    public Credentials credentials = new Credentials();

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
                 * client id and secret are submitted as HTTP query parameters. This option is only supported by the OIDC
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
         * JWT Bearer client authentication is also supported.
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        @ConfigGroup
        public static class Jwt {

            public static enum Source {
                // JWT token is generated by the OIDC provider client to support
                // `client_secret_jwt` and `private_key_jwt` authentication methods
                CLIENT,
                // JWT bearer token as used as a client assertion: https://www.rfc-editor.org/rfc/rfc7523#section-2.2
                // This option is only supported by the OIDC client extension.
                BEARER
            }

            /**
             * JWT token source: OIDC provider client or an existing JWT bearer token.
             */
            @ConfigItem(defaultValue = "client")
            public Source source = Source.CLIENT;

            /**
             * If provided, indicates that JWT is signed using a secret key.
             * It is mutually exclusive with {@link #key}, {@link #keyFile} and {@link #keyStore} properties.
             */
            @ConfigItem
            public Optional<String> secret = Optional.empty();

            /**
             * If provided, indicates that JWT is signed using a secret key provided by Secret CredentialsProvider.
             */
            @ConfigItem
            public Provider secretProvider = new Provider();

            /**
             * String representation of a private key. If provided, indicates that JWT is signed using a private key in PEM or
             * JWK format.
             * It is mutually exclusive with {@link #secret}, {@link #keyFile} and {@link #keyStore} properties.
             * You can use the {@link #signatureAlgorithm} property to override the default key algorithm, `RS256`.
             */
            @ConfigItem
            public Optional<String> key = Optional.empty();

            /**
             * If provided, indicates that JWT is signed using a private key in PEM or JWK format.
             * It is mutually exclusive with {@link #secret}, {@link #key} and {@link #keyStore} properties.
             * You can use the {@link #signatureAlgorithm} property to override the default key algorithm, `RS256`.
             */
            @ConfigItem
            public Optional<String> keyFile = Optional.empty();

            /**
             * If provided, indicates that JWT is signed using a private key from a keystore.
             * It is mutually exclusive with {@link #secret}, {@link #key} and {@link #keyFile} properties.
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
            @ConfigDocMapKey("claim-name")
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

            /**
             * If true then the client authentication token is a JWT bearer grant assertion. Instead of producing
             * 'client_assertion'
             * and 'client_assertion_type' form properties, only 'assertion' is produced.
             * This option is only supported by the OIDC client extension.
             */
            @ConfigItem(defaultValue = "false")
            public boolean assertion = false;

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

            public Optional<String> getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = Optional.of(key);
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

            public Source getSource() {
                return source;
            }

            public void setSource(Source source) {
                this.source = source;
            }

            public boolean isAssertion() {
                return assertion;
            }

            public void setAssertion(boolean assertion) {
                this.assertion = assertion;
            }

        }

        /**
         * CredentialsProvider, which provides a client secret.
         */
        @ConfigGroup
        public static class Provider {

            /**
             * The CredentialsProvider bean name, which should only be set if more than one CredentialsProvider is
             * registered
             */
            @ConfigItem
            public Optional<String> name = Optional.empty();

            /**
             * The CredentialsProvider keyring name.
             * The keyring name is only required when the CredentialsProvider being
             * used requires the keyring name to look up the secret, which is often the case when a CredentialsProvider is
             * shared by multiple extensions to retrieve credentials from a more dynamic source like a vault instance or secret
             * manager
             */
            @ConfigItem
            public Optional<String> keyringName = Optional.empty();

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

            public Optional<String> getKeyringName() {
                return keyringName;
            }

            public void setKeyringName(String keyringName) {
                this.keyringName = Optional.of(keyringName);
            }

            public Optional<String> getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = Optional.of(key);
            }
        }
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

    public Optional<String> getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = Optional.of(clientName);
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
}
