package io.quarkus.oidc.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.oidc.JavaScriptRequestChecker;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TokenCustomizer;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;
import io.quarkus.oidc.common.runtime.config.OidcCommonConfig;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

public interface OidcTenantConfig extends OidcClientCommonConfig {

    /**
     * A unique tenant identifier. It can be set by {@code TenantConfigResolver} providers, which
     * resolve the tenant configuration dynamically.
     */
    Optional<String> tenantId();

    /**
     * If this tenant configuration is enabled.
     *
     * The default tenant is disabled if it is not configured but
     * a {@link TenantConfigResolver} that resolves tenant configurations is registered,
     * or named tenants are configured.
     * In this case, you do not need to disable the default tenant.
     */
    @WithDefault("true")
    boolean tenantEnabled();

    /**
     * The application type, which can be one of the following {@link ApplicationType} values.
     */
    @ConfigDocDefault("service")
    Optional<ApplicationType> applicationType();

    /**
     * The relative path or absolute URL of the OpenID Connect (OIDC) authorization endpoint, which authenticates
     * users.
     * You must set this property for `web-app` applications if OIDC discovery is disabled.
     * This property is ignored if OIDC discovery is enabled.
     */
    Optional<String> authorizationPath();

    /**
     * The relative path or absolute URL of the OIDC UserInfo endpoint.
     * You must set this property for `web-app` applications if OIDC discovery is disabled
     * and the `authentication.user-info-required` property is enabled.
     * This property is ignored if OIDC discovery is enabled.
     */
    Optional<String> userInfoPath();

    /**
     * Relative path or absolute URL of the OIDC RFC7662 introspection endpoint which can introspect both opaque and
     * JSON Web Token (JWT) tokens.
     * This property must be set if OIDC discovery is disabled and 1) the opaque bearer access tokens must be verified
     * or 2) JWT tokens must be verified while the cached JWK verification set with no matching JWK is being refreshed.
     * This property is ignored if the discovery is enabled.
     */
    Optional<String> introspectionPath();

    /**
     * Relative path or absolute URL of the OIDC JSON Web Key Set (JWKS) endpoint which returns a JSON Web Key
     * Verification Set.
     * This property should be set if OIDC discovery is disabled and the local JWT verification is required.
     * This property is ignored if the discovery is enabled.
     */
    Optional<String> jwksPath();

    /**
     * Relative path or absolute URL of the OIDC end_session_endpoint.
     * This property must be set if OIDC discovery is disabled and RP Initiated Logout support for the `web-app` applications is
     * required.
     * This property is ignored if the discovery is enabled.
     */
    Optional<String> endSessionPath();

    /**
     * The paths which must be secured by this tenant. Tenant with the most specific path wins.
     * Please see the xref:security-openid-connect-multitenancy.adoc#configure-tenant-paths[Configure tenant paths]
     * section of the OIDC multitenancy guide for explanation of allowed path patterns.
     *
     * @asciidoclet
     */
    Optional<List<String>> tenantPaths();

    /**
     * The public key for the local JWT token verification.
     * OIDC server connection is not created when this property is set.
     */
    Optional<String> publicKey();

    /**
     * Optional introspection endpoint-specific basic authentication configuration.
     * It must be configured only if the introspection is required
     * but OpenId Connect Provider does not support the OIDC client authentication configured with
     * {@link OidcCommonConfig#credentials} for its introspection endpoint.
     */
    @ConfigDocSection
    IntrospectionCredentials introspectionCredentials();

    /**
     * Optional introspection endpoint-specific authentication configuration.
     */
    interface IntrospectionCredentials {
        /**
         * Name
         */
        Optional<String> name();

        /**
         * Secret
         */
        Optional<String> secret();

        /**
         * Include OpenId Connect Client ID configured with `quarkus.oidc.client-id`.
         */
        @WithDefault("true")
        boolean includeClientId();

    }

    /**
     * Configuration to find and parse custom claims which contain roles.
     */
    @ConfigDocSection
    Roles roles();

    /**
     * Configuration to provide protected resource metadata.
     */
    @ConfigDocSection
    ResourceMetadata resourceMetadata();

    /**
     * Protected resource metadata.
     */
    interface ResourceMetadata {
        /**
         * If the resource metadata can be provided.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Protected resource identifier.
         */
        Optional<String> resource();

        /**
         * Force a protected resource identifier HTTPS scheme.
         * This property is ignored if {@link #resource() is an absolute URL}
         */
        @WithDefault("true")
        boolean forceHttpsScheme();
    }

    /**
     * Configuration to customize validation of token claims.
     */
    @ConfigDocSection
    Token token();

    /**
     * RP-initiated, back-channel and front-channel logout configuration.
     */
    @ConfigDocSection
    Logout logout();

    /**
     * Configuration of the certificate chain which can be used to verify tokens.
     * If the certificate chain truststore is configured, the tokens can be verified using the certificate
     * chain inlined in the Base64-encoded format as an `x5c` header in the token itself.
     * <p/>
     * The certificate chain inlined in the token is verified.
     * Signature of every certificate in the chain but the root certificate is verified by the next certificate in the chain.
     * Thumbprint of the root certificate in the chain must match a thumbprint of one of the certificates in the truststore.
     * <p/>
     * Additionally, a direct trust in the leaf chain certificate which will be used to verify the token signature must
     * be established.
     * By default, the leaf certificate's thumbprint must match a thumbprint of one of the certificates in the truststore.
     * If the truststore does not have the leaf certificate imported, then the leaf certificate must be identified by its Common
     * Name.
     */
    @ConfigDocSection
    CertificateChain certificateChain();

    /**
     * Configuration of the certificate chain which can be used to verify tokens.
     */
    interface CertificateChain {
        /**
         * Common name of the leaf certificate. It must be set if the {@link #trustStoreFile} does not have
         * this certificate imported.
         *
         */
        Optional<String> leafCertificateName();

        /**
         * Truststore file which keeps thumbprints of the trusted certificates.
         */
        Optional<Path> trustStoreFile();

        /**
         * A parameter to specify the password of the truststore file if it is configured with {@link #trustStoreFile}.
         */
        Optional<String> trustStorePassword();

        /**
         * A parameter to specify the alias of the truststore certificate.
         */
        Optional<String> trustStoreCertAlias();

        /**
         * An optional parameter to specify type of the truststore file. If not given, the type is automatically
         * detected
         * based on the file name.
         */
        Optional<String> trustStoreFileType();

    }

    /**
     * Configuration for managing an authorization code flow.
     */
    @ConfigDocSection
    Authentication authentication();

    /**
     * Configuration to complete an authorization code flow grant.
     */
    @ConfigDocSection
    CodeGrant codeGrant();

    /**
     * Default token state manager configuration
     */
    @ConfigDocSection
    TokenStateManager tokenStateManager();

    /**
     * Allow caching the token introspection data.
     * Note enabling this property does not enable the cache itself but only permits to cache the token introspection
     * for a given tenant. If the default token cache can be used, see {@link OidcConfig.TokenCache} to enable
     * it.
     */
    @WithDefault("true")
    boolean allowTokenIntrospectionCache();

    /**
     * Allow caching the user info data.
     * Note enabling this property does not enable the cache itself but only permits to cache the user info data
     * for a given tenant. If the default token cache can be used, see {@link OidcConfig.TokenCache} to enable
     * it.
     */
    @WithDefault("true")
    boolean allowUserInfoCache();

    /**
     * Allow inlining UserInfo in IdToken instead of caching it in the token cache.
     * This property is only checked when an internal IdToken is generated when OAuth2 providers do not return IdToken.
     * Inlining UserInfo in the generated IdToken allows to store it in the session cookie and avoids introducing a cached
     * state.
     * <p>
     * Inlining UserInfo in the generated IdToken is enabled if the session cookie is encrypted
     * and the UserInfo cache is not enabled or caching UserInfo is disabled for the current tenant
     * with the {@link #allowUserInfoCache} property set to `false`.
     */
    Optional<Boolean> cacheUserInfoInIdtoken();

    interface Logout {

        /**
         * The relative path of the logout endpoint at the application. If provided, the application is able to
         * initiate the
         * logout through this endpoint in conformance with the OpenID Connect RP-Initiated Logout specification.
         */
        Optional<String> path();

        /**
         * Relative path of the application endpoint where the user should be redirected to after logging out from the
         * OpenID
         * Connect Provider.
         * This endpoint URI must be properly registered at the OpenID Connect Provider as a valid redirect URI.
         */
        Optional<String> postLogoutPath();

        /**
         * Name of the post logout URI parameter which is added as a query parameter to the logout redirect URI.
         */
        @WithDefault(OidcConstants.POST_LOGOUT_REDIRECT_URI)
        String postLogoutUriParam();

        /**
         * Additional properties which is added as the query parameters to the logout redirect URI.
         */
        @ConfigDocMapKey("query-parameter-name")
        Map<String, String> extraParams();

        /**
         * Back-Channel Logout configuration
         */
        Backchannel backchannel();

        /**
         * Front-Channel Logout configuration
         */
        Frontchannel frontchannel();

        enum ClearSiteData {
            /**
             * Clear cache
             */
            CACHE("cache"),

            /**
             * Clear client hints.
             */
            CLIENT_HINTS("clientHints"),

            /**
             * Clear cookies.
             */
            COOKIES("cookies"),

            /**
             * Clear execution contexts
             */
            EXECUTION_CONTEXTS("executionContexts"),

            /**
             * Clear storage
             */
            STORAGE("storage"),

            /**
             * Clear all types of data
             */
            WILDCARD("*");

            private String directive;

            private ClearSiteData(String directive) {
                this.directive = directive;
            }

            public String directive() {
                return "\"" + directive + "\"";
            }
        }

        /**
         * Clear-Site-Data header directives
         */
        Optional<Set<ClearSiteData>> clearSiteData();

        enum LogoutMode {
            /**
             * Logout parameters are encoded in the query string
             */
            QUERY,

            /**
             * Logout parameters are encoded as HTML form values that are auto-submitted in the browser
             * and transmitted by the HTTP POST method using the application/x-www-form-urlencoded content type
             */
            FORM_POST
        }

        /**
         * Logout mode
         */
        @WithDefault("query")
        LogoutMode logoutMode();
    }

    interface Backchannel {
        /**
         * The relative path of the Back-Channel Logout endpoint at the application.
         * It must start with the forward slash '/', for example, '/back-channel-logout'.
         * This value is always resolved relative to 'quarkus.http.root-path'.
         */
        Optional<String> path();

        /**
         * Maximum number of logout tokens that can be cached before they are matched against ID tokens stored in session
         * cookies.
         */
        @WithDefault("10")
        int tokenCacheSize();

        /**
         * Number of minutes a logout token can be cached for.
         */
        @WithDefault("10M")
        Duration tokenCacheTimeToLive();

        /**
         * Token cache timer interval.
         * If this property is set, a timer checks and removes the stale entries periodically.
         */
        Optional<Duration> cleanUpTimerInterval();

        /**
         * Logout token claim whose value is used as a key for caching the tokens.
         * Only `sub` (subject) and `sid` (session id) claims can be used as keys.
         * Set it to `sid` only if ID tokens issued by the OIDC provider have no `sub` but have `sid` claim.
         */
        @WithDefault("sub")
        String logoutTokenKey();
    }

    /**
     * How JsonWebKey verification key set should be acquired and managed.
     */
    @ConfigDocSection
    Jwks jwks();

    interface Jwks {
        /**
         * If JWK verification keys should be fetched at the moment a connection to the OIDC provider
         * is initialized.
         * <p/>
         * Disabling this property delays the key acquisition until the moment the current token
         * has to be verified. Typically it can only be necessary if the token or other telated request properties
         * provide an additional context which is required to resolve the keys correctly.
         */
        @WithDefault("true")
        boolean resolveEarly();

        /**
         * Maximum number of JWK keys that can be cached.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        @WithDefault("10")
        int cacheSize();

        /**
         * Number of minutes a JWK key can be cached for.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        @WithDefault("10M")
        Duration cacheTimeToLive();

        /**
         * Cache timer interval.
         * If this property is set, a timer checks and removes the stale entries periodically.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        Optional<Duration> cleanUpTimerInterval();

        /**
         * In case there is no key identifier ('kid') or certificate thumbprints ('x5t', 'x5t#S256') specified in the JOSE
         * header and no key could be determined, check all available keys matching the token algorithm ('alg') header value.
         */
        @WithDefault("false")
        boolean tryAll();

    }

    interface Frontchannel {
        /**
         * The relative path of the Front-Channel Logout endpoint at the application.
         */
        Optional<String> path();
    }

    /**
     * Default Authorization Code token state manager configuration
     */
    interface TokenStateManager {

        enum Strategy {
            /**
             * Keep ID, access and refresh tokens.
             */
            KEEP_ALL_TOKENS,

            /**
             * Keep ID token only
             */
            ID_TOKEN,

            /**
             * Keep ID and refresh tokens only
             */
            ID_REFRESH_TOKENS
        }

        /**
         * Default TokenStateManager strategy.
         */
        @WithDefault("keep_all_tokens")
        Strategy strategy();

        /**
         * Default TokenStateManager keeps all tokens (ID, access and refresh)
         * returned in the authorization code grant response in a single session cookie by default.
         *
         * Enable this property to minimize a session cookie size
         */
        @WithDefault("false")
        boolean splitTokens();

        /**
         * Mandates that the Default TokenStateManager encrypt the session cookie that stores the tokens.
         */
        @WithDefault("true")
        boolean encryptionRequired();

        /**
         * The secret used by the Default TokenStateManager to encrypt the session cookie
         * storing the tokens when {@link #encryptionRequired} property is enabled.
         * <p>
         * If this secret is not set, the client secret configured with
         * either `quarkus.oidc.credentials.secret` or `quarkus.oidc.credentials.client-secret.value` is checked.
         * Finally, `quarkus.oidc.credentials.jwt.secret` which can be used for `client_jwt_secret` authentication is
         * checked.
         * The secret is auto-generated every time an application starts if it remains uninitialized after checking all of these
         * properties.
         * Generated secret can not decrypt the session cookie encrypted before the restart, therefore a user re-authentication
         * will be required.
         * <p>
         * The length of the secret used to encrypt the tokens should be at least 32 characters long.
         * A warning is logged if the secret length is less than 16 characters.
         */
        Optional<String> encryptionSecret();

        /**
         * Supported session cookie key encryption algorithms
         */
        enum EncryptionAlgorithm {
            /**
             * Content encryption key will be generated and encrypted using the A256GCMKW algorithm and the configured
             * encryption secret.
             * The generated content encryption key will be used to encrypt the session cookie content.
             */
            A256GCMKW,
            /**
             * The configured key encryption secret will be used as the content encryption key to encrypt the session cookie
             * content.
             * Using the direct encryption avoids a content encryption key generation step and
             * will make the encrypted session cookie sequence slightly shorter.
             * <p/>
             * Avoid using the direct encryption if the encryption secret is less than 32 characters long.
             */
            DIR;
        }

        /**
         * Session cookie key encryption algorithm
         */
        @WithDefault("A256GCMKW")
        EncryptionAlgorithm encryptionAlgorithm();
    }

    interface Roles {

        /**
         * A list of paths to claims containing an array of groups.
         * Each path starts from the top level JWT JSON object
         * and can contain multiple segments.
         * Each segment represents a JSON object name only; for example: "realm/groups".
         * Use double quotes with the namespace-qualified claim names.
         * This property can be used if a token has no `groups` claim but has the groups set in one or more different claims.
         */
        Optional<List<String>> roleClaimPath();

        /**
         * The separator for splitting strings that contain multiple group values.
         * It is only used if the "role-claim-path" property points to one or more custom claims whose values are strings.
         * A single space is used by default because the standard `scope` claim can contain a space-separated sequence.
         */
        Optional<String> roleClaimSeparator();

        /**
         * Source of the principal roles.
         */
        Optional<Source> source();

        // Source of the principal roles
        enum Source {
            /**
             * ID Token - the default value for the `web-app` applications.
             */
            idtoken,

            /**
             * Access Token - the default value for the `service` applications;
             * can also be used as the source of roles for the `web-app` applications.
             */
            accesstoken,

            /**
             * User Info
             */
            userinfo
        }
    }

    /**
     * Defines the authorization request properties when authenticating
     * users using the Authorization Code Grant Type.
     */
    interface Authentication {

        /**
         * SameSite attribute values for the session cookie.
         */
        enum CookieSameSite {
            STRICT,
            LAX,
            NONE
        }

        /**
         * Authorization code flow response mode
         */
        enum ResponseMode {
            /**
             * Authorization response parameters are encoded in the query string added to the `redirect_uri`
             */
            QUERY,

            /**
             * Authorization response parameters are encoded as HTML form values that are auto-submitted in the browser
             * and transmitted by the HTTP POST method using the application/x-www-form-urlencoded content type
             */
            FORM_POST
        }

        /**
         * Authorization code flow response mode.
         */
        @ConfigDocDefault("query")
        Optional<ResponseMode> responseMode();

        /**
         * The relative path for calculating a `redirect_uri` query parameter.
         * It has to start from a forward slash and is appended to the request URI's host and port.
         * For example, if the current request URI is `https://localhost:8080/service`, a `redirect_uri` parameter
         * is set to `https://localhost:8080/` if this property is set to `/` and be the same as the request URI
         * if this property has not been configured.
         * Note the original request URI is restored after the user has authenticated if `restorePathAfterRedirect` is set
         * to `true`.
         */
        Optional<String> redirectPath();

        /**
         * If this property is set to `true`, the original request URI which was used before
         * the authentication is restored after the user has been redirected back to the application.
         *
         * Note if `redirectPath` property is not set, the original request URI is restored even if this property is
         * disabled.
         */
        @WithDefault("false")
        boolean restorePathAfterRedirect();

        /**
         * Remove the query parameters such as `code` and `state` set by the OIDC server on the redirect URI
         * after the user has authenticated by redirecting a user to the same URI but without the query parameters.
         */
        @WithDefault("true")
        boolean removeRedirectParameters();

        /**
         * Relative path to the public endpoint which processes the error response from the OIDC authorization
         * endpoint.
         * If the user authentication has failed, the OIDC provider returns an `error` and an optional
         * `error_description`
         * parameters, instead of the expected authorization `code`.
         *
         * If this property is set, the user is redirected to the endpoint which can return a user-friendly
         * error description page. It has to start from a forward slash and is appended to the request URI's host and port.
         * For example, if it is set as `/error` and the current request URI is
         * `https://localhost:8080/callback?error=invalid_scope`,
         * a redirect is made to `https://localhost:8080/error?error=invalid_scope`.
         *
         * If this property is not set, HTTP 401 status is returned in case of the user authentication failure.
         */
        Optional<String> errorPath();

        /**
         * Relative path to the public endpoint which an authenticated user is redirected to when the session has expired.
         * <p>
         * When the OIDC session has expired and the session can not be refreshed, a user is redirected
         * to the OIDC provider to re-authenticate. The user experience may not be ideal in this case
         * as it may not be obvious to the authenticated user why an authentication challenge is returned.
         * <p>
         * Set this property if you would like the user whose session has expired be redirected to a public application specific
         * page
         * instead, which can inform that the session has expired and advise the user to re-authenticated by following
         * a link to the secured initial entry page.
         */
        Optional<String> sessionExpiredPath();

        /**
         * Both ID and access tokens are fetched from the OIDC provider as part of the authorization code flow.
         * <p>
         * ID token is always verified on every user request as the primary token which is used
         * to represent the principal and extract the roles.
         * <p>
         * Authorization code flow access token is meant to be propagated to downstream services
         * and is not verified by default unless `quarkus.oidc.roles.source` property is set to `accesstoken`
         * which means the authorization decision is based on the roles extracted from the access token.
         * <p>
         * Authorization code flow access token verification is also enabled if this token is injected as JsonWebToken.
         * Set this property to `false` if it is not required.
         * <p>
         * Bearer access token is always verified.
         */
        @ConfigDocDefault("true when access token is injected as the JsonWebToken bean, false otherwise")
        @WithDefault("false")
        boolean verifyAccessToken();

        /**
         * Force `https` as the `redirect_uri` parameter scheme when running behind an SSL/TLS terminating reverse
         * proxy.
         * This property, if enabled, also affects the logout `post_logout_redirect_uri` and the local redirect requests.
         */
        @ConfigDocDefault("false")
        Optional<Boolean> forceRedirectHttpsScheme();

        /**
         * List of scopes
         */
        Optional<List<String>> scopes();

        /**
         * The separator which is used when more than one scope is configured.
         * A single space is used by default.
         */
        Optional<String> scopeSeparator();

        /**
         * Require that ID token includes a `nonce` claim which must match `nonce` authentication request query parameter.
         * Enabling this property can help mitigate replay attacks.
         * Do not enable this property if your OpenId Connect provider does not support setting `nonce` in ID token
         * or if you work with OAuth2 provider such as `GitHub` which does not issue ID tokens.
         */
        @WithDefault("false")
        boolean nonceRequired();

        /**
         * Add the `openid` scope automatically to the list of scopes. This is required for OpenId Connect providers,
         * but does not work for OAuth2 providers such as Twitter OAuth2, which do not accept this scope and throw errors.
         */
        @ConfigDocDefault("true")
        Optional<Boolean> addOpenidScope();

        /**
         * Additional properties added as query parameters to the authentication redirect URI.
         */
        @ConfigDocMapKey("parameter-name")
        Map<String, String> extraParams();

        /**
         * Request URL query parameters which, if present, are added to the authentication redirect URI.
         */
        Optional<List<@WithConverter(TrimmedStringConverter.class) String>> forwardParams();

        /**
         * If enabled the state, session, and post logout cookies have their `secure` parameter set to `true`
         * when HTTP is used. It might be necessary when running behind an SSL/TLS terminating reverse proxy.
         * The cookies are always secure if HTTPS is used, even if this property is set to false.
         */
        @WithDefault("false")
        boolean cookieForceSecure();

        /**
         * Cookie name suffix.
         * For example, a session cookie name for the default OIDC tenant is `q_session` but can be changed to `q_session_test`
         * if this property is set to `test`.
         */
        Optional<String> cookieSuffix();

        /**
         * Cookie path parameter value which, if set, is used to set a path parameter for the session, state and post
         * logout cookies.
         * The `cookie-path-header` property, if set, is checked first.
         */
        @WithDefault("/")
        String cookiePath();

        /**
         * Cookie path header parameter value which, if set, identifies the incoming HTTP header
         * whose value is used to set a path parameter for the session, state and post logout cookies.
         * If the header is missing, the `cookie-path` property is checked.
         */
        Optional<String> cookiePathHeader();

        /**
         * Cookie domain parameter value which, if set, is used for the session, state and post logout cookies.
         */
        Optional<String> cookieDomain();

        /**
         * SameSite attribute for the session cookie.
         */
        @WithDefault("lax")
        CookieSameSite cookieSameSite();

        /**
         * If a state cookie is present, a `state` query parameter must also be present and both the state
         * cookie name suffix and state cookie value must match the value of the `state` query parameter when
         * the redirect path matches the current path.
         * However, if multiple authentications are attempted from the same browser, for example, from the different
         * browser tabs, then the currently available state cookie might represent the authentication flow
         * initiated from another tab and not related to the current request.
         * Disable this property to permit only a single authorization code flow in the same browser.
         *
         */
        @WithDefault("true")
        boolean allowMultipleCodeFlows();

        /**
         * Fail with the HTTP 401 error if the state cookie is present but no state query parameter is present.
         * <p/>
         * When either multiple authentications are disabled or the redirect URL
         * matches the original request URL, the stale state cookie might remain in the browser cache from
         * the earlier failed redirect to an OpenId Connect provider and be visible during the current request.
         * For example, if Single-page application (SPA) uses XHR to handle redirects to the provider
         * which does not support CORS for its authorization endpoint, the browser blocks it
         * and the state cookie created by Quarkus remains in the browser cache.
         * Quarkus reports an authentication failure when it detects such an old state cookie but find no matching state
         * query parameter.
         * <p/>
         * Reporting HTTP 401 error is usually the right thing to do in such cases, it minimizes a risk of the
         * browser redirect loop but also can identify problems in the way SPA or Quarkus application manage redirects.
         * For example, enabling {@link #javaScriptAutoRedirect} or having the provider redirect to URL configured
         * with {@link #redirectPath} might be needed to avoid such errors.
         * <p/>
         * However, setting this property to `false` might help if the above options are not suitable.
         * It causes a new authentication redirect to OpenId Connect provider. Doing so might increase the
         * risk of browser redirect loops.
         */
        @WithDefault("false")
        boolean failOnMissingStateParam();

        /**
         * Fail with the HTTP 401 error if the ID token signature can not be verified during the re-authentication only due to
         * an unresolved token key identifier (`kid`).
         * <p>
         * This property might need to be disabled when multiple tab authentications are allowed, with one of the tabs keeping
         * an expired ID token with its `kid`
         * unresolved due to the verification key set refreshed due to another tab initiating an authorization code flow. In
         * such cases, instead of failing with the HTTP 401 error,
         * redirecting the user to re-authenticate with the HTTP 302 status may provide better user experience.
         */
        @WithDefault("true")
        boolean failOnUnresolvedKid();

        /**
         * If this property is set to `true`, an OIDC UserInfo endpoint is called.
         * <p>
         * This property is enabled automatically if `quarkus.oidc.roles.source` is set to `userinfo`
         * or `quarkus.oidc.token.verify-access-token-with-user-info` is set to `true`
         * or `quarkus.oidc.authentication.id-token-required` is set to `false`,
         * the current OIDC tenant must support a UserInfo endpoint in these cases.
         * <p>
         * It is also enabled automatically if `io.quarkus.oidc.UserInfo` injection point is detected but only
         * if the current OIDC tenant supports a UserInfo endpoint.
         */
        @ConfigDocDefault("true when UserInfo bean is injected, false otherwise")
        Optional<Boolean> userInfoRequired();

        /**
         * Session age extension in minutes.
         * The user session age property is set to the value of the ID token life-span by default and
         * the user is redirected to the OIDC provider to re-authenticate once the session has expired.
         * If this property is set to a nonzero value, then the expired ID token can be refreshed before
         * the session has expired.
         * This property is ignored if the `token.refresh-expired` property has not been enabled.
         */
        @WithDefault("5M")
        Duration sessionAgeExtension();

        /**
         * State cookie age in minutes.
         * State cookie is created every time a new authorization code flow redirect starts
         * and removed when this flow is completed.
         * State cookie name is unique by default, see {@link #allowMultipleCodeFlows}.
         * Keep its age to the reasonable minimum value such as 5 minutes or less.
         */
        @WithDefault("5M")
        Duration stateCookieAge();

        /**
         * If this property is set to `true`, a normal 302 redirect response is returned
         * if the request was initiated by a JavaScript API such as XMLHttpRequest or Fetch and the current user needs to be
         * (re)authenticated, which might not be desirable for Single-page applications (SPA) since
         * it automatically following the redirect might not work given that OIDC authorization endpoints typically do not
         * support
         * CORS.
         * <p/>
         * If this property is set to `false`, a status code of `499` is returned to allow
         * SPA to handle the redirect manually if a request header identifying current request as a JavaScript request is found.
         * `X-Requested-With` request header with its value set to either `JavaScript` or `XMLHttpRequest` is expected by
         * default if
         * this property is enabled. You can register a custom {@linkplain JavaScriptRequestChecker} to do a custom JavaScript
         * request check instead.
         */
        @WithDefault("true")
        boolean javaScriptAutoRedirect();

        /**
         * Requires that ID token is available when the authorization code flow completes.
         * Disable this property only when you need to use the authorization code flow with OAuth2 providers which do not return
         * ID token - an internal IdToken is generated in such cases.
         */
        @ConfigDocDefault("true")
        Optional<Boolean> idTokenRequired();

        /**
         * Internal ID token lifespan.
         * This property is only checked when an internal IdToken is generated when OAuth2 providers do not return IdToken.
         * If this property is not configured then an access token `expires_in` property
         * in the OAuth2 authorization code flow response is used to set an internal IdToken lifespan.
         */
        Optional<Duration> internalIdTokenLifespan();

        /**
         * Requires that a Proof Key for Code Exchange (PKCE) is used.
         */
        @ConfigDocDefault("false")
        Optional<Boolean> pkceRequired();

        /**
         * Secret used to encrypt a Proof Key for Code Exchange (PKCE) code verifier in the code flow state.
         * This secret should be at least 32 characters long.
         *
         * @deprecated This field is deprecated. Use {@link #stateSecret} instead.
         *
         */
        @Deprecated(forRemoval = true)
        Optional<String> pkceSecret();

        /**
         * Secret used to encrypt Proof Key for Code Exchange (PKCE) code verifier and/or nonce in the code flow
         * state.
         * This secret should be at least 32 characters long.
         * <p/>
         * If this secret is not set, the client secret configured with
         * either `quarkus.oidc.credentials.secret` or `quarkus.oidc.credentials.client-secret.value` is checked.
         * Finally, `quarkus.oidc.credentials.jwt.secret` which can be used for `client_jwt_secret` authentication is
         * checked. A client secret is not be used as a state encryption secret if it is less than 32 characters
         * long.
         * </p>
         * The secret is auto-generated if it remains uninitialized after checking all of these properties.
         * <p/>
         * Error is reported if the secret length is less than 16 characters.
         */
        Optional<String> stateSecret();

    }

    /**
     * Authorization Code grant configuration
     */
    interface CodeGrant {

        /**
         * Additional parameters, in addition to the required `code` and `redirect-uri` parameters,
         * which must be included to complete the authorization code grant request.
         */
        @ConfigDocMapKey("parameter-name")
        Map<String, String> extraParams();

        /**
         * Custom HTTP headers which must be sent to complete the authorization code grant request.
         */
        @ConfigDocMapKey("header-name")
        Map<String, String> headers();

    }

    /**
     * Supported asymmetric signature algorithms
     */
    enum SignatureAlgorithm {
        RS256,
        RS384,
        RS512,
        PS256,
        PS384,
        PS512,
        ES256,
        ES384,
        ES512,
        EDDSA;

        private static String EDDSA_ALG = "EDDSA";
        private static String REQUIRED_EDDSA_ALG = "EdDSA";

        public String getAlgorithm() {
            String name = name();
            return EDDSA_ALG.equals(name) ? REQUIRED_EDDSA_ALG : name;
        }
    }

    interface Token {

        /**
         * The expected issuer `iss` claim value.
         * This property overrides the `issuer` property, which might be set in OpenId Connect provider's well-known
         * configuration.
         * If the `iss` claim value varies depending on the host, IP address, or tenant id of the provider, you can skip the
         * issuer verification by setting this property to `any`, but it should be done only when other options (such as
         * configuring
         * the provider to use the fixed `iss` claim value) are not possible.
         */
        Optional<String> issuer();

        /**
         * The expected audience `aud` claim value, which can be a string or an array of strings.
         *
         * Note the audience claim is verified for ID tokens by default.
         * ID token audience must be equal to the value of `quarkus.oidc.client-id` property.
         * Use this property to override the expected value if your OpenID Connect provider
         * sets a different audience claim value in ID tokens. Set it to `any` if your provider
         * does not set ID token audience` claim.
         *
         * Audience verification for access tokens is only done if this property is configured.
         */
        Optional<List<String>> audience();

        /**
         * Require that the token includes a `sub` (subject) claim which is a unique
         * and never reassigned identifier for the current user.
         * Note that if you enable this property and if UserInfo is also required,
         * both the token and UserInfo `sub` claims must be present and match each other.
         */
        @WithDefault("false")
        boolean subjectRequired();

        /**
         * A map of required claims and their expected values.
         * For example, `quarkus.oidc.token.required-claims.org_id = org_xyz` would require tokens to have the `org_id`
         * claim to be present and set to `org_xyz`. On the other hand, if it was set to `org_xyz,org_abc`,
         * the `org_id` claim would need to have both `org_xyz` and `org_abc` values.
         * Strings and arrays of strings are currently the only supported types.
         * Use {@linkplain SecurityIdentityAugmentor} to verify claims of other types or complex claims.
         */
        @ConfigDocMapKey("claim-name")
        Map<String, Set<@WithConverter(TrimmedStringConverter.class) String>> requiredClaims();

        /**
         * Expected token type
         */
        Optional<String> tokenType();

        /**
         * Life span grace period in seconds.
         * When checking token expiry, current time is allowed to be later than token expiration time by at most the configured
         * number of seconds.
         * When checking token issuance, current time is allowed to be sooner than token issue time by at most the configured
         * number of seconds.
         */
        OptionalInt lifespanGrace();

        /**
         * Token age.
         *
         * It allows for the number of seconds to be specified that must not elapse since the `iat` (issued at) time.
         * A small leeway to account for clock skew which can be configured with `quarkus.oidc.token.lifespan-grace` to verify
         * the token expiry time
         * can also be used to verify the token age property.
         *
         * Note that setting this property does not relax the requirement that Bearer and Code Flow JWT tokens
         * must have a valid (`exp`) expiry claim value. The only exception where setting this property relaxes the requirement
         * is when a logout token is sent with a back-channel logout request since the current
         * OpenId Connect Back-Channel specification does not explicitly require the logout tokens to contain an `exp` claim.
         * However, even if the current logout token is allowed to have no `exp` claim, the `exp` claim is still verified
         * if the logout token contains it.
         */
        Optional<Duration> age();

        /**
         * Require that the token includes a `iat` (issued at) claim
         *
         * Set this property to `false` if your JWT token does not contain an `iat` (issued at) claim.
         * Note that ID token is always required to have an `iat` claim and therefore this property has no impact on the ID
         * token verification process.
         */
        @WithDefault("true")
        boolean issuedAtRequired();

        /**
         * Name of the claim which contains a principal name. By default, the `upn`, `preferred_username` and `sub`
         * claims are
         * checked.
         */
        Optional<String> principalClaim();

        /**
         * Refresh expired authorization code flow ID or access tokens.
         * If this property is enabled, a refresh token request is performed if the authorization code
         * ID or access token has expired and, if successful, the local session is updated with the new set of tokens.
         * Otherwise, the local session is invalidated and the user redirected to the OpenID Provider to re-authenticate.
         * In this case, the user might not be challenged again if the OIDC provider session is still active.
         *
         * For this option be effective the `authentication.session-age-extension` property should also be set to a nonzero
         * value since the refresh token is currently kept in the user session.
         *
         * This option is valid only when the application is of type {@link ApplicationType#WEB_APP}.
         *
         * This property is enabled if `quarkus.oidc.token.refresh-token-time-skew` is configured,
         * you do not need to enable this property manually in this case.
         */
        @WithDefault("false")
        boolean refreshExpired();

        /**
         * The refresh token time skew, in seconds.
         * If this property is enabled, the configured number of seconds is added to the current time
         * when checking if the authorization code ID or access token should be refreshed.
         * If the sum is greater than the authorization code ID or access token's expiration time, a refresh is going to
         * happen.
         */
        Optional<Duration> refreshTokenTimeSkew();

        /**
         * The forced JWK set refresh interval in minutes.
         */
        @WithDefault("10M")
        Duration forcedJwkRefreshInterval();

        /**
         * Custom HTTP header that contains a bearer token.
         * This option is valid only when the application is of type {@link ApplicationType#SERVICE}.
         */
        Optional<String> header();

        /**
         * HTTP Authorization header scheme.
         */
        @WithDefault(OidcConstants.BEARER_SCHEME)
        String authorizationScheme();

        /**
         * Required signature algorithm.
         * OIDC providers support many signature algorithms but if necessary you can restrict
         * Quarkus application to accept tokens signed only using an algorithm configured with this property.
         */
        Optional<SignatureAlgorithm> signatureAlgorithm();

        /**
         * Decryption key location for encrypted ID and access tokens.
         */
        Optional<String> decryptionKeyLocation();

        /**
         * Decrypt ID token.
         *
         * If the {@link Token#decryptionKeyLocation()} property is configured then the decryption key will be loaded from this
         * location.
         * Otherwise, if the JWT authentication token key is available, then it will be used to decrypt the token.
         * Finally, if a client secret is configured, it will be used as a secret key to decrypt the token.
         */
        Optional<Boolean> decryptIdToken();

        /**
         * Decrypt access token.
         *
         * If the {@link Token#decryptionKeyLocation()} property is configured then the decryption key will be loaded from this
         * location.
         * Otherwise, if the JWT authentication token key is available, then it will be used to decrypt the token.
         * Finally, if a client secret is configured, it will be used as a secret key to decrypt the token.
         */
        @WithDefault("false")
        boolean decryptAccessToken();

        /**
         * Allow the remote introspection of JWT tokens when no matching JWK key is available.
         *
         * This property is set to `true` by default for backward-compatibility reasons. It is planned that this default value
         * will be changed to `false` in an upcoming release.
         *
         * Also note this property is ignored if JWK endpoint URI is not available and introspecting the tokens is
         * the only verification option.
         */
        @WithDefault("true")
        boolean allowJwtIntrospection();

        /**
         * Require that JWT tokens are only introspected remotely.
         *
         */
        @WithDefault("false")
        boolean requireJwtIntrospectionOnly();

        /**
         * Allow the remote introspection of the opaque tokens.
         *
         * Set this property to `false` if only JWT tokens are expected.
         */
        @WithDefault("true")
        boolean allowOpaqueTokenIntrospection();

        /**
         * Token customizer name.
         *
         * Allows to select a tenant specific token customizer as a named bean.
         * Prefer using {@link TenantFeature} qualifier when registering custom {@link TokenCustomizer}.
         * Use this property only to refer to `TokenCustomizer` implementations provided by this extension.
         */
        Optional<String> customizerName();

        /**
         * Indirectly verify that the opaque (binary) access token is valid by using it to request UserInfo.
         * Opaque access token is considered valid if the provider accepted this token and returned a valid UserInfo.
         * You should only enable this option if the opaque access tokens must be accepted but OpenId Connect
         * provider does not have a token introspection endpoint.
         * This property has no effect when JWT tokens must be verified.
         */
        @ConfigDocDefault("false")
        Optional<Boolean> verifyAccessTokenWithUserInfo();

        /**
         * Token certificate binding options.
         */
        Binding binding();

    }

    interface Binding {

        /**
         * If a bearer access token must be bound to the client mTLS certificate.
         * It requires that JWT tokens must contain a confirmation `cnf` claim with a SHA256 certificate thumbprint
         * matching the client mTLS certificate's SHA256 certificate thumbprint.
         * <p>
         * For opaque tokens, SHA256 certificate thumbprint must be returned in their introspection response.
         */
        @WithDefault("false")
        boolean certificate();
    }

    enum ApplicationType {
        /**
         * A {@code WEB_APP} is a client that serves pages, usually a front-end application. For this type of client the
         * Authorization Code Flow is defined as the preferred method for authenticating users.
         */
        WEB_APP,

        /**
         * A {@code SERVICE} is a client that has a set of protected HTTP resources, usually a backend application following the
         * RESTful Architectural Design. For this type of client, the Bearer Authorization method is defined as the preferred
         * method for authenticating and authorizing users.
         */
        SERVICE,

        /**
         * A combined {@code SERVICE} and {@code WEB_APP} client.
         * For this type of client, the Bearer Authorization method is used if the Authorization header is set
         * and Authorization Code Flow - if not.
         */
        HYBRID
    }

    /**
     * Well known OpenId Connect provider identifier
     */
    Optional<Provider> provider();

    enum Provider {
        APPLE,
        DISCORD,
        FACEBOOK,
        GITHUB,
        GOOGLE,
        LINKEDIN,
        MASTODON,
        MICROSOFT,
        SLACK,
        SPOTIFY,
        STRAVA,
        TWITCH,
        TWITTER,
        // New name for Twitter
        X
    }

}
