package io.quarkus.oidc;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.oidc.common.runtime.OidcClientCommonConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.common.runtime.config.OidcCommonConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.builders.AuthenticationConfigBuilder;
import io.quarkus.oidc.runtime.builders.LogoutConfigBuilder;
import io.quarkus.oidc.runtime.builders.TokenConfigBuilder;
import io.quarkus.security.identity.SecurityIdentityAugmentor;

public class OidcTenantConfig extends OidcClientCommonConfig implements io.quarkus.oidc.runtime.OidcTenantConfig {

    /**
     * @deprecated Use {@link #builder()} to create this config
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public OidcTenantConfig() {

    }

    private OidcTenantConfig(io.quarkus.oidc.runtime.OidcTenantConfig mapping) {
        super(mapping);
        tenantId = mapping.tenantId();
        tenantEnabled = mapping.tenantEnabled();
        applicationType = mapping.applicationType().map(Enum::toString).map(ApplicationType::valueOf);
        authorizationPath = mapping.authorizationPath();
        userInfoPath = mapping.userInfoPath();
        introspectionPath = mapping.introspectionPath();
        jwksPath = mapping.jwksPath();
        endSessionPath = mapping.endSessionPath();
        tenantPaths = mapping.tenantPaths();
        publicKey = mapping.publicKey();
        introspectionCredentials.addConfigMappingValues(mapping.introspectionCredentials());
        roles.addConfigMappingValues(mapping.roles());
        token.addConfigMappingValues(mapping.token());
        logout.addConfigMappingValues(mapping.logout());
        resourceMetadata.addConfigMappingValues(mapping.resourceMetadata());
        certificateChain.addConfigMappingValues(mapping.certificateChain());
        authentication.addConfigMappingValues(mapping.authentication());
        codeGrant.addConfigMappingValues(mapping.codeGrant());
        tokenStateManager.addConfigMappingValues(mapping.tokenStateManager());
        allowTokenIntrospectionCache = mapping.allowTokenIntrospectionCache();
        allowUserInfoCache = mapping.allowUserInfoCache();
        cacheUserInfoInIdtoken = mapping.cacheUserInfoInIdtoken();
        jwks.addConfigMappingValues(mapping.jwks());
        provider = mapping.provider().map(Enum::toString).map(Provider::valueOf);
    }

    /**
     * A unique tenant identifier. It can be set by {@code TenantConfigResolver} providers, which
     * resolve the tenant configuration dynamically.
     *
     * @deprecated use {@link #tenantId()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> tenantId = Optional.empty();

    /**
     * If this tenant configuration is enabled.
     *
     * The default tenant is disabled if it is not configured but
     * a {@link TenantConfigResolver} that resolves tenant configurations is registered,
     * or named tenants are configured.
     * In this case, you do not need to disable the default tenant.
     *
     * @deprecated use {@link #tenantEnabled()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public boolean tenantEnabled = true;

    /**
     * The application type, which can be one of the following {@link ApplicationType} values.
     *
     * @deprecated use {@link #applicationType()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<ApplicationType> applicationType = Optional.empty();

    /**
     * The relative path or absolute URL of the OpenID Connect (OIDC) authorization endpoint, which authenticates
     * users.
     * You must set this property for `web-app` applications if OIDC discovery is disabled.
     * This property is ignored if OIDC discovery is enabled.
     *
     * @deprecated use {@link #authorizationPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> authorizationPath = Optional.empty();

    /**
     * The relative path or absolute URL of the OIDC UserInfo endpoint.
     * You must set this property for `web-app` applications if OIDC discovery is disabled
     * and the `authentication.user-info-required` property is enabled.
     * This property is ignored if OIDC discovery is enabled.
     *
     * @deprecated use {@link #userInfoPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> userInfoPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC RFC7662 introspection endpoint which can introspect both opaque and
     * JSON Web Token (JWT) tokens.
     * This property must be set if OIDC discovery is disabled and 1) the opaque bearer access tokens must be verified
     * or 2) JWT tokens must be verified while the cached JWK verification set with no matching JWK is being refreshed.
     * This property is ignored if the discovery is enabled.
     *
     * @deprecated use {@link #introspectionPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> introspectionPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC JSON Web Key Set (JWKS) endpoint which returns a JSON Web Key
     * Verification Set.
     * This property should be set if OIDC discovery is disabled and the local JWT verification is required.
     * This property is ignored if the discovery is enabled.
     *
     * @deprecated use {@link #jwksPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> jwksPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC end_session_endpoint.
     * This property must be set if OIDC discovery is disabled and RP Initiated Logout support for the `web-app` applications is
     * required.
     * This property is ignored if the discovery is enabled.
     *
     * @deprecated use {@link #endSessionPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> endSessionPath = Optional.empty();

    /**
     * The paths which must be secured by this tenant. Tenant with the most specific path wins.
     * Please see the xref:security-openid-connect-multitenancy.adoc#configure-tenant-paths[Configure tenant paths]
     * section of the OIDC multitenancy guide for explanation of allowed path patterns.
     *
     * @deprecated use {@link #tenantPaths()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<List<String>> tenantPaths = Optional.empty();

    /**
     * The public key for the local JWT token verification.
     * OIDC server connection is not created when this property is set.
     *
     * @deprecated use {@link #publicKey()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> publicKey = Optional.empty();

    /**
     * Introspection Basic Authentication which must be configured only if the introspection is required
     * and OpenId Connect Provider does not support the OIDC client authentication configured with
     * {@link OidcCommonConfig#credentials} for its introspection endpoint.
     *
     * @deprecated use {@link #introspectionCredentials()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public IntrospectionCredentials introspectionCredentials = new IntrospectionCredentials();

    /**
     * Introspection Basic Authentication configuration
     *
     * @deprecated use the {@link OidcTenantConfigBuilder.IntrospectionCredentialsBuilder}
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class IntrospectionCredentials implements io.quarkus.oidc.runtime.OidcTenantConfig.IntrospectionCredentials {
        /**
         * Name
         */
        public Optional<String> name = Optional.empty();

        /**
         * Secret
         */
        public Optional<String> secret = Optional.empty();

        /**
         * Include OpenId Connect Client ID configured with `quarkus.oidc.client-id`.
         */
        public boolean includeClientId = true;

        public Optional<String> getName() {
            return name;
        }

        public void setName(String name) {
            this.name = Optional.of(name);
        }

        public Optional<String> getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = Optional.of(secret);
        }

        public boolean isIncludeClientId() {
            return includeClientId;
        }

        public void setIncludeClientId(boolean includeClientId) {
            this.includeClientId = includeClientId;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.IntrospectionCredentials mapping) {
            name = mapping.name();
            secret = mapping.secret();
            includeClientId = mapping.includeClientId();
        }

        @Override
        public Optional<String> name() {
            return name;
        }

        @Override
        public Optional<String> secret() {
            return secret;
        }

        @Override
        public boolean includeClientId() {
            return includeClientId;
        }
    }

    /**
     * Configuration to find and parse a custom claim containing the roles information.
     *
     * @deprecated use the {@link #roles()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Roles roles = new Roles();

    /**
     * Configuration how to validate the token claims.
     *
     * @deprecated use the {@link #token()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Token token = new Token();

    /**
     * RP Initiated, BackChannel and FrontChannel Logout configuration
     *
     * @deprecated use the {@link #logout()} method
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Logout logout = new Logout();

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
     *
     * @deprecated use {@link #certificateChain()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public CertificateChain certificateChain = new CertificateChain();

    /**
     * @deprecated use the {@link OidcTenantConfigBuilder.CertificateChainBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class CertificateChain implements io.quarkus.oidc.runtime.OidcTenantConfig.CertificateChain {
        /**
         * Common name of the leaf certificate. It must be set if the {@link #trustStoreFile} does not have
         * this certificate imported.
         *
         */
        public Optional<String> leafCertificateName = Optional.empty();

        /**
         * Truststore file which keeps thumbprints of the trusted certificates.
         */
        public Optional<Path> trustStoreFile = Optional.empty();

        /**
         * A parameter to specify the password of the truststore file if it is configured with {@link #trustStoreFile}.
         */
        public Optional<String> trustStorePassword = Optional.empty();

        /**
         * A parameter to specify the alias of the truststore certificate.
         */
        public Optional<String> trustStoreCertAlias = Optional.empty();

        /**
         * An optional parameter to specify type of the truststore file. If not given, the type is automatically
         * detected
         * based on the file name.
         */
        public Optional<String> trustStoreFileType = Optional.empty();

        public Optional<Path> getTrustStoreFile() {
            return trustStoreFile;
        }

        public void setTrustStoreFile(Path trustStoreFile) {
            this.trustStoreFile = Optional.of(trustStoreFile);
        }

        public Optional<String> getTrustStoreCertAlias() {
            return trustStoreCertAlias;
        }

        public void setTrustStoreCertAlias(String trustStoreCertAlias) {
            this.trustStoreCertAlias = Optional.of(trustStoreCertAlias);
        }

        public Optional<String> getTrustStoreFileType() {
            return trustStoreFileType;
        }

        public void setTrustStoreFileType(Optional<String> trustStoreFileType) {
            this.trustStoreFileType = trustStoreFileType;
        }

        public Optional<String> getLeafCertificateName() {
            return leafCertificateName;
        }

        public void setLeafCertificateName(String leafCertificateName) {
            this.leafCertificateName = Optional.of(leafCertificateName);
        }

        public Optional<String> getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = Optional.ofNullable(trustStorePassword);
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.CertificateChain mapping) {
            leafCertificateName = mapping.leafCertificateName();
            trustStoreFile = mapping.trustStoreFile();
            trustStorePassword = mapping.trustStorePassword();
            trustStoreCertAlias = mapping.trustStoreCertAlias();
            trustStoreFileType = mapping.trustStoreFileType();
        }

        @Override
        public Optional<String> leafCertificateName() {
            return leafCertificateName;
        }

        @Override
        public Optional<Path> trustStoreFile() {
            return trustStoreFile;
        }

        @Override
        public Optional<String> trustStorePassword() {
            return trustStorePassword;
        }

        @Override
        public Optional<String> trustStoreCertAlias() {
            return trustStoreCertAlias;
        }

        @Override
        public Optional<String> trustStoreFileType() {
            return trustStoreFileType;
        }
    }

    /**
     * Different options to configure authorization requests
     *
     * @deprecated use the {@link #authentication()} method
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Authentication authentication = new Authentication();

    /**
     * Authorization code grant configuration
     *
     * @deprecated use the {@link #codeGrant()} method
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public CodeGrant codeGrant = new CodeGrant();

    /**
     * Default token state manager configuration
     *
     * @deprecated use the {@link #tokenStateManager()} method
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public TokenStateManager tokenStateManager = new TokenStateManager();

    /**
     * Allow caching the token introspection data.
     * Note enabling this property does not enable the cache itself but only permits to cache the token introspection
     * for a given tenant. If the default token cache can be used, see {@link OidcConfig.TokenCache} to enable
     * it.
     *
     * @deprecated use the {@link #allowTokenIntrospectionCache()} method
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public boolean allowTokenIntrospectionCache = true;

    /**
     * Allow caching the user info data.
     * Note enabling this property does not enable the cache itself but only permits to cache the user info data
     * for a given tenant. If the default token cache can be used, see {@link OidcConfig.TokenCache} to enable
     * it.
     *
     * @deprecated use the {@link #allowUserInfoCache()} method
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public boolean allowUserInfoCache = true;

    /**
     * Allow inlining UserInfo in IdToken instead of caching it in the token cache.
     * This property is only checked when an internal IdToken is generated when OAuth2 providers do not return IdToken.
     * Inlining UserInfo in the generated IdToken allows to store it in the session cookie and avoids introducing a cached
     * state.
     * <p>
     * Inlining UserInfo in the generated IdToken is enabled if the session cookie is encrypted
     * and the UserInfo cache is not enabled or caching UserInfo is disabled for the current tenant
     * with the {@link #allowUserInfoCache} property set to `false`.
     *
     * @deprecated use the {@link #cacheUserInfoInIdtoken()} method
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Boolean> cacheUserInfoInIdtoken = Optional.empty();

    /**
     * @deprecated use the {@link LogoutConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Logout implements io.quarkus.oidc.runtime.OidcTenantConfig.Logout {

        /**
         * The relative path of the logout endpoint at the application. If provided, the application is able to
         * initiate the
         * logout through this endpoint in conformance with the OpenID Connect RP-Initiated Logout specification.
         */
        public Optional<String> path = Optional.empty();

        /**
         * Relative path of the application endpoint where the user should be redirected to after logging out from the
         * OpenID
         * Connect Provider.
         * This endpoint URI must be properly registered at the OpenID Connect Provider as a valid redirect URI.
         */
        public Optional<String> postLogoutPath = Optional.empty();

        /**
         * Name of the post logout URI parameter which is added as a query parameter to the logout redirect URI.
         */
        public String postLogoutUriParam;

        /**
         * Additional properties which is added as the query parameters to the logout redirect URI.
         */
        public Map<String, String> extraParams;

        /**
         * Clear-Site-Data header directives
         */
        Optional<Set<ClearSiteData>> clearSiteData = Optional.of(Set.of());

        LogoutMode logoutMode = LogoutMode.QUERY;

        /**
         * Back-Channel Logout configuration
         */
        public Backchannel backchannel = new Backchannel();

        /**
         * Front-Channel Logout configuration
         */
        public Frontchannel frontchannel = new Frontchannel();

        public void setPath(Optional<String> path) {
            this.path = path;
        }

        public Optional<String> getPath() {
            return path;
        }

        public void setPostLogoutPath(Optional<String> postLogoutPath) {
            this.postLogoutPath = postLogoutPath;
        }

        public Optional<String> getPostLogoutPath() {
            return postLogoutPath;
        }

        public Map<String, String> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, String> extraParams) {
            this.extraParams = extraParams;
        }

        public String getPostLogoutUriParam() {
            return postLogoutUriParam;
        }

        public void setPostLogoutUriParam(String postLogoutUriParam) {
            this.postLogoutUriParam = postLogoutUriParam;
        }

        public Backchannel getBackchannel() {
            return backchannel;
        }

        public void setBackchannel(Backchannel backchannel) {
            this.backchannel = backchannel;
        }

        public Frontchannel getFrontchannel() {
            return frontchannel;
        }

        public void setFrontchannel(Frontchannel frontchannel) {
            this.frontchannel = frontchannel;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.Logout mapping) {
            path = mapping.path();
            postLogoutPath = mapping.postLogoutPath();
            postLogoutUriParam = mapping.postLogoutUriParam();
            extraParams = mapping.extraParams();
            clearSiteData = mapping.clearSiteData();
            logoutMode = mapping.logoutMode();
            backchannel.addConfigMappingValues(mapping.backchannel());
            frontchannel.addConfigMappingValues(mapping.frontchannel());
        }

        @Override
        public Optional<String> path() {
            return path;
        }

        @Override
        public Optional<String> postLogoutPath() {
            return postLogoutPath;
        }

        @Override
        public String postLogoutUriParam() {
            return postLogoutUriParam;
        }

        @Override
        public Map<String, String> extraParams() {
            return extraParams;
        }

        @Override
        public io.quarkus.oidc.runtime.OidcTenantConfig.Backchannel backchannel() {
            return backchannel;
        }

        @Override
        public io.quarkus.oidc.runtime.OidcTenantConfig.Frontchannel frontchannel() {
            return frontchannel;
        }

        @Override
        public Optional<Set<ClearSiteData>> clearSiteData() {
            return clearSiteData;
        }

        @Override
        public LogoutMode logoutMode() {
            return logoutMode;
        }
    }

    /**
     * @deprecated use the {@link OidcTenantConfigBuilder.BackchannelBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Backchannel implements io.quarkus.oidc.runtime.OidcTenantConfig.Backchannel {
        /**
         * The relative path of the Back-Channel Logout endpoint at the application.
         * It must start with the forward slash '/', for example, '/back-channel-logout'.
         * This value is always resolved relative to 'quarkus.http.root-path'.
         */
        public Optional<String> path = Optional.empty();

        /**
         * Maximum number of logout tokens that can be cached before they are matched against ID tokens stored in session
         * cookies.
         */
        public int tokenCacheSize = 10;

        /**
         * Number of minutes a logout token can be cached for.
         */
        public Duration tokenCacheTimeToLive = Duration.ofMinutes(10);

        /**
         * Token cache timer interval.
         * If this property is set, a timer checks and removes the stale entries periodically.
         */
        public Optional<Duration> cleanUpTimerInterval = Optional.empty();

        /**
         * Logout token claim whose value is used as a key for caching the tokens.
         * Only `sub` (subject) and `sid` (session id) claims can be used as keys.
         * Set it to `sid` only if ID tokens issued by the OIDC provider have no `sub` but have `sid` claim.
         */
        public String logoutTokenKey = "sub";

        public void setPath(Optional<String> path) {
            this.path = path;
        }

        public Optional<String> getPath() {
            return path;
        }

        public String getLogoutTokenKey() {
            return logoutTokenKey;
        }

        public void setLogoutTokenKey(String logoutTokenKey) {
            this.logoutTokenKey = logoutTokenKey;
        }

        public int getTokenCacheSize() {
            return tokenCacheSize;
        }

        public void setTokenCacheSize(int tokenCacheSize) {
            this.tokenCacheSize = tokenCacheSize;
        }

        public Duration getTokenCacheTimeToLive() {
            return tokenCacheTimeToLive;
        }

        public void setTokenCacheTimeToLive(Duration tokenCacheTimeToLive) {
            this.tokenCacheTimeToLive = tokenCacheTimeToLive;
        }

        public Optional<Duration> getCleanUpTimerInterval() {
            return cleanUpTimerInterval;
        }

        public void setCleanUpTimerInterval(Duration cleanUpTimerInterval) {
            this.cleanUpTimerInterval = Optional.of(cleanUpTimerInterval);
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.Backchannel mapping) {
            path = mapping.path();
            tokenCacheSize = mapping.tokenCacheSize();
            tokenCacheTimeToLive = mapping.tokenCacheTimeToLive();
            cleanUpTimerInterval = mapping.cleanUpTimerInterval();
            logoutTokenKey = mapping.logoutTokenKey();
        }

        @Override
        public Optional<String> path() {
            return path;
        }

        @Override
        public int tokenCacheSize() {
            return tokenCacheSize;
        }

        @Override
        public Duration tokenCacheTimeToLive() {
            return tokenCacheTimeToLive;
        }

        @Override
        public Optional<Duration> cleanUpTimerInterval() {
            return cleanUpTimerInterval;
        }

        @Override
        public String logoutTokenKey() {
            return logoutTokenKey;
        }
    }

    /**
     * Configuration for controlling how JsonWebKeySet containing verification keys should be acquired and managed.
     *
     * @deprecated use the {@link #jwks()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Jwks jwks = new Jwks();

    /**
     * @deprecated use the {@link OidcTenantConfigBuilder.JwksBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Jwks implements io.quarkus.oidc.runtime.OidcTenantConfig.Jwks {
        /**
         * If JWK verification keys should be fetched at the moment a connection to the OIDC provider
         * is initialized.
         * <p/>
         * Disabling this property delays the key acquisition until the moment the current token
         * has to be verified. Typically it can only be necessary if the token or other telated request properties
         * provide an additional context which is required to resolve the keys correctly.
         */
        public boolean resolveEarly = true;

        /**
         * Maximum number of JWK keys that can be cached.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        public int cacheSize = 10;

        /**
         * Number of minutes a JWK key can be cached for.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        public Duration cacheTimeToLive = Duration.ofMinutes(10);

        /**
         * Cache timer interval.
         * If this property is set, a timer checks and removes the stale entries periodically.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        public Optional<Duration> cleanUpTimerInterval = Optional.empty();

        /**
         * In case there is no key identifier ('kid') or certificate thumbprints ('x5t', 'x5t#S256') specified in the JOSE
         * header and no key could be determined, check all available keys matching the token algorithm ('alg') header value.
         */
        public boolean tryAll = false;

        public int getCacheSize() {
            return cacheSize;
        }

        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        public Duration getCacheTimeToLive() {
            return cacheTimeToLive;
        }

        public void setCacheTimeToLive(Duration cacheTimeToLive) {
            this.cacheTimeToLive = cacheTimeToLive;
        }

        public Optional<Duration> getCleanUpTimerInterval() {
            return cleanUpTimerInterval;
        }

        public void setCleanUpTimerInterval(Duration cleanUpTimerInterval) {
            this.cleanUpTimerInterval = Optional.of(cleanUpTimerInterval);
        }

        public boolean isResolveEarly() {
            return resolveEarly;
        }

        public void setResolveEarly(boolean resolveEarly) {
            this.resolveEarly = resolveEarly;
        }

        public boolean isTryAll() {
            return tryAll;
        }

        public void setTryAll(boolean fallbackToTryAll) {
            this.tryAll = fallbackToTryAll;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.Jwks mapping) {
            resolveEarly = mapping.resolveEarly();
            cacheSize = mapping.cacheSize();
            cacheTimeToLive = mapping.cacheTimeToLive();
            cleanUpTimerInterval = mapping.cleanUpTimerInterval();
            tryAll = mapping.tryAll();
        }

        @Override
        public boolean resolveEarly() {
            return resolveEarly;
        }

        @Override
        public int cacheSize() {
            return cacheSize;
        }

        @Override
        public Duration cacheTimeToLive() {
            return cacheTimeToLive;
        }

        @Override
        public Optional<Duration> cleanUpTimerInterval() {
            return cleanUpTimerInterval;
        }

        @Override
        public boolean tryAll() {
            return tryAll;
        }
    }

    /**
     * @deprecated use the {@link LogoutConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Frontchannel implements io.quarkus.oidc.runtime.OidcTenantConfig.Frontchannel {
        /**
         * The relative path of the Front-Channel Logout endpoint at the application.
         */
        public Optional<String> path = Optional.empty();

        public void setPath(Optional<String> path) {
            this.path = path;
        }

        public Optional<String> getPath() {
            return path;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.Frontchannel mapping) {
            path = mapping.path();
        }

        @Override
        public Optional<String> path() {
            return path;
        }
    }

    /**
     * Default Authorization Code token state manager configuration
     *
     * @deprecated use the {@link OidcTenantConfigBuilder.TokenStateManagerBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class TokenStateManager implements io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager {

        @Override
        public io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.Strategy strategy() {
            return strategy == null ? null
                    : io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.Strategy.valueOf(strategy.toString());
        }

        @Override
        public boolean splitTokens() {
            return splitTokens;
        }

        @Override
        public boolean encryptionRequired() {
            return encryptionRequired;
        }

        @Override
        public Optional<String> encryptionSecret() {
            return encryptionSecret;
        }

        @Override
        public io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.EncryptionAlgorithm encryptionAlgorithm() {
            return encryptionAlgorithm == null ? null
                    : io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.EncryptionAlgorithm
                            .valueOf(encryptionAlgorithm.toString());
        }

        public enum Strategy {
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
        public Strategy strategy = Strategy.KEEP_ALL_TOKENS;

        /**
         * Default TokenStateManager keeps all tokens (ID, access and refresh)
         * returned in the authorization code grant response in a single session cookie by default.
         *
         * Enable this property to minimize a session cookie size
         */
        public boolean splitTokens;

        /**
         * Mandates that the Default TokenStateManager encrypt the session cookie that stores the tokens.
         */
        public boolean encryptionRequired = true;

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
        public Optional<String> encryptionSecret = Optional.empty();

        /**
         * Supported session cookie key encryption algorithms
         */
        public static enum EncryptionAlgorithm {
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
        public EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithm.A256GCMKW;

        public boolean isEncryptionRequired() {
            return encryptionRequired;
        }

        public void setEncryptionRequired(boolean encryptionRequired) {
            this.encryptionRequired = encryptionRequired;
        }

        public Optional<String> getEncryptionSecret() {
            return encryptionSecret;
        }

        public void setEncryptionSecret(String encryptionSecret) {
            this.encryptionSecret = Optional.of(encryptionSecret);
        }

        public boolean isSplitTokens() {
            return splitTokens;
        }

        public void setSplitTokens(boolean splitTokens) {
            this.splitTokens = splitTokens;
        }

        public Strategy getStrategy() {
            return strategy;
        }

        public void setStrategy(Strategy strategy) {
            this.strategy = strategy;
        }

        public EncryptionAlgorithm getEncryptionAlgorithm() {
            return encryptionAlgorithm;
        }

        public void setEncryptionAlgorithm(EncryptionAlgorithm encryptionAlgorithm) {
            this.encryptionAlgorithm = encryptionAlgorithm;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager mapping) {
            strategy = Strategy.valueOf(mapping.strategy().toString());
            splitTokens = mapping.splitTokens();
            encryptionRequired = mapping.encryptionRequired();
            encryptionSecret = mapping.encryptionSecret();
            encryptionAlgorithm = EncryptionAlgorithm.valueOf(mapping.encryptionAlgorithm().toString());
        }
    }

    /**
     * @deprecated use the {@link #authorizationPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getAuthorizationPath() {
        return authorizationPath();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setAuthorizationPath(String authorizationPath) {
        this.authorizationPath = Optional.of(authorizationPath);
    }

    /**
     * @deprecated use the {@link #userInfoPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getUserInfoPath() {
        return userInfoPath();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setUserInfoPath(String userInfoPath) {
        this.userInfoPath = Optional.of(userInfoPath);
    }

    /**
     * @deprecated use the {@link #introspectionPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getIntrospectionPath() {
        return introspectionPath();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setIntrospectionPath(String introspectionPath) {
        this.introspectionPath = Optional.of(introspectionPath);
    }

    /**
     * @deprecated use the {@link #jwksPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getJwksPath() {
        return jwksPath();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setJwksPath(String jwksPath) {
        this.jwksPath = Optional.of(jwksPath);
    }

    /**
     * @deprecated use the {@link #endSessionPath()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getEndSessionPath() {
        return endSessionPath();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setEndSessionPath(String endSessionPath) {
        this.endSessionPath = Optional.of(endSessionPath);
    }

    /**
     * @deprecated use the {@link #publicKey()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getPublicKey() {
        return publicKey();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setPublicKey(String publicKey) {
        this.publicKey = Optional.of(publicKey);
    }

    /**
     * @deprecated use the {@link #roles()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Roles getRoles() {
        return roles;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setRoles(Roles roles) {
        this.roles = roles;
    }

    /**
     * @deprecated use the {@link #token()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Token getToken() {
        return token;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setToken(Token token) {
        this.token = token;
    }

    /**
     * @deprecated use the {@link #authentication()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * @deprecated use the {@link #tenantId()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<String> getTenantId() {
        return tenantId();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setTenantId(String tenantId) {
        this.tenantId = Optional.of(tenantId);
    }

    /**
     * @deprecated use the {@link #tenantEnabled()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public boolean isTenantEnabled() {
        return tenantEnabled();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setTenantEnabled(boolean enabled) {
        this.tenantEnabled = enabled;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setLogout(Logout logout) {
        this.logout = logout;
    }

    /**
     * @deprecated use the {@link #logout()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Logout getLogout() {
        return logout;
    }

    /**
     * @deprecated use the {@link OidcTenantConfigBuilder.RolesBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Roles implements io.quarkus.oidc.runtime.OidcTenantConfig.Roles {

        public static Roles fromClaimPath(List<String> path) {
            return fromClaimPathAndSeparator(path, null);
        }

        public static Roles fromClaimPathAndSeparator(List<String> path, String sep) {
            Roles roles = new Roles();
            roles.roleClaimPath = Optional.ofNullable(path);
            roles.roleClaimSeparator = Optional.ofNullable(sep);
            return roles;
        }

        /**
         * A list of paths to claims containing an array of groups.
         * Each path starts from the top level JWT JSON object
         * and can contain multiple segments.
         * Each segment represents a JSON object name only; for example: "realm/groups".
         * Use double quotes with the namespace-qualified claim names.
         * This property can be used if a token has no `groups` claim but has the groups set in one or more different claims.
         */
        public Optional<List<String>> roleClaimPath = Optional.empty();
        /**
         * The separator for splitting strings that contain multiple group values.
         * It is only used if the "role-claim-path" property points to one or more custom claims whose values are strings.
         * A single space is used by default because the standard `scope` claim can contain a space-separated sequence.
         */
        public Optional<String> roleClaimSeparator = Optional.empty();

        /**
         * Source of the principal roles.
         */
        public Optional<Source> source = Optional.empty();

        public Optional<List<String>> getRoleClaimPath() {
            return roleClaimPath;
        }

        public void setRoleClaimPath(List<String> roleClaimPath) {
            this.roleClaimPath = Optional.of(roleClaimPath);
        }

        public Optional<String> getRoleClaimSeparator() {
            return roleClaimSeparator;
        }

        public void setRoleClaimSeparator(String roleClaimSeparator) {
            this.roleClaimSeparator = Optional.of(roleClaimSeparator);
        }

        public Optional<Source> getSource() {
            return source;
        }

        public void setSource(Source source) {
            this.source = Optional.of(source);
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.Roles mapping) {
            roleClaimPath = mapping.roleClaimPath();
            roleClaimSeparator = mapping.roleClaimSeparator();
            source = mapping.source().map(Enum::toString).map(Source::valueOf);
        }

        @Override
        public Optional<List<String>> roleClaimPath() {
            return roleClaimPath;
        }

        @Override
        public Optional<String> roleClaimSeparator() {
            return roleClaimSeparator;
        }

        @Override
        public Optional<io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source> source() {
            return source.map(Enum::toString).map(io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source::valueOf);
        }

        // Source of the principal roles
        public static enum Source {
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
     *
     * @deprecated use the {@link AuthenticationConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Authentication implements io.quarkus.oidc.runtime.OidcTenantConfig.Authentication {

        @Override
        public Optional<io.quarkus.oidc.runtime.OidcTenantConfig.Authentication.ResponseMode> responseMode() {
            return responseMode.map(Enum::toString)
                    .map(io.quarkus.oidc.runtime.OidcTenantConfig.Authentication.ResponseMode::valueOf);
        }

        @Override
        public Optional<String> redirectPath() {
            return redirectPath;
        }

        @Override
        public boolean restorePathAfterRedirect() {
            return restorePathAfterRedirect;
        }

        @Override
        public boolean removeRedirectParameters() {
            return removeRedirectParameters;
        }

        @Override
        public Optional<String> errorPath() {
            return errorPath;
        }

        @Override
        public Optional<String> sessionExpiredPath() {
            return sessionExpiredPath;
        }

        @Override
        public boolean verifyAccessToken() {
            return verifyAccessToken;
        }

        @Override
        public Optional<Boolean> forceRedirectHttpsScheme() {
            return forceRedirectHttpsScheme;
        }

        @Override
        public Optional<List<String>> scopes() {
            return scopes;
        }

        @Override
        public Optional<String> scopeSeparator() {
            return scopeSeparator;
        }

        @Override
        public boolean nonceRequired() {
            return nonceRequired;
        }

        @Override
        public Optional<Boolean> addOpenidScope() {
            return addOpenidScope;
        }

        @Override
        public Map<String, String> extraParams() {
            return extraParams;
        }

        @Override
        public Optional<List<String>> forwardParams() {
            return forwardParams;
        }

        @Override
        public boolean cookieForceSecure() {
            return cookieForceSecure;
        }

        @Override
        public Optional<String> cookieSuffix() {
            return cookieSuffix;
        }

        @Override
        public String cookiePath() {
            return cookiePath;
        }

        @Override
        public Optional<String> cookiePathHeader() {
            return cookiePathHeader;
        }

        @Override
        public Optional<String> cookieDomain() {
            return cookieDomain;
        }

        @Override
        public io.quarkus.oidc.runtime.OidcTenantConfig.Authentication.CookieSameSite cookieSameSite() {
            return cookieSameSite == null ? null
                    : io.quarkus.oidc.runtime.OidcTenantConfig.Authentication.CookieSameSite.valueOf(cookieSameSite.toString());
        }

        @Override
        public boolean allowMultipleCodeFlows() {
            return allowMultipleCodeFlows;
        }

        @Override
        public boolean failOnMissingStateParam() {
            return failOnMissingStateParam;
        }

        @Override
        public boolean failOnUnresolvedKid() {
            return failOnUnresolvedKid;
        }

        @Override
        public Optional<Boolean> userInfoRequired() {
            return userInfoRequired;
        }

        @Override
        public Duration sessionAgeExtension() {
            return sessionAgeExtension;
        }

        @Override
        public Duration stateCookieAge() {
            return stateCookieAge;
        }

        @Override
        public boolean javaScriptAutoRedirect() {
            return javaScriptAutoRedirect;
        }

        @Override
        public Optional<Boolean> idTokenRequired() {
            return idTokenRequired;
        }

        @Override
        public Optional<Duration> internalIdTokenLifespan() {
            return internalIdTokenLifespan;
        }

        @Override
        public Optional<Boolean> pkceRequired() {
            return pkceRequired;
        }

        @Override
        public Optional<String> pkceSecret() {
            return pkceSecret;
        }

        @Override
        public Optional<String> stateSecret() {
            return stateSecret;
        }

        /**
         * SameSite attribute values for the session cookie.
         */
        public enum CookieSameSite {
            STRICT,
            LAX,
            NONE
        }

        /**
         * Authorization code flow response mode
         */
        public enum ResponseMode {
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
        public Optional<ResponseMode> responseMode = Optional.empty();

        /**
         * The relative path for calculating a `redirect_uri` query parameter.
         * It has to start from a forward slash and is appended to the request URI's host and port.
         * For example, if the current request URI is `https://localhost:8080/service`, a `redirect_uri` parameter
         * is set to `https://localhost:8080/` if this property is set to `/` and be the same as the request URI
         * if this property has not been configured.
         * Note the original request URI is restored after the user has authenticated if `restorePathAfterRedirect` is set
         * to `true`.
         */
        public Optional<String> redirectPath = Optional.empty();

        /**
         * If this property is set to `true`, the original request URI which was used before
         * the authentication is restored after the user has been redirected back to the application.
         *
         * Note if `redirectPath` property is not set, the original request URI is restored even if this property is
         * disabled.
         */
        public boolean restorePathAfterRedirect;

        /**
         * Remove the query parameters such as `code` and `state` set by the OIDC server on the redirect URI
         * after the user has authenticated by redirecting a user to the same URI but without the query parameters.
         */
        public boolean removeRedirectParameters = true;

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
        public Optional<String> errorPath = Optional.empty();

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
        public Optional<String> sessionExpiredPath = Optional.empty();

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
        public boolean verifyAccessToken;

        /**
         * Force `https` as the `redirect_uri` parameter scheme when running behind an SSL/TLS terminating reverse
         * proxy.
         * This property, if enabled, also affects the logout `post_logout_redirect_uri` and the local redirect requests.
         */
        public Optional<Boolean> forceRedirectHttpsScheme = Optional.empty();

        /**
         * List of scopes
         */
        public Optional<List<String>> scopes = Optional.empty();

        /**
         * The separator which is used when more than one scope is configured.
         * A single space is used by default.
         */
        public Optional<String> scopeSeparator = Optional.empty();

        /**
         * Require that ID token includes a `nonce` claim which must match `nonce` authentication request query parameter.
         * Enabling this property can help mitigate replay attacks.
         * Do not enable this property if your OpenId Connect provider does not support setting `nonce` in ID token
         * or if you work with OAuth2 provider such as `GitHub` which does not issue ID tokens.
         */
        public boolean nonceRequired = false;

        /**
         * Add the `openid` scope automatically to the list of scopes. This is required for OpenId Connect providers,
         * but does not work for OAuth2 providers such as Twitter OAuth2, which do not accept this scope and throw errors.
         */
        public Optional<Boolean> addOpenidScope = Optional.empty();

        /**
         * Additional properties added as query parameters to the authentication redirect URI.
         */
        public Map<String, String> extraParams = new HashMap<>();

        /**
         * Request URL query parameters which, if present, are added to the authentication redirect URI.
         */
        public Optional<List<String>> forwardParams = Optional.empty();

        /**
         * If enabled the state, session, and post logout cookies have their `secure` parameter set to `true`
         * when HTTP is used. It might be necessary when running behind an SSL/TLS terminating reverse proxy.
         * The cookies are always secure if HTTPS is used, even if this property is set to false.
         */
        public boolean cookieForceSecure;

        /**
         * Cookie name suffix.
         * For example, a session cookie name for the default OIDC tenant is `q_session` but can be changed to `q_session_test`
         * if this property is set to `test`.
         */
        public Optional<String> cookieSuffix = Optional.empty();

        /**
         * Cookie path parameter value which, if set, is used to set a path parameter for the session, state and post
         * logout cookies.
         * The `cookie-path-header` property, if set, is checked first.
         */
        public String cookiePath = "/";

        /**
         * Cookie path header parameter value which, if set, identifies the incoming HTTP header
         * whose value is used to set a path parameter for the session, state and post logout cookies.
         * If the header is missing, the `cookie-path` property is checked.
         */
        public Optional<String> cookiePathHeader = Optional.empty();

        /**
         * Cookie domain parameter value which, if set, is used for the session, state and post logout cookies.
         */
        public Optional<String> cookieDomain = Optional.empty();

        /**
         * SameSite attribute for the session cookie.
         */
        public CookieSameSite cookieSameSite = CookieSameSite.LAX;

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
        public boolean allowMultipleCodeFlows = true;

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
        public boolean failOnMissingStateParam = false;

        /**
         * Fail with the HTTP 401 error if the ID token signature can not be verified during the re-authentication only due to
         * an unresolved token key identifier (`kid`).
         * <p>
         * This property might need to be disabled when multiple tab authentications are allowed, with one of the tabs keeping
         * an expired ID token with its `kid`
         * unresolved due to the verification key set refreshed due to another tab initiating an authorization code flow. In
         * such cases, instead of failing with the HTTP 401 error,
         * redirecting the user to re-authenticate with the HTTP 302 status may provide better user experience.
         * <p>
         * Note that the HTTP 401 error is always returned if the ID token signature can not be verified due to an unresolved
         * kid during an initial ID token verification
         * following the authorization code flow completion, before a session cookie is created.
         */
        public boolean failOnUnresolvedKid = true;

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
        public Optional<Boolean> userInfoRequired = Optional.empty();

        /**
         * Session age extension in minutes.
         * The user session age property is set to the value of the ID token life-span by default and
         * the user is redirected to the OIDC provider to re-authenticate once the session has expired.
         * If this property is set to a nonzero value, then the expired ID token can be refreshed before
         * the session has expired.
         * This property is ignored if the `token.refresh-expired` property has not been enabled.
         */
        public Duration sessionAgeExtension = Duration.ofMinutes(5);

        /**
         * State cookie age in minutes.
         * State cookie is created every time a new authorization code flow redirect starts
         * and removed when this flow is completed.
         * State cookie name is unique by default, see {@link #allowMultipleCodeFlows}.
         * Keep its age to the reasonable minimum value such as 5 minutes or less.
         */
        public Duration stateCookieAge = Duration.ofMinutes(5);

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
        public boolean javaScriptAutoRedirect = true;

        /**
         * Requires that ID token is available when the authorization code flow completes.
         * Disable this property only when you need to use the authorization code flow with OAuth2 providers which do not return
         * ID token - an internal IdToken is generated in such cases.
         */
        public Optional<Boolean> idTokenRequired = Optional.empty();

        /**
         * Internal ID token lifespan.
         * This property is only checked when an internal IdToken is generated when Oauth2 providers do not return IdToken.
         */
        public Optional<Duration> internalIdTokenLifespan = Optional.empty();

        /**
         * Requires that a Proof Key for Code Exchange (PKCE) is used.
         */
        public Optional<Boolean> pkceRequired = Optional.empty();

        /**
         * Secret used to encrypt a Proof Key for Code Exchange (PKCE) code verifier in the code flow state.
         * This secret should be at least 32 characters long.
         *
         * @deprecated This field is deprecated. Use {@link #stateSecret} instead.
         *
         */
        public Optional<String> pkceSecret = Optional.empty();

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
        public Optional<String> stateSecret = Optional.empty();

        public Optional<Duration> getInternalIdTokenLifespan() {
            return internalIdTokenLifespan;
        }

        public void setInternalIdTokenLifespan(Duration internalIdTokenLifespan) {
            this.internalIdTokenLifespan = Optional.of(internalIdTokenLifespan);
        }

        public Optional<Boolean> isPkceRequired() {
            return pkceRequired;
        }

        public void setPkceRequired(boolean pkceRequired) {
            this.pkceRequired = Optional.of(pkceRequired);
        }

        @Deprecated(forRemoval = true)
        public Optional<String> getPkceSecret() {
            return pkceSecret;
        }

        @Deprecated(forRemoval = true)
        public void setPkceSecret(String pkceSecret) {
            this.pkceSecret = Optional.of(pkceSecret);
        }

        public Optional<String> getErrorPath() {
            return errorPath;
        }

        public void setErrorPath(String errorPath) {
            this.errorPath = Optional.of(errorPath);
        }

        public boolean isJavaScriptAutoRedirect() {
            return javaScriptAutoRedirect;
        }

        public void setJavaScriptAutoredirect(boolean autoRedirect) {
            this.javaScriptAutoRedirect = autoRedirect;
        }

        public Optional<String> getRedirectPath() {
            return redirectPath;
        }

        public void setRedirectPath(String redirectPath) {
            this.redirectPath = Optional.of(redirectPath);
        }

        public Optional<List<String>> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = Optional.of(scopes);
        }

        public Map<String, String> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, String> extraParams) {
            this.extraParams = extraParams;
        }

        public void setAddOpenidScope(boolean addOpenidScope) {
            this.addOpenidScope = Optional.of(addOpenidScope);
        }

        public Optional<Boolean> isAddOpenidScope() {
            return addOpenidScope;
        }

        public Optional<Boolean> isForceRedirectHttpsScheme() {
            return forceRedirectHttpsScheme;
        }

        public void setForceRedirectHttpsScheme(boolean forceRedirectHttpsScheme) {
            this.forceRedirectHttpsScheme = Optional.of(forceRedirectHttpsScheme);
        }

        public boolean isRestorePathAfterRedirect() {
            return restorePathAfterRedirect;
        }

        public void setRestorePathAfterRedirect(boolean restorePathAfterRedirect) {
            this.restorePathAfterRedirect = restorePathAfterRedirect;
        }

        public boolean isCookieForceSecure() {
            return cookieForceSecure;
        }

        public void setCookieForceSecure(boolean cookieForceSecure) {
            this.cookieForceSecure = cookieForceSecure;
        }

        public String getCookiePath() {
            return cookiePath;
        }

        public void setCookiePath(String cookiePath) {
            this.cookiePath = cookiePath;
        }

        public Optional<String> getCookieDomain() {
            return cookieDomain;
        }

        public void setCookieDomain(String cookieDomain) {
            this.cookieDomain = Optional.of(cookieDomain);
        }

        public Optional<Boolean> isUserInfoRequired() {
            return userInfoRequired;
        }

        public void setUserInfoRequired(boolean userInfoRequired) {
            this.userInfoRequired = Optional.of(userInfoRequired);
        }

        public boolean isRemoveRedirectParameters() {
            return removeRedirectParameters;
        }

        public void setRemoveRedirectParameters(boolean removeRedirectParameters) {
            this.removeRedirectParameters = removeRedirectParameters;
        }

        public boolean isVerifyAccessToken() {
            return verifyAccessToken;
        }

        public void setVerifyAccessToken(boolean verifyAccessToken) {
            this.verifyAccessToken = verifyAccessToken;
        }

        public Duration getSessionAgeExtension() {
            return sessionAgeExtension;
        }

        public void setSessionAgeExtension(Duration sessionAgeExtension) {
            this.sessionAgeExtension = sessionAgeExtension;
        }

        public Optional<String> getCookiePathHeader() {
            return cookiePathHeader;
        }

        public void setCookiePathHeader(String cookiePathHeader) {
            this.cookiePathHeader = Optional.of(cookiePathHeader);
        }

        public Optional<Boolean> isIdTokenRequired() {
            return idTokenRequired;
        }

        public void setIdTokenRequired(boolean idTokenRequired) {
            this.idTokenRequired = Optional.of(idTokenRequired);
        }

        public Optional<String> getCookieSuffix() {
            return cookieSuffix;
        }

        public void setCookieSuffix(String cookieSuffix) {
            this.cookieSuffix = Optional.of(cookieSuffix);
        }

        public Optional<ResponseMode> getResponseMode() {
            return responseMode;
        }

        public void setResponseMode(ResponseMode responseMode) {
            this.responseMode = Optional.of(responseMode);
        }

        public Optional<List<String>> getForwardParams() {
            return forwardParams;
        }

        public void setForwardParams(List<String> forwardParams) {
            this.forwardParams = Optional.of(forwardParams);
        }

        public CookieSameSite getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(CookieSameSite cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }

        public boolean isAllowMultipleCodeFlows() {
            return allowMultipleCodeFlows;
        }

        public void setAllowMultipleCodeFlows(boolean allowMultipleCodeFlows) {
            this.allowMultipleCodeFlows = allowMultipleCodeFlows;
        }

        public boolean isNonceRequired() {
            return nonceRequired;
        }

        public void setNonceRequired(boolean nonceRequired) {
            this.nonceRequired = nonceRequired;
        }

        public Optional<String> getStateSecret() {
            return stateSecret;
        }

        public void setStateSecret(Optional<String> stateSecret) {
            this.stateSecret = stateSecret;
        }

        public Optional<String> getScopeSeparator() {
            return scopeSeparator;
        }

        public void setScopeSeparator(String scopeSeparator) {
            this.scopeSeparator = Optional.of(scopeSeparator);
        }

        public Duration getStateCookieAge() {
            return stateCookieAge;
        }

        public void setStateCookieAge(Duration stateCookieAge) {
            this.stateCookieAge = stateCookieAge;
        }

        public Optional<String> getSessionExpiredPath() {
            return sessionExpiredPath;
        }

        public void setSessionExpiredPath(String sessionExpiredPath) {
            this.sessionExpiredPath = Optional.of(sessionExpiredPath);
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.Authentication mapping) {
            responseMode = mapping.responseMode().map(Enum::toString).map(ResponseMode::valueOf);
            redirectPath = mapping.redirectPath();
            restorePathAfterRedirect = mapping.restorePathAfterRedirect();
            removeRedirectParameters = mapping.removeRedirectParameters();
            errorPath = mapping.errorPath();
            sessionExpiredPath = mapping.sessionExpiredPath();
            verifyAccessToken = mapping.verifyAccessToken();
            forceRedirectHttpsScheme = mapping.forceRedirectHttpsScheme();
            scopes = mapping.scopes();
            scopeSeparator = mapping.scopeSeparator();
            nonceRequired = mapping.nonceRequired();
            addOpenidScope = mapping.addOpenidScope();
            extraParams = mapping.extraParams();
            forwardParams = mapping.forwardParams();
            cookieForceSecure = mapping.cookieForceSecure();
            cookieSuffix = mapping.cookieSuffix();
            cookiePath = mapping.cookiePath();
            cookiePathHeader = mapping.cookiePathHeader();
            cookieDomain = mapping.cookieDomain();
            cookieSameSite = CookieSameSite.valueOf(mapping.cookieSameSite().toString());
            allowMultipleCodeFlows = mapping.allowMultipleCodeFlows();
            failOnMissingStateParam = mapping.failOnMissingStateParam();
            failOnUnresolvedKid = mapping.failOnUnresolvedKid();
            userInfoRequired = mapping.userInfoRequired();
            sessionAgeExtension = mapping.sessionAgeExtension();
            stateCookieAge = mapping.stateCookieAge();
            javaScriptAutoRedirect = mapping.javaScriptAutoRedirect();
            idTokenRequired = mapping.idTokenRequired();
            internalIdTokenLifespan = mapping.internalIdTokenLifespan();
            pkceRequired = mapping.pkceRequired();
            pkceSecret = mapping.pkceSecret();
            stateSecret = mapping.stateSecret();
        }
    }

    /**
     * Authorization Code grant configuration
     *
     * @deprecated use the {@link OidcTenantConfigBuilder.CodeGrantBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class CodeGrant implements io.quarkus.oidc.runtime.OidcTenantConfig.CodeGrant {

        /**
         * Additional parameters, in addition to the required `code` and `redirect-uri` parameters,
         * which must be included to complete the authorization code grant request.
         */
        public Map<String, String> extraParams = new HashMap<>();

        /**
         * Custom HTTP headers which must be sent to complete the authorization code grant request.
         */
        public Map<String, String> headers = new HashMap<>();

        public Map<String, String> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, String> extraParams) {
            this.extraParams = extraParams;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.CodeGrant mapping) {
            extraParams = mapping.extraParams();
            headers = mapping.headers();
        }

        @Override
        public Map<String, String> extraParams() {
            return extraParams;
        }

        @Override
        public Map<String, String> headers() {
            return headers;
        }
    }

    /**
     * Supported asymmetric signature algorithms
     */
    public static enum SignatureAlgorithm {
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

    /**
     * @deprecated use the {@link TokenConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Token implements io.quarkus.oidc.runtime.OidcTenantConfig.Token {

        public static Token fromIssuer(String issuer) {
            Token tokenClaims = new Token();
            tokenClaims.issuer = Optional.of(issuer);
            tokenClaims.audience = Optional.ofNullable(null);
            return tokenClaims;
        }

        public static Token fromAudience(String... audience) {
            Token tokenClaims = new Token();
            tokenClaims.issuer = Optional.ofNullable(null);
            tokenClaims.audience = Optional.of(Arrays.asList(audience));
            return tokenClaims;
        }

        /**
         * The expected issuer `iss` claim value.
         * This property overrides the `issuer` property, which might be set in OpenId Connect provider's well-known
         * configuration.
         * If the `iss` claim value varies depending on the host, IP address, or tenant id of the provider, you can skip the
         * issuer verification by setting this property to `any`, but it should be done only when other options (such as
         * configuring
         * the provider to use the fixed `iss` claim value) are not possible.
         */
        public Optional<String> issuer = Optional.empty();

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
        public Optional<List<String>> audience = Optional.empty();

        /**
         * Require that the token includes a `sub` (subject) claim which is a unique
         * and never reassigned identifier for the current user.
         * Note that if you enable this property and if UserInfo is also required,
         * both the token and UserInfo `sub` claims must be present and match each other.
         */
        public boolean subjectRequired = false;

        /**
         * A map of required claims and their expected values.
         * For example, `quarkus.oidc.token.required-claims.org_id = org_xyz` would require tokens to have the `org_id` claim to
         * be present and set to `org_xyz`.
         * Strings are the only supported types. Use {@linkplain SecurityIdentityAugmentor} to verify claims of other types or
         * complex claims.
         */
        public Map<String, Set<String>> requiredClaims = new HashMap<>();

        /**
         * Expected token type
         */
        public Optional<String> tokenType = Optional.empty();

        /**
         * Life span grace period in seconds.
         * When checking token expiry, current time is allowed to be later than token expiration time by at most the configured
         * number of seconds.
         * When checking token issuance, current time is allowed to be sooner than token issue time by at most the configured
         * number of seconds.
         */
        public OptionalInt lifespanGrace = OptionalInt.empty();

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
        public Optional<Duration> age = Optional.empty();

        /**
         * Require that the token includes a `iat` (issued at) claim
         *
         * Set this property to `false` if your JWT token does not contain an `iat` (issued at) claim.
         * Note that ID token is always required to have an `iat` claim and therefore this property has no impact on the ID
         * token verification process.
         */
        public boolean issuedAtRequired = true;

        /**
         * Name of the claim which contains a principal name. By default, the `upn`, `preferred_username` and `sub`
         * claims are
         * checked.
         */
        public Optional<String> principalClaim = Optional.empty();

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
        public boolean refreshExpired;

        /**
         * The refresh token time skew, in seconds.
         * If this property is enabled, the configured number of seconds is added to the current time
         * when checking if the authorization code ID or access token should be refreshed.
         * If the sum is greater than the authorization code ID or access token's expiration time, a refresh is going to
         * happen.
         */
        public Optional<Duration> refreshTokenTimeSkew = Optional.empty();

        /**
         * The forced JWK set refresh interval in minutes.
         */
        public Duration forcedJwkRefreshInterval = Duration.ofMinutes(10);

        /**
         * Custom HTTP header that contains a bearer token.
         * This option is valid only when the application is of type {@link ApplicationType#SERVICE}.
         */
        public Optional<String> header = Optional.empty();

        /**
         * HTTP Authorization header scheme.
         */
        public String authorizationScheme = OidcConstants.BEARER_SCHEME;

        /**
         * Required signature algorithm.
         * OIDC providers support many signature algorithms but if necessary you can restrict
         * Quarkus application to accept tokens signed only using an algorithm configured with this property.
         */
        public Optional<SignatureAlgorithm> signatureAlgorithm = Optional.empty();

        /**
         * Decryption key location.
         * JWT tokens can be inner-signed and encrypted by OpenId Connect providers.
         * However, it is not always possible to remotely introspect such tokens because
         * the providers might not control the private decryption keys.
         * In such cases set this property to point to the file containing the decryption private key in
         * PEM or JSON Web Key (JWK) format.
         * If this property is not set and the `private_key_jwt` client authentication method is used, the private key
         * used to sign the client authentication JWT tokens are also used to decrypt the encrypted ID tokens.
         */
        public Optional<String> decryptionKeyLocation = Optional.empty();

        /**
         * Decrypt ID token.
         */
        Optional<Boolean> decryptIdToken = Optional.empty();

        /**
         * Decrypt access token.
         */
        boolean decryptAccessToken;

        /**
         * Allow the remote introspection of JWT tokens when no matching JWK key is available.
         *
         * This property is set to `true` by default for backward-compatibility reasons. It is planned that this default value
         * will be changed to `false` in an upcoming release.
         *
         * Also note this property is ignored if JWK endpoint URI is not available and introspecting the tokens is
         * the only verification option.
         */
        public boolean allowJwtIntrospection = true;

        /**
         * Require that JWT tokens are only introspected remotely.
         *
         */
        public boolean requireJwtIntrospectionOnly = false;

        /**
         * Allow the remote introspection of the opaque tokens.
         *
         * Set this property to `false` if only JWT tokens are expected.
         */
        public boolean allowOpaqueTokenIntrospection = true;

        /**
         * Token customizer name.
         *
         * Allows to select a tenant specific token customizer as a named bean.
         * Prefer using {@link TenantFeature} qualifier when registering custom {@link TokenCustomizer}.
         * Use this property only to refer to `TokenCustomizer` implementations provided by this extension.
         */
        public Optional<String> customizerName = Optional.empty();

        /**
         * Indirectly verify that the opaque (binary) access token is valid by using it to request UserInfo.
         * Opaque access token is considered valid if the provider accepted this token and returned a valid UserInfo.
         * You should only enable this option if the opaque access tokens must be accepted but OpenId Connect
         * provider does not have a token introspection endpoint.
         * This property has no effect when JWT tokens must be verified.
         */
        public Optional<Boolean> verifyAccessTokenWithUserInfo = Optional.empty();

        /**
         * Token binding options
         */
        Binding binding = new Binding();

        public Optional<Boolean> isVerifyAccessTokenWithUserInfo() {
            return verifyAccessTokenWithUserInfo;
        }

        public void setVerifyAccessTokenWithUserInfo(boolean verify) {
            this.verifyAccessTokenWithUserInfo = Optional.of(verify);
        }

        public Optional<String> getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = Optional.of(issuer);
        }

        public Optional<String> getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = Optional.of(header);
        }

        public Optional<List<String>> getAudience() {
            return audience;
        }

        public void setAudience(List<String> audience) {
            this.audience = Optional.of(audience);
        }

        public OptionalInt getLifespanGrace() {
            return lifespanGrace;
        }

        public void setLifespanGrace(int lifespanGrace) {
            this.lifespanGrace = OptionalInt.of(lifespanGrace);
        }

        public Optional<String> getPrincipalClaim() {
            return principalClaim;
        }

        public void setPrincipalClaim(String principalClaim) {
            this.principalClaim = Optional.of(principalClaim);
        }

        public boolean isRefreshExpired() {
            return refreshExpired;
        }

        public void setRefreshExpired(boolean refreshExpired) {
            this.refreshExpired = refreshExpired;
        }

        public Duration getForcedJwkRefreshInterval() {
            return forcedJwkRefreshInterval;
        }

        public void setForcedJwkRefreshInterval(Duration forcedJwkRefreshInterval) {
            this.forcedJwkRefreshInterval = forcedJwkRefreshInterval;
        }

        public Optional<String> getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = Optional.of(tokenType);
        }

        public Optional<Duration> getRefreshTokenTimeSkew() {
            return refreshTokenTimeSkew;
        }

        public void setRefreshTokenTimeSkew(Duration refreshTokenTimeSkew) {
            this.refreshTokenTimeSkew = Optional.of(refreshTokenTimeSkew);
        }

        public boolean isAllowJwtIntrospection() {
            return allowJwtIntrospection;
        }

        public void setAllowJwtIntrospection(boolean allowJwtIntrospection) {
            this.allowJwtIntrospection = allowJwtIntrospection;
        }

        public boolean isAllowOpaqueTokenIntrospection() {
            return allowOpaqueTokenIntrospection;
        }

        public void setAllowOpaqueTokenIntrospection(boolean allowOpaqueTokenIntrospection) {
            this.allowOpaqueTokenIntrospection = allowOpaqueTokenIntrospection;
        }

        public Binding getBinding() {
            return binding;
        }

        public io.quarkus.oidc.runtime.OidcTenantConfig.Binding binding() {
            return binding;
        }

        public Optional<Duration> getAge() {
            return age;
        }

        public void setAge(Duration age) {
            this.age = Optional.of(age);
        }

        public boolean isIssuedAtRequired() {
            return issuedAtRequired;
        }

        public void setIssuedAtRequired(boolean issuedAtRequired) {
            this.issuedAtRequired = issuedAtRequired;
        }

        public Optional<String> getDecryptionKeyLocation() {
            return decryptionKeyLocation;
        }

        public void setDecryptionKeyLocation(String decryptionKeyLocation) {
            this.decryptionKeyLocation = Optional.of(decryptionKeyLocation);
        }

        public Map<String, Set<String>> getRequiredClaims() {
            return requiredClaims;
        }

        public void setRequiredClaims(Map<String, Set<String>> requiredClaims) {
            this.requiredClaims = requiredClaims;
        }

        public boolean isRequireJwtIntrospectionOnly() {
            return requireJwtIntrospectionOnly;
        }

        public void setRequireJwtIntrospectionOnly(boolean requireJwtIntrospectionOnly) {
            this.requireJwtIntrospectionOnly = requireJwtIntrospectionOnly;
        }

        public Optional<SignatureAlgorithm> getSignatureAlgorithm() {
            return signatureAlgorithm;
        }

        public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
            this.signatureAlgorithm = Optional.of(signatureAlgorithm);
        }

        public Optional<String> getCustomizerName() {
            return customizerName;
        }

        public void setCustomizerName(String customizerName) {
            this.customizerName = Optional.of(customizerName);
        }

        public boolean isSubjectRequired() {
            return subjectRequired;
        }

        public void setSubjectRequired(boolean subjectRequired) {
            this.subjectRequired = subjectRequired;
        }

        public String getAuthorizationScheme() {
            return authorizationScheme;
        }

        public void setAuthorizationScheme(String authorizationScheme) {
            this.authorizationScheme = authorizationScheme;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.Token mapping) {
            issuer = mapping.issuer();
            audience = mapping.audience();
            subjectRequired = mapping.subjectRequired();
            requiredClaims = mapping.requiredClaims();
            tokenType = mapping.tokenType();
            lifespanGrace = mapping.lifespanGrace();
            age = mapping.age();
            issuedAtRequired = mapping.issuedAtRequired();
            principalClaim = mapping.principalClaim();
            refreshExpired = mapping.refreshExpired();
            refreshTokenTimeSkew = mapping.refreshTokenTimeSkew();
            forcedJwkRefreshInterval = mapping.forcedJwkRefreshInterval();
            header = mapping.header();
            authorizationScheme = mapping.authorizationScheme();
            signatureAlgorithm = mapping.signatureAlgorithm().map(Enum::toString).map(SignatureAlgorithm::valueOf);
            decryptionKeyLocation = mapping.decryptionKeyLocation();
            decryptIdToken = mapping.decryptIdToken();
            decryptAccessToken = mapping.decryptAccessToken();
            allowJwtIntrospection = mapping.allowJwtIntrospection();
            requireJwtIntrospectionOnly = mapping.requireJwtIntrospectionOnly();
            allowOpaqueTokenIntrospection = mapping.allowOpaqueTokenIntrospection();
            customizerName = mapping.customizerName();
            verifyAccessTokenWithUserInfo = mapping.verifyAccessTokenWithUserInfo();
            binding.addConfigMappingValues(mapping.binding());
        }

        @Override
        public Optional<String> issuer() {
            return issuer;
        }

        @Override
        public Optional<List<String>> audience() {
            return audience;
        }

        @Override
        public boolean subjectRequired() {
            return subjectRequired;
        }

        @Override
        public Map<String, Set<String>> requiredClaims() {
            return requiredClaims;
        }

        @Override
        public Optional<String> tokenType() {
            return tokenType;
        }

        @Override
        public OptionalInt lifespanGrace() {
            return lifespanGrace;
        }

        @Override
        public Optional<Duration> age() {
            return age;
        }

        @Override
        public boolean issuedAtRequired() {
            return issuedAtRequired;
        }

        @Override
        public Optional<String> principalClaim() {
            return principalClaim;
        }

        @Override
        public boolean refreshExpired() {
            return refreshExpired;
        }

        @Override
        public Optional<Duration> refreshTokenTimeSkew() {
            return refreshTokenTimeSkew;
        }

        @Override
        public Duration forcedJwkRefreshInterval() {
            return forcedJwkRefreshInterval;
        }

        @Override
        public Optional<String> header() {
            return header;
        }

        @Override
        public String authorizationScheme() {
            return authorizationScheme;
        }

        @Override
        public Optional<io.quarkus.oidc.runtime.OidcTenantConfig.SignatureAlgorithm> signatureAlgorithm() {
            return signatureAlgorithm.map(Enum::toString)
                    .map(io.quarkus.oidc.runtime.OidcTenantConfig.SignatureAlgorithm::valueOf);
        }

        @Override
        public Optional<String> decryptionKeyLocation() {
            return decryptionKeyLocation;
        }

        @Override
        public Optional<Boolean> decryptIdToken() {
            return decryptIdToken;
        }

        @Override
        public boolean decryptAccessToken() {
            return decryptAccessToken;
        }

        @Override
        public boolean allowJwtIntrospection() {
            return allowJwtIntrospection;
        }

        @Override
        public boolean requireJwtIntrospectionOnly() {
            return requireJwtIntrospectionOnly;
        }

        @Override
        public boolean allowOpaqueTokenIntrospection() {
            return allowOpaqueTokenIntrospection;
        }

        @Override
        public Optional<String> customizerName() {
            return customizerName;
        }

        @Override
        public Optional<Boolean> verifyAccessTokenWithUserInfo() {
            return verifyAccessTokenWithUserInfo;
        }
    }

    /**
     * @deprecated use the {@link TokenConfigBuilder.BindingConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public static class Binding implements io.quarkus.oidc.runtime.OidcTenantConfig.Binding {

        /**
         * If a bearer access token must be bound to the client mTLS certificate.
         * It requires that JWT tokens must contain a confirmation `cnf` claim with a SHA256 certificate thumbprint
         * matching the client mTLS certificate's SHA256 certificate thumbprint.
         * <p>
         * For opaque tokens, SHA256 certificate thumbprint must be returned in their introspection response.
         */
        public boolean certificate = false;

        @Override
        public boolean certificate() {
            return certificate;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.Binding mapping) {
            certificate = mapping.certificate();
        }
    }

    @Deprecated(since = "3.25", forRemoval = true)
    ResourceMetadata resourceMetadata = new ResourceMetadata();

    @Deprecated(since = "3.25", forRemoval = true)
    public static class ResourceMetadata implements io.quarkus.oidc.runtime.OidcTenantConfig.ResourceMetadata {

        public boolean enabled;
        public Optional<String> resource = Optional.empty();
        public boolean forceHttpsScheme = true;

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public Optional<String> resource() {
            return resource;
        }

        @Override
        public boolean forceHttpsScheme() {
            return forceHttpsScheme;
        }

        private void addConfigMappingValues(io.quarkus.oidc.runtime.OidcTenantConfig.ResourceMetadata mapping) {
            enabled = mapping.enabled();
            resource = mapping.resource();
            forceHttpsScheme = mapping.forceHttpsScheme();
        }
    }

    public static enum ApplicationType {
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
     *
     * @deprecated use the {@link #provider()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Provider> provider = Optional.empty();

    public static enum Provider {
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

    /**
     * @deprecated use the {@link #provider()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Provider> getProvider() {
        return provider;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setProvider(Provider provider) {
        this.provider = Optional.of(provider);
    }

    /**
     * @deprecated use the {@link #applicationType()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<ApplicationType> getApplicationType() {
        return applicationType;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setApplicationType(ApplicationType type) {
        this.applicationType = Optional.of(type);
    }

    /**
     * @deprecated use the {@link #allowTokenIntrospectionCache()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public boolean isAllowTokenIntrospectionCache() {
        return allowTokenIntrospectionCache();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setAllowTokenIntrospectionCache(boolean allowTokenIntrospectionCache) {
        this.allowTokenIntrospectionCache = allowTokenIntrospectionCache;
    }

    /**
     * @deprecated use the {@link #allowUserInfoCache()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public boolean isAllowUserInfoCache() {
        return allowUserInfoCache();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setAllowUserInfoCache(boolean allowUserInfoCache) {
        this.allowUserInfoCache = allowUserInfoCache;
    }

    /**
     * @deprecated use the {@link #cacheUserInfoInIdtoken()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public Optional<Boolean> isCacheUserInfoInIdtoken() {
        return cacheUserInfoInIdtoken();
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setCacheUserInfoInIdtoken(boolean cacheUserInfoInIdtoken) {
        this.cacheUserInfoInIdtoken = Optional.of(cacheUserInfoInIdtoken);
    }

    /**
     * @deprecated use the {@link #introspectionCredentials()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public IntrospectionCredentials getIntrospectionCredentials() {
        return introspectionCredentials;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setIntrospectionCredentials(IntrospectionCredentials introspectionCredentials) {
        this.introspectionCredentials = introspectionCredentials;
    }

    /**
     * @deprecated use the {@link #codeGrant()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public CodeGrant getCodeGrant() {
        return codeGrant;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setCodeGrant(CodeGrant codeGrant) {
        this.codeGrant = codeGrant;
    }

    /**
     * @deprecated use the {@link #certificateChain()} method instead
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public CertificateChain getCertificateChain() {
        return certificateChain;
    }

    /**
     * @deprecated build this config with the {@link OidcTenantConfigBuilder} builder
     */
    @Deprecated(since = "3.18", forRemoval = true)
    public void setCertificateChain(CertificateChain certificateChain) {
        this.certificateChain = certificateChain;
    }

    @Override
    public Optional<String> tenantId() {
        return tenantId;
    }

    @Override
    public boolean tenantEnabled() {
        return tenantEnabled;
    }

    @Override
    public Optional<io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType> applicationType() {
        return applicationType.map(Enum::toString).map(io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType::valueOf);
    }

    @Override
    public Optional<String> authorizationPath() {
        return authorizationPath;
    }

    @Override
    public Optional<String> userInfoPath() {
        return userInfoPath;
    }

    @Override
    public Optional<String> introspectionPath() {
        return introspectionPath;
    }

    @Override
    public Optional<String> jwksPath() {
        return jwksPath;
    }

    @Override
    public Optional<String> endSessionPath() {
        return endSessionPath;
    }

    @Override
    public Optional<List<String>> tenantPaths() {
        return tenantPaths;
    }

    @Override
    public Optional<String> publicKey() {
        return publicKey;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.IntrospectionCredentials introspectionCredentials() {
        return introspectionCredentials;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.Roles roles() {
        return roles;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.Token token() {
        return token;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.Logout logout() {
        return logout;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.ResourceMetadata resourceMetadata() {
        return resourceMetadata;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.CertificateChain certificateChain() {
        return certificateChain;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.Authentication authentication() {
        return authentication;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.CodeGrant codeGrant() {
        return codeGrant;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager tokenStateManager() {
        return tokenStateManager;
    }

    @Override
    public boolean allowTokenIntrospectionCache() {
        return allowTokenIntrospectionCache;
    }

    @Override
    public boolean allowUserInfoCache() {
        return allowUserInfoCache;
    }

    @Override
    public Optional<Boolean> cacheUserInfoInIdtoken() {
        return cacheUserInfoInIdtoken;
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.Jwks jwks() {
        return jwks;
    }

    @Override
    public Optional<io.quarkus.oidc.runtime.OidcTenantConfig.Provider> provider() {
        return provider.map(Enum::toString).map(io.quarkus.oidc.runtime.OidcTenantConfig.Provider::valueOf);
    }

    /**
     * Creates {@link OidcTenantConfigBuilder} builder populated with documented default values.
     *
     * @return OidcTenantConfigBuilder builder
     */
    public static OidcTenantConfigBuilder builder() {
        return new OidcTenantConfigBuilder();
    }

    /**
     * Creates {@link OidcTenantConfigBuilder} builder from the existing {@link io.quarkus.oidc.runtime.OidcTenantConfig}
     *
     * @param mapping existing io.quarkus.oidc.runtime.OidcTenantConfig
     */
    public static OidcTenantConfigBuilder builder(io.quarkus.oidc.runtime.OidcTenantConfig mapping) {
        return new OidcTenantConfigBuilder(mapping);
    }

    /**
     * Creates {@link OidcTenantConfig} from the {@code mapping}. This method is more efficient than
     * the {@link #builder()} method if you don't need to modify the {@code mapping}.
     *
     * @param mapping existing io.quarkus.oidc.runtime.OidcTenantConfig
     * @return OidcTenantConfig
     */
    public static OidcTenantConfig of(io.quarkus.oidc.runtime.OidcTenantConfig mapping) {
        return new OidcTenantConfig(mapping);
    }

    /**
     * Creates {@link OidcTenantConfigBuilder} builder populated with documented default values and the provided base URL.
     *
     * @param authServerUrl {@link #authServerUrl()}
     * @return OidcTenantConfigBuilder builder
     */
    public static OidcTenantConfigBuilder authServerUrl(String authServerUrl) {
        return builder().authServerUrl(authServerUrl);
    }

    /**
     * Creates {@link OidcTenantConfigBuilder} builder populated with documented default values and the provided client
     * registration path.
     *
     * @param registrationPath {@link #registrationPath()}
     * @return OidcTenantConfigBuilder builder
     */
    public static OidcTenantConfigBuilder registrationPath(String registrationPath) {
        return builder().registrationPath(registrationPath);
    }

    /**
     * Creates {@link OidcTenantConfigBuilder} builder populated with documented default values and the provided token path.
     *
     * @param tokenPath {@link #tokenPath()}
     * @return OidcTenantConfigBuilder builder
     */
    public static OidcTenantConfigBuilder tokenPath(String tokenPath) {
        return builder().tokenPath(tokenPath);
    }

}
