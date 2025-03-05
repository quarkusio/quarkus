package io.quarkus.oidc.common.runtime.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface OidcClientCommonConfig extends OidcCommonConfig {
    /**
     * The OIDC token endpoint that issues access and refresh tokens;
     * specified as a relative path or absolute URL.
     * Set if {@link #discoveryEnabled} is `false` or a discovered token endpoint path must be customized.
     */
    Optional<String> tokenPath();

    /**
     * The relative path or absolute URL of the OIDC token revocation endpoint.
     */
    Optional<String> revokePath();

    /**
     * The client id of the application. Each application has a client id that is used to identify the application.
     * Setting the client id is not required if {@link #applicationType} is `service` and no token introspection is required.
     */
    Optional<String> clientId();

    /**
     * The client name of the application. It is meant to represent a human readable description of the application which you
     * may provide when an application (client) is registered in an OpenId Connect provider's dashboard.
     * For example, you can set this property to have more informative log messages which record an activity of the given
     * client.
     */
    Optional<String> clientName();

    /**
     * Different authentication options for OIDC client to access OIDC token and other secured endpoints.
     */
    @ConfigDocSection
    Credentials credentials();

    /**
     * Credentials used by OIDC client to authenticate to OIDC token and other secured endpoints.
     */
    interface Credentials {

        /**
         * The client secret used by the `client_secret_basic` authentication method.
         * Must be set unless a secret is set in {@link #clientSecret} or {@link #jwt} client authentication is required.
         * You can use `client-secret.value` instead, but both properties are mutually exclusive.
         */
        Optional<String> secret();

        /**
         * The client secret used by the `client_secret_basic` (default), `client_secret_post`, or `client_secret_jwt`
         * authentication methods.
         * Note that a `secret.value` property can be used instead to support the `client_secret_basic` method
         * but both properties are mutually exclusive.
         */
        Secret clientSecret();

        /**
         * Client JSON Web Token (JWT) authentication methods
         */
        Jwt jwt();

        /**
         * Supports the client authentication methods that involve sending a client secret.
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        interface Secret {

            enum Method {
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
            Optional<String> value();

            /**
             * The Secret CredentialsProvider.
             */
            Provider provider();

            /**
             * The authentication method.
             * If the `clientSecret.value` secret is set, this method is `basic` by default.
             */
            Optional<Method> method();

        }

        /**
         * Supports the client authentication `client_secret_jwt` and `private_key_jwt` methods, which involves sending a JWT
         * token assertion signed with a client secret or private key.
         * JWT Bearer client authentication is also supported.
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        interface Jwt {

            enum Source {
                /**
                 * JWT token is generated by the OIDC provider client to support
                 * `client_secret_jwt` and `private_key_jwt` authentication methods.
                 */
                CLIENT,
                /**
                 * JWT bearer token is used as a client assertion: https://www.rfc-editor.org/rfc/rfc7523#section-2.2.
                 */
                BEARER
            }

            /**
             * JWT token source: OIDC provider client or an existing JWT bearer token.
             */
            @WithDefault("client")
            Source source();

            /**
             * Path to a file with a JWT bearer token that should be used as a client assertion.
             * This path can only be set when JWT source ({@link #source()}) is set to {@link Source#BEARER}.
             */
            Optional<Path> tokenPath();

            /**
             * If provided, indicates that JWT is signed using a secret key.
             * It is mutually exclusive with {@link #key}, {@link #keyFile} and {@link #keyStore} properties.
             */
            Optional<String> secret();

            /**
             * If provided, indicates that JWT is signed using a secret key provided by Secret CredentialsProvider.
             */
            Provider secretProvider();

            /**
             * String representation of a private key. If provided, indicates that JWT is signed using a private key in PEM or
             * JWK format.
             * It is mutually exclusive with {@link #secret}, {@link #keyFile} and {@link #keyStore} properties.
             * You can use the {@link #signatureAlgorithm} property to override the default key algorithm, `RS256`.
             */
            Optional<String> key();

            /**
             * If provided, indicates that JWT is signed using a private key in PEM or JWK format.
             * It is mutually exclusive with {@link #secret}, {@link #key} and {@link #keyStore} properties.
             * You can use the {@link #signatureAlgorithm} property to override the default key algorithm, `RS256`.
             */
            Optional<String> keyFile();

            /**
             * If provided, indicates that JWT is signed using a private key from a keystore.
             * It is mutually exclusive with {@link #secret}, {@link #key} and {@link #keyFile} properties.
             */
            Optional<String> keyStoreFile();

            /**
             * A parameter to specify the password of the keystore file.
             */
            Optional<String> keyStorePassword();

            /**
             * The private key id or alias.
             */
            Optional<String> keyId();

            /**
             * The private key password.
             */
            Optional<String> keyPassword();

            /**
             * The JWT audience (`aud`) claim value.
             * By default, the audience is set to the address of the OpenId Connect Provider's token endpoint.
             */
            Optional<String> audience();

            /**
             * The key identifier of the signing key added as a JWT `kid` header.
             */
            Optional<String> tokenKeyId();

            /**
             * The issuer of the signing key added as a JWT `iss` claim. The default value is the client id.
             */
            Optional<String> issuer();

            /**
             * Subject of the signing key added as a JWT `sub` claim The default value is the client id.
             */
            Optional<String> subject();

            /**
             * Additional claims.
             */
            @ConfigDocMapKey("claim-name")
            Map<String, String> claims();

            /**
             * The signature algorithm used for the {@link #keyFile} property.
             * Supported values: `RS256` (default), `RS384`, `RS512`, `PS256`, `PS384`, `PS512`, `ES256`, `ES384`, `ES512`,
             * `HS256`, `HS384`, `HS512`.
             */
            Optional<String> signatureAlgorithm();

            /**
             * The JWT lifespan in seconds. This value is added to the time at which the JWT was issued to calculate the
             * expiration time.
             */
            @WithDefault("10")
            int lifespan();

            /**
             * If true then the client authentication token is a JWT bearer grant assertion. Instead of producing
             * 'client_assertion'
             * and 'client_assertion_type' form properties, only 'assertion' is produced.
             * This option is only supported by the OIDC client extension.
             */
            @WithDefault("false")
            boolean assertion();

        }

        /**
         * CredentialsProvider, which provides a client secret.
         */
        interface Provider {

            /**
             * The CredentialsProvider bean name, which should only be set if more than one CredentialsProvider is
             * registered
             */
            Optional<String> name();

            /**
             * The CredentialsProvider keyring name.
             * The keyring name is only required when the CredentialsProvider being
             * used requires the keyring name to look up the secret, which is often the case when a CredentialsProvider is
             * shared by multiple extensions to retrieve credentials from a more dynamic source like a vault instance or secret
             * manager
             */
            Optional<String> keyringName();

            /**
             * The CredentialsProvider client secret key
             */
            Optional<String> key();

        }
    }

}
