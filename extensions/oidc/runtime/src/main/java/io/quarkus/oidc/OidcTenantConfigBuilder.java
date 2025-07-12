package io.quarkus.oidc;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfigBuilder;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.runtime.OidcTenantConfig.Authentication;
import io.quarkus.oidc.runtime.OidcTenantConfig.CertificateChain;
import io.quarkus.oidc.runtime.OidcTenantConfig.CodeGrant;
import io.quarkus.oidc.runtime.OidcTenantConfig.IntrospectionCredentials;
import io.quarkus.oidc.runtime.OidcTenantConfig.Jwks;
import io.quarkus.oidc.runtime.OidcTenantConfig.Logout;
import io.quarkus.oidc.runtime.OidcTenantConfig.Provider;
import io.quarkus.oidc.runtime.OidcTenantConfig.ResourceMetadata;
import io.quarkus.oidc.runtime.OidcTenantConfig.Roles;
import io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.runtime.OidcTenantConfig.Token;
import io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager;
import io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.EncryptionAlgorithm;
import io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.Strategy;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.builders.AuthenticationConfigBuilder;
import io.quarkus.oidc.runtime.builders.LogoutConfigBuilder;
import io.quarkus.oidc.runtime.builders.TokenConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Builder for the {@link io.quarkus.oidc.OidcTenantConfig}. This builder is not thread-safe.
 */
public final class OidcTenantConfigBuilder extends OidcClientCommonConfigBuilder<OidcTenantConfigBuilder> {

    /**
     * {@link io.quarkus.oidc.OidcTenantConfig} with documented defaults.
     * Cached here so that we avoid building the SmallRye Config again and again when no-args builder constructors
     * are used.
     */
    private static volatile OidcTenantConfig configWithDefaults = null;

    private static final class OidcTenantConfigImpl extends OidcClientCommonConfigImpl implements OidcTenantConfig {
        private final Optional<String> tenantId;
        private final boolean tenantEnabled;
        private final Optional<ApplicationType> applicationType;
        private final Optional<String> authorizationPath;
        private final Optional<String> userInfoPath;
        private final Optional<String> introspectionPath;
        private final Optional<String> jwksPath;
        private final Optional<String> endSessionPath;
        private final Optional<List<String>> tenantPaths;
        private final Optional<String> publicKey;
        private final IntrospectionCredentials introspectionCredentials;
        private final Roles roles;
        private final Token token;
        private final Logout logout;
        private final ResourceMetadata resourceMetadata;
        private final CertificateChain certificateChain;
        private final Authentication authentication;
        private final CodeGrant codeGrant;
        private final TokenStateManager tokenStateManager;
        private final boolean allowTokenIntrospectionCache;
        private final boolean allowUserInfoCache;
        private final Optional<Boolean> cacheUserInfoInIdtoken;
        private final Jwks jwks;
        private final Optional<Provider> provider;

        private OidcTenantConfigImpl(OidcTenantConfigBuilder builder) {
            super(builder);
            this.tenantId = builder.tenantId;
            this.tenantEnabled = builder.tenantEnabled;
            this.applicationType = builder.applicationType;
            this.authorizationPath = builder.authorizationPath;
            this.userInfoPath = builder.userInfoPath;
            this.introspectionPath = builder.introspectionPath;
            this.jwksPath = builder.jwksPath;
            this.endSessionPath = builder.endSessionPath;
            this.tenantPaths = builder.tenantPaths.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(builder.tenantPaths));
            this.publicKey = builder.publicKey;
            this.introspectionCredentials = builder.introspectionCredentials;
            this.roles = builder.roles;
            this.token = builder.token;
            this.logout = builder.logout;
            this.certificateChain = builder.certificateChain;
            this.resourceMetadata = builder.resourceMetadata;
            this.authentication = builder.authentication;
            this.codeGrant = builder.codeGrant;
            this.tokenStateManager = builder.tokenStateManager;
            this.allowTokenIntrospectionCache = builder.allowTokenIntrospectionCache;
            this.allowUserInfoCache = builder.allowUserInfoCache;
            this.cacheUserInfoInIdtoken = builder.cacheUserInfoInIdtoken;
            this.jwks = builder.jwks;
            this.provider = builder.provider;
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
        public Optional<ApplicationType> applicationType() {
            return applicationType;
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
        public IntrospectionCredentials introspectionCredentials() {
            return introspectionCredentials;
        }

        @Override
        public Roles roles() {
            return roles;
        }

        @Override
        public Token token() {
            return token;
        }

        @Override
        public ResourceMetadata resourceMetadata() {
            return resourceMetadata;
        }

        @Override
        public Logout logout() {
            return logout;
        }

        @Override
        public CertificateChain certificateChain() {
            return certificateChain;
        }

        @Override
        public Authentication authentication() {
            return authentication;
        }

        @Override
        public CodeGrant codeGrant() {
            return codeGrant;
        }

        @Override
        public TokenStateManager tokenStateManager() {
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
        public Jwks jwks() {
            return jwks;
        }

        @Override
        public Optional<Provider> provider() {
            return provider;
        }
    }

    private Optional<String> tenantId;
    private boolean tenantEnabled;
    private Optional<ApplicationType> applicationType;
    private Optional<String> authorizationPath;
    private Optional<String> userInfoPath;
    private Optional<String> introspectionPath;
    private Optional<String> jwksPath;
    private Optional<String> endSessionPath;
    private final List<String> tenantPaths = new ArrayList<>();
    private Optional<String> publicKey;
    private IntrospectionCredentials introspectionCredentials;
    private Roles roles;
    private ResourceMetadata resourceMetadata;
    private CertificateChain certificateChain;
    private CodeGrant codeGrant;
    private TokenStateManager tokenStateManager;
    private boolean allowTokenIntrospectionCache;
    private boolean allowUserInfoCache;
    private Optional<Boolean> cacheUserInfoInIdtoken;
    private Jwks jwks;
    private Optional<Provider> provider;
    private Logout logout;
    private Token token;
    private Authentication authentication;

    public OidcTenantConfigBuilder() {
        this(getConfigWithDefaults());
    }

    public OidcTenantConfigBuilder(OidcTenantConfig mapping) {
        super(Objects.requireNonNull(mapping));
        this.tenantId = mapping.tenantId();
        this.tenantEnabled = mapping.tenantEnabled();
        this.applicationType = mapping.applicationType();
        this.authorizationPath = mapping.authorizationPath();
        this.userInfoPath = mapping.userInfoPath();
        this.introspectionPath = mapping.introspectionPath();
        this.jwksPath = mapping.jwksPath();
        this.endSessionPath = mapping.endSessionPath();
        this.publicKey = mapping.publicKey();
        this.introspectionCredentials = mapping.introspectionCredentials();
        this.roles = mapping.roles();
        this.token = mapping.token();
        this.logout = mapping.logout();
        this.certificateChain = mapping.certificateChain();
        this.resourceMetadata = mapping.resourceMetadata();
        this.authentication = mapping.authentication();
        this.codeGrant = mapping.codeGrant();
        this.tokenStateManager = mapping.tokenStateManager();
        this.allowTokenIntrospectionCache = mapping.allowTokenIntrospectionCache();
        this.allowUserInfoCache = mapping.allowUserInfoCache();
        this.cacheUserInfoInIdtoken = mapping.cacheUserInfoInIdtoken();
        this.jwks = mapping.jwks();
        this.provider = mapping.provider();
        if (mapping.tenantPaths().isPresent()) {
            this.tenantPaths.addAll(mapping.tenantPaths().get());
        }
    }

    @Override
    protected OidcTenantConfigBuilder getBuilder() {
        return this;
    }

    /**
     * @param tenantId {@link OidcTenantConfig#tenantId()}
     * @return this builder
     */
    public OidcTenantConfigBuilder tenantId(String tenantId) {
        this.tenantId = Optional.ofNullable(tenantId);
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig#tenantEnabled()} to false.
     *
     * @return this builder
     */
    public OidcTenantConfigBuilder disableTenant() {
        return tenantEnabled(false);
    }

    /**
     * Sets {@link OidcTenantConfig#tenantEnabled()} to true.
     *
     * @return this builder
     */
    public OidcTenantConfigBuilder enableTenant() {
        return tenantEnabled(true);
    }

    /**
     * @param tenantEnabled {@link OidcTenantConfig#tenantEnabled()}
     * @return this builder
     */
    public OidcTenantConfigBuilder tenantEnabled(boolean tenantEnabled) {
        this.tenantEnabled = tenantEnabled;
        return this;
    }

    /**
     * @param applicationType {@link OidcTenantConfig#applicationType()}
     * @return this builder
     */
    public OidcTenantConfigBuilder applicationType(ApplicationType applicationType) {
        this.applicationType = Optional.ofNullable(applicationType);
        return this;
    }

    /**
     * @param authorizationPath {@link OidcTenantConfig#authorizationPath()}
     * @return this builder
     */
    public OidcTenantConfigBuilder authorizationPath(String authorizationPath) {
        this.authorizationPath = Optional.ofNullable(authorizationPath);
        return this;
    }

    /**
     * @param userInfoPath {@link OidcTenantConfig#userInfoPath()}
     * @return this builder
     */
    public OidcTenantConfigBuilder userInfoPath(String userInfoPath) {
        this.userInfoPath = Optional.ofNullable(userInfoPath);
        return this;
    }

    /**
     * @param introspectionPath {@link OidcTenantConfig#introspectionPath()}
     * @return this builder
     */
    public OidcTenantConfigBuilder introspectionPath(String introspectionPath) {
        this.introspectionPath = Optional.ofNullable(introspectionPath);
        return this;
    }

    /**
     * @param jwksPath {@link OidcTenantConfig#jwksPath()}
     * @return this builder
     */
    public OidcTenantConfigBuilder jwksPath(String jwksPath) {
        this.jwksPath = Optional.ofNullable(jwksPath);
        return this;
    }

    /**
     * @param endSessionPath {@link OidcTenantConfig#endSessionPath()}
     * @return this builder
     */
    public OidcTenantConfigBuilder endSessionPath(String endSessionPath) {
        this.endSessionPath = Optional.ofNullable(endSessionPath);
        return this;
    }

    /**
     * @param tenantPath {@link OidcTenantConfig#tenantPaths()}
     * @return this builder
     */
    public OidcTenantConfigBuilder tenantPath(String tenantPath) {
        if (tenantPath != null) {
            this.tenantPaths.add(tenantPath);
        }
        return this;
    }

    /**
     * @param tenantPaths {@link OidcTenantConfig#tenantPaths()}
     * @return this builder
     */
    public OidcTenantConfigBuilder tenantPaths(String... tenantPaths) {
        if (tenantPaths != null) {
            this.tenantPaths.addAll(Arrays.asList(tenantPaths));
        }
        return this;
    }

    /**
     * @param tenantPaths {@link OidcTenantConfig#tenantPaths()}
     * @return this builder
     */
    public OidcTenantConfigBuilder tenantPaths(List<String> tenantPaths) {
        if (tenantPaths != null) {
            this.tenantPaths.addAll(tenantPaths);
        }
        return this;
    }

    /**
     * @param publicKey {@link OidcTenantConfig#publicKey()}
     * @return this builder
     */
    public OidcTenantConfigBuilder publicKey(String publicKey) {
        this.publicKey = Optional.ofNullable(publicKey);
        return this;
    }

    /**
     * @param introspectionCredentials {@link OidcTenantConfig#introspectionCredentials()}
     * @return this builder
     */
    public OidcTenantConfigBuilder introspectionCredentials(IntrospectionCredentials introspectionCredentials) {
        this.introspectionCredentials = Objects.requireNonNull(introspectionCredentials);
        return this;
    }

    /**
     * @param name {@link IntrospectionCredentials#name()}
     * @param secret {@link IntrospectionCredentials#secret()}
     * @return this builder
     */
    public OidcTenantConfigBuilder introspectionCredentials(String name, String secret) {
        return new IntrospectionCredentialsBuilder(this).name(name).secret(secret).end();
    }

    /**
     * @return builder for the {@link OidcTenantConfig#introspectionCredentials()}
     */
    public IntrospectionCredentialsBuilder introspectionCredentials() {
        return new IntrospectionCredentialsBuilder(this);
    }

    /**
     * @param roles {@link OidcTenantConfig#roles()}
     * @return this builder
     */
    public OidcTenantConfigBuilder roles(Roles roles) {
        this.roles = Objects.requireNonNull(roles);
        return this;
    }

    /**
     * @return {@link OidcTenantConfig#roles()} builder
     */
    public RolesBuilder roles() {
        return new RolesBuilder(this);
    }

    /**
     * @param source {@link Roles#source()}
     * @param roleClaimPaths {@link Roles#roleClaimPath()}
     * @return this builder
     */
    public OidcTenantConfigBuilder roles(Source source, String... roleClaimPaths) {
        return roles().source(source).roleClaimPath(roleClaimPaths).end();
    }

    /**
     * @param token {@link OidcTenantConfig#token()}
     * @return this builder
     */
    public OidcTenantConfigBuilder token(Token token) {
        this.token = Objects.requireNonNull(token);
        return this;
    }

    /**
     * @param principalClaim {@link Token#principalClaim()}
     * @return this builder
     */
    public OidcTenantConfigBuilder token(String principalClaim) {
        return token().principalClaim(principalClaim).end();
    }

    /**
     * @return builder for the {@link OidcTenantConfig#token()}
     */
    public TokenConfigBuilder token() {
        return new TokenConfigBuilder(this);
    }

    /**
     * @param logout {@link OidcTenantConfig#logout()}
     * @return this builder
     */
    public OidcTenantConfigBuilder logout(Logout logout) {
        this.logout = Objects.requireNonNull(logout);
        return this;
    }

    /**
     * Creates builder for the {@link OidcTenantConfig#logout()}.
     *
     * @return LogoutConfigBuilder
     */
    public LogoutConfigBuilder logout() {
        return new LogoutConfigBuilder(this);
    }

    /**
     * @param resourceMetadata {@link OidcTenantConfig#resourceMetadata()}
     * @return this builder
     */
    public OidcTenantConfigBuilder resourceMetadata(ResourceMetadata resourceMetadata) {
        this.resourceMetadata = Objects.requireNonNull(resourceMetadata);
        return this;
    }

    /**
     * Creates builder for the {@link OidcTenantConfig#resourceMetadata()}.
     *
     * @return ResourceMetadataConfigBuilder
     */
    public ResourceMetadataBuilder resourceMetadata() {
        return new ResourceMetadataBuilder(this);
    }

    /**
     * @param certificateChain {@link OidcTenantConfig#certificateChain()}
     * @return this builder
     */
    public OidcTenantConfigBuilder certificateChain(CertificateChain certificateChain) {
        this.certificateChain = Objects.requireNonNull(certificateChain);
        return this;
    }

    /**
     * @return builder for the {@link OidcTenantConfig#certificateChain()}
     */
    public CertificateChainBuilder certificateChain() {
        return new CertificateChainBuilder(this);
    }

    /**
     * @param authentication {@link OidcTenantConfig#authentication()}
     * @return this builder
     */
    public OidcTenantConfigBuilder authentication(Authentication authentication) {
        this.authentication = Objects.requireNonNull(authentication);
        return this;
    }

    /**
     * @return builder for the {@link OidcTenantConfig#authentication()}.
     */
    public AuthenticationConfigBuilder authentication() {
        return new AuthenticationConfigBuilder(this);
    }

    /**
     * @param headers {@link CodeGrant#headers()}
     * @param extraParams {@link CodeGrant#extraParams()}
     * @return this builder
     */
    public OidcTenantConfigBuilder codeGrant(Map<String, String> headers, Map<String, String> extraParams) {
        return codeGrant().headers(headers).extraParams(extraParams).end();
    }

    /**
     * @param headers {@link CodeGrant#headers()}
     * @return this builder
     */
    public OidcTenantConfigBuilder codeGrant(Map<String, String> headers) {
        return codeGrant().headers(headers).end();
    }

    /**
     * @return builder for the {@link OidcTenantConfig#codeGrant()}
     */
    public CodeGrantBuilder codeGrant() {
        return new CodeGrantBuilder(this);
    }

    /**
     * @param codeGrant {@link OidcTenantConfig#codeGrant()}
     * @return this builder
     */
    public OidcTenantConfigBuilder codeGrant(CodeGrant codeGrant) {
        this.codeGrant = Objects.requireNonNull(codeGrant);
        return this;
    }

    /**
     * @param tokenStateManager {@link OidcTenantConfig#tokenStateManager()}
     * @return this builder
     */
    public OidcTenantConfigBuilder tokenStateManager(TokenStateManager tokenStateManager) {
        this.tokenStateManager = Objects.requireNonNull(tokenStateManager);
        return this;
    }

    /**
     * @return builder for the {@link OidcTenantConfig#tokenStateManager()}
     */
    public TokenStateManagerBuilder tokenStateManager() {
        return new TokenStateManagerBuilder(this);
    }

    /**
     * Sets {@link OidcTenantConfig#allowTokenIntrospectionCache()} to true.
     *
     * @return this builder
     */
    public OidcTenantConfigBuilder allowTokenIntrospectionCache() {
        return allowTokenIntrospectionCache(true);
    }

    /**
     * @param allowTokenIntrospectionCache {@link OidcTenantConfig#allowTokenIntrospectionCache()}
     * @return this builder
     */
    public OidcTenantConfigBuilder allowTokenIntrospectionCache(boolean allowTokenIntrospectionCache) {
        this.allowTokenIntrospectionCache = allowTokenIntrospectionCache;
        return this;
    }

    /**
     * @param allowUserInfoCache {@link OidcTenantConfig#allowUserInfoCache()}
     * @return this builder
     */
    public OidcTenantConfigBuilder allowUserInfoCache(boolean allowUserInfoCache) {
        this.allowUserInfoCache = allowUserInfoCache;
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig#allowUserInfoCache()} to true.
     *
     * @return this builder
     */
    public OidcTenantConfigBuilder allowUserInfoCache() {
        return allowUserInfoCache(true);
    }

    /**
     * @param cacheUserInfoInIdtoken {@link OidcTenantConfig#cacheUserInfoInIdtoken()}
     * @return this builder
     */
    public OidcTenantConfigBuilder cacheUserInfoInIdtoken(boolean cacheUserInfoInIdtoken) {
        this.cacheUserInfoInIdtoken = Optional.of(cacheUserInfoInIdtoken);
        return this;
    }

    /**
     * Sets {@link OidcTenantConfig#cacheUserInfoInIdtoken()} to true.
     *
     * @return this builder
     */
    public OidcTenantConfigBuilder cacheUserInfoInIdtoken() {
        return cacheUserInfoInIdtoken(true);
    }

    /**
     * @param jwks {@link OidcTenantConfig#jwks()}
     * @return this builder
     */
    public OidcTenantConfigBuilder jwks(Jwks jwks) {
        this.jwks = Objects.requireNonNull(jwks);
        return this;
    }

    /**
     * @return builder for the {@link OidcTenantConfig#jwks()}
     */
    public JwksBuilder jwks() {
        return new JwksBuilder(this);
    }

    /**
     * @param provider {@link OidcTenantConfig#provider()}
     * @return this builder
     */
    public OidcTenantConfigBuilder provider(Provider provider) {
        this.provider = Optional.ofNullable(provider);
        return this;
    }

    /**
     * @return build {@link io.quarkus.oidc.OidcTenantConfig}
     */
    public io.quarkus.oidc.OidcTenantConfig build() {
        if (tenantId.isEmpty()) {
            tenantId(OidcUtils.DEFAULT_TENANT_ID);
        }
        var mapping = new OidcTenantConfigImpl(this);
        return io.quarkus.oidc.OidcTenantConfig.of(mapping);
    }

    /**
     * Builder for the {@link IntrospectionCredentials}.
     */
    public static final class IntrospectionCredentialsBuilder {

        private record IntrospectionCredentialsImpl(Optional<String> name, Optional<String> secret,
                boolean includeClientId) implements IntrospectionCredentials {
        }

        private final OidcTenantConfigBuilder builder;
        private Optional<String> name;
        private Optional<String> secret;
        private boolean includeClientId;

        public IntrospectionCredentialsBuilder() {
            this(new OidcTenantConfigBuilder());
        }

        public IntrospectionCredentialsBuilder(OidcTenantConfigBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
            this.name = builder.introspectionCredentials.name();
            this.secret = builder.introspectionCredentials.secret();
            this.includeClientId = builder.introspectionCredentials.includeClientId();
        }

        /**
         * @param name {@link IntrospectionCredentials#name()}
         * @return this builder
         */
        public IntrospectionCredentialsBuilder name(String name) {
            this.name = Optional.ofNullable(name);
            return this;
        }

        /**
         * @param secret {@link IntrospectionCredentials#secret()}
         * @return this builder
         */
        public IntrospectionCredentialsBuilder secret(String secret) {
            this.secret = Optional.ofNullable(secret);
            return this;
        }

        /**
         * @param includeClientId {@link IntrospectionCredentials#includeClientId()}
         * @return this builder
         */
        public IntrospectionCredentialsBuilder includeClientId(boolean includeClientId) {
            this.includeClientId = includeClientId;
            return this;
        }

        /**
         * @return OidcTenantConfigBuilder builder
         */
        public OidcTenantConfigBuilder end() {
            return builder.introspectionCredentials(build());
        }

        /**
         * @return built IntrospectionCredentials
         */
        public IntrospectionCredentials build() {
            return new IntrospectionCredentialsImpl(name, secret, includeClientId);
        }
    }

    /**
     * Builder for the {@link CertificateChain}.
     */
    public static final class CertificateChainBuilder {

        private record CertificateChainImpl(Optional<String> leafCertificateName, Optional<Path> trustStoreFile,
                Optional<String> trustStorePassword, Optional<String> trustStoreCertAlias,
                Optional<String> trustStoreFileType) implements CertificateChain {
        }

        private final OidcTenantConfigBuilder builder;
        private Optional<String> leafCertificateName;
        private Optional<Path> trustStoreFile;
        private Optional<String> trustStorePassword;
        private Optional<String> trustStoreCertAlias;
        private Optional<String> trustStoreFileType;

        public CertificateChainBuilder() {
            this(new OidcTenantConfigBuilder());
        }

        public CertificateChainBuilder(OidcTenantConfigBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
            var certificateChain = builder.certificateChain;
            this.leafCertificateName = certificateChain.leafCertificateName();
            this.trustStoreFile = certificateChain.trustStoreFile();
            this.trustStorePassword = certificateChain.trustStorePassword();
            this.trustStoreCertAlias = certificateChain.trustStoreCertAlias();
            this.trustStoreFileType = certificateChain.trustStoreFileType();
        }

        /**
         * @param leafCertificateName {@link CertificateChain#leafCertificateName()}
         * @return this builder
         */
        public CertificateChainBuilder leafCertificateName(String leafCertificateName) {
            this.leafCertificateName = Optional.ofNullable(leafCertificateName);
            return this;
        }

        /**
         * @param trustStoreFile {@link CertificateChain#trustStoreFile()}
         * @return this builder
         */
        public CertificateChainBuilder trustStoreFile(Path trustStoreFile) {
            this.trustStoreFile = Optional.ofNullable(trustStoreFile);
            return this;
        }

        /**
         * @param trustStorePassword {@link CertificateChain#trustStorePassword()}
         * @return this builder
         */
        public CertificateChainBuilder trustStorePassword(String trustStorePassword) {
            this.trustStorePassword = Optional.ofNullable(trustStorePassword);
            return this;
        }

        /**
         * @param trustStoreCertAlias {@link CertificateChain#trustStoreCertAlias()}
         * @return this builder
         */
        public CertificateChainBuilder trustStoreCertAlias(String trustStoreCertAlias) {
            this.trustStoreCertAlias = Optional.ofNullable(trustStoreCertAlias);
            return this;
        }

        /**
         * @param trustStoreFileType {@link CertificateChain#trustStoreFileType()}
         * @return this builder
         */
        public CertificateChainBuilder trustStoreFileType(String trustStoreFileType) {
            this.trustStoreFileType = Optional.ofNullable(trustStoreFileType);
            return this;
        }

        /**
         * @return builds a new {@link CertificateChain} and return the {@link OidcTenantConfigBuilder} builder
         */
        public OidcTenantConfigBuilder end() {
            return builder.certificateChain(build());
        }

        /**
         * @return builds new {@link CertificateChain}
         */
        public CertificateChain build() {
            return new CertificateChainImpl(leafCertificateName, trustStoreFile, trustStorePassword, trustStoreCertAlias,
                    trustStoreFileType);
        }
    }

    /**
     * Builder for the {@link IntrospectionCredentials}.
     */
    public static final class ResourceMetadataBuilder {

        private record ResourceMetadataImpl(boolean enabled, Optional<String> resource,
                boolean forceHttpsScheme) implements ResourceMetadata {
        }

        private final OidcTenantConfigBuilder builder;
        private boolean enabled;
        private Optional<String> resource;
        private boolean forceHttpsScheme;

        public ResourceMetadataBuilder() {
            this(new OidcTenantConfigBuilder());
        }

        public ResourceMetadataBuilder(OidcTenantConfigBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
            this.enabled = builder.resourceMetadata.enabled();
            this.resource = builder.resourceMetadata.resource();
            this.forceHttpsScheme = builder.resourceMetadata.forceHttpsScheme();
        }

        /**
         * {@link ResourceMetadata#enabled()}
         *
         * @return this builder
         */
        public ResourceMetadataBuilder enabled() {
            return enabled(true);
        }

        /**
         * @param enabled {@link ResourceMetadata#enabled()}
         * @return this builder
         */
        public ResourceMetadataBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * @param resource {@link ResourceMetadata#resource()}
         * @return this builder
         */
        public ResourceMetadataBuilder resource(String resource) {
            this.resource = Optional.ofNullable(resource);
            return this;
        }

        /**
         * forceHttpsScheme {@link ResourceMetadata#forceHttpsScheme()}
         *
         * @return this builder
         */
        public ResourceMetadataBuilder forceHttpsScheme() {
            return forceHttpsScheme(true);
        }

        /**
         * @param forceHttpsScheme {@link ResourceMetadata#forceHttpsScheme()}
         * @return this builder
         */
        public ResourceMetadataBuilder forceHttpsScheme(boolean forceHttpsScheme) {
            this.forceHttpsScheme = forceHttpsScheme;
            return this;
        }

        /**
         * @return OidcTenantConfigBuilder builder
         */
        public OidcTenantConfigBuilder end() {
            return builder.resourceMetadata(build());
        }

        /**
         * @return built ResourceMetadata
         */
        public ResourceMetadata build() {
            return new ResourceMetadataImpl(enabled, resource, forceHttpsScheme);
        }
    }

    /**
     * Builder for the {@link Roles}.
     */
    public static final class RolesBuilder {

        private record RolesImpl(Optional<List<String>> roleClaimPath, Optional<String> roleClaimSeparator,
                Optional<Source> source) implements Roles {
        }

        private final OidcTenantConfigBuilder builder;
        private final List<String> roleClaimPath = new ArrayList<>();
        private Optional<String> roleClaimSeparator;
        private Optional<Source> source;

        public RolesBuilder() {
            this(new OidcTenantConfigBuilder());
        }

        public RolesBuilder(OidcTenantConfigBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
            this.roleClaimSeparator = builder.roles.roleClaimSeparator();
            this.source = builder.roles.source();
            if (builder.roles.roleClaimPath().isPresent()) {
                this.roleClaimPath.addAll(builder.roles.roleClaimPath().get());
            }
        }

        /**
         * @param separator {@link Roles#roleClaimSeparator()}
         * @return this builder
         */
        public RolesBuilder roleClaimSeparator(String separator) {
            this.roleClaimSeparator = Optional.ofNullable(separator);
            return this;
        }

        /**
         * @param source {@link Roles#source()}
         * @return this builder
         */
        public RolesBuilder source(Source source) {
            this.source = Optional.ofNullable(source);
            return this;
        }

        /**
         * @param roleClaimPaths {@link Roles#roleClaimPath()}
         * @return this builder
         */
        public RolesBuilder roleClaimPath(String... roleClaimPaths) {
            if (roleClaimPaths != null) {
                this.roleClaimPath.addAll(Arrays.asList(roleClaimPaths));
            }
            return this;
        }

        /**
         * @param roleClaimPaths {@link Roles#roleClaimPath()}
         * @return this builder
         */
        public RolesBuilder roleClaimPath(List<String> roleClaimPaths) {
            if (roleClaimPaths != null) {
                this.roleClaimPath.addAll(roleClaimPaths);
            }
            return this;
        }

        /**
         * @return OidcTenantConfigBuilder builder
         */
        public OidcTenantConfigBuilder end() {
            return builder.roles(build());
        }

        /**
         * @return built {@link Roles}
         */
        public Roles build() {
            var roleClaimPathOptional = roleClaimPath.isEmpty() ? Optional.<List<String>> empty()
                    : Optional.of(List.copyOf(roleClaimPath));
            return new RolesImpl(roleClaimPathOptional, roleClaimSeparator, source);
        }
    }

    /**
     * Builder for the {@link Jwks}.
     */
    public static final class JwksBuilder {
        private record JwksImpl(boolean resolveEarly, int cacheSize, Duration cacheTimeToLive,
                Optional<Duration> cleanUpTimerInterval, boolean tryAll) implements Jwks {
        }

        private final OidcTenantConfigBuilder builder;
        private boolean resolveEarly;
        private int cacheSize;
        private Duration cacheTimeToLive;
        private Optional<Duration> cleanUpTimerInterval;
        private boolean tryAll;

        public JwksBuilder() {
            this(new OidcTenantConfigBuilder());
        }

        public JwksBuilder(OidcTenantConfigBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
            var jwks = builder.jwks;
            this.resolveEarly = jwks.resolveEarly();
            this.cacheSize = jwks.cacheSize();
            this.cacheTimeToLive = jwks.cacheTimeToLive();
            this.cleanUpTimerInterval = jwks.cleanUpTimerInterval();
            this.tryAll = jwks.tryAll();
        }

        /**
         * Sets {@link Jwks#resolveEarly()} to true.
         *
         * @return this builder
         */
        public JwksBuilder resolveEarly() {
            return resolveEarly(true);
        }

        /**
         * @param resolveEarly {@link Jwks#resolveEarly()}
         * @return this builder
         */
        public JwksBuilder resolveEarly(boolean resolveEarly) {
            this.resolveEarly = resolveEarly;
            return this;
        }

        /**
         * @param cacheSize {@link Jwks#cacheSize()}
         * @return this builder
         */
        public JwksBuilder cacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        /**
         * @param cacheTimeToLive {@link Jwks#cacheTimeToLive()}
         * @return this builder
         */
        public JwksBuilder cacheTimeToLive(Duration cacheTimeToLive) {
            this.cacheTimeToLive = Objects.requireNonNull(cacheTimeToLive);
            return this;
        }

        /**
         * @param cleanUpTimerInterval {@link Jwks#cleanUpTimerInterval()}
         * @return this builder
         */
        public JwksBuilder cleanUpTimerInterval(Duration cleanUpTimerInterval) {
            this.cleanUpTimerInterval = Optional.ofNullable(cleanUpTimerInterval);
            return this;
        }

        /**
         * Sets {@link Jwks#tryAll()} to true.
         *
         * @return this builder
         */
        public JwksBuilder tryAll() {
            return tryAll(true);
        }

        /**
         * @param tryAll {@link Jwks#tryAll()}
         * @return this builder
         */
        public JwksBuilder tryAll(boolean tryAll) {
            this.tryAll = tryAll;
            return this;
        }

        /**
         * @return builds {@link Jwks} and creates {@link OidcTenantConfigBuilder}
         */
        public OidcTenantConfigBuilder end() {
            return builder.jwks(build());
        }

        /**
         * @return builds {@link Jwks}
         */
        public Jwks build() {
            return new JwksImpl(resolveEarly, cacheSize, cacheTimeToLive, cleanUpTimerInterval, tryAll);
        }
    }

    /**
     * Builder for the {@link TokenStateManager}.
     */
    public static final class TokenStateManagerBuilder {

        private record TokenStateManagerImpl(Strategy strategy, boolean splitTokens, boolean encryptionRequired,
                Optional<String> encryptionSecret, EncryptionAlgorithm encryptionAlgorithm) implements TokenStateManager {

        }

        private final OidcTenantConfigBuilder builder;
        private Strategy strategy;
        private boolean splitTokens;
        private boolean encryptionRequired;
        private Optional<String> encryptionSecret;
        private EncryptionAlgorithm encryptionAlgorithm;

        public TokenStateManagerBuilder() {
            this(new OidcTenantConfigBuilder());
        }

        public TokenStateManagerBuilder(OidcTenantConfigBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
            var tokenStateManager = builder.tokenStateManager;
            this.strategy = tokenStateManager.strategy();
            this.splitTokens = tokenStateManager.splitTokens();
            this.encryptionRequired = tokenStateManager.encryptionRequired();
            this.encryptionSecret = tokenStateManager.encryptionSecret();
            this.encryptionAlgorithm = tokenStateManager.encryptionAlgorithm();
        }

        /**
         * @param encryptionAlgorithm {@link TokenStateManager#encryptionAlgorithm()}
         * @return this builder
         */
        public TokenStateManagerBuilder encryptionAlgorithm(EncryptionAlgorithm encryptionAlgorithm) {
            this.encryptionAlgorithm = Objects.requireNonNull(encryptionAlgorithm);
            return this;
        }

        /**
         * @param encryptionSecret {@link TokenStateManager#encryptionSecret()}
         * @return this builder
         */
        public TokenStateManagerBuilder encryptionSecret(String encryptionSecret) {
            this.encryptionSecret = Optional.ofNullable(encryptionSecret);
            return this;
        }

        /**
         * @param strategy {@link TokenStateManager#strategy()}
         * @return this builder
         */
        public TokenStateManagerBuilder strategy(Strategy strategy) {
            this.strategy = Objects.requireNonNull(strategy);
            return this;
        }

        /**
         * Sets {@link TokenStateManager#encryptionRequired()} to true.
         *
         * @return this builder
         */
        public TokenStateManagerBuilder encryptionRequired() {
            return encryptionRequired(true);
        }

        /**
         * @param encryptionRequired {@link TokenStateManager#encryptionRequired()}
         * @return this builder
         */
        public TokenStateManagerBuilder encryptionRequired(boolean encryptionRequired) {
            this.encryptionRequired = encryptionRequired;
            return this;
        }

        /**
         * Sets {@link TokenStateManager#splitTokens()} to true.
         *
         * @return this builder
         */
        public TokenStateManagerBuilder splitTokens() {
            return splitTokens(true);
        }

        /**
         * @param splitTokens {@link TokenStateManager#splitTokens()}
         * @return this builder
         */
        public TokenStateManagerBuilder splitTokens(boolean splitTokens) {
            this.splitTokens = splitTokens;
            return this;
        }

        public OidcTenantConfigBuilder end() {
            return builder.tokenStateManager(build());
        }

        public TokenStateManager build() {
            return new TokenStateManagerImpl(strategy, splitTokens, encryptionRequired, encryptionSecret, encryptionAlgorithm);
        }
    }

    /**
     * Builder for the {@link CodeGrant}.
     */
    public static final class CodeGrantBuilder {

        private record CodeGrantImpl(Map<String, String> extraParams, Map<String, String> headers) implements CodeGrant {
        }

        private final OidcTenantConfigBuilder builder;
        private final Map<String, String> extraParams = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();

        public CodeGrantBuilder() {
            this(new OidcTenantConfigBuilder());
        }

        public CodeGrantBuilder(OidcTenantConfigBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
            var codeGrant = builder.codeGrant;
            extraParams.putAll(codeGrant.extraParams());
            headers.putAll(codeGrant.headers());
        }

        /**
         * @param headerName {@link CodeGrant#headers()} key
         * @param headerValue {@link CodeGrant#headers()} value
         * @return this builder
         */
        public CodeGrantBuilder header(String headerName, String headerValue) {
            Objects.requireNonNull(headerName);
            Objects.requireNonNull(headerValue);
            this.headers.put(headerName, headerValue);
            return this;
        }

        /**
         * @param headers {@link CodeGrant#headers()}
         * @return this builder
         */
        public CodeGrantBuilder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        /**
         * @param extraParamKey {@link CodeGrant#extraParams()} key
         * @param extraParamValue {@link CodeGrant#extraParams()} value
         * @return this builder
         */
        public CodeGrantBuilder extraParam(String extraParamKey, String extraParamValue) {
            Objects.requireNonNull(extraParamKey);
            Objects.requireNonNull(extraParamValue);
            this.extraParams.put(extraParamKey, extraParamValue);
            return this;
        }

        /**
         * @param extraParams {@link CodeGrant#extraParams()}
         * @return this builder
         */
        public CodeGrantBuilder extraParams(Map<String, String> extraParams) {
            if (extraParams != null) {
                this.extraParams.putAll(extraParams);
            }
            return this;
        }

        /**
         * @return builds {@link CodeGrant} and returns {@link OidcTenantConfigBuilder}
         */
        public OidcTenantConfigBuilder end() {
            return builder.codeGrant(build());
        }

        /**
         * @return builds {@link CodeGrant}
         */
        public CodeGrant build() {
            return new CodeGrantImpl(Map.copyOf(extraParams), Map.copyOf(headers));
        }
    }

    private static io.quarkus.oidc.runtime.OidcTenantConfig getConfigWithDefaults() {
        if (configWithDefaults == null) {
            final OidcConfig oidcConfig = new SmallRyeConfigBuilder()
                    .addDiscoveredConverters()
                    .withMapping(OidcConfig.class)
                    .build()
                    .getConfigMapping(OidcConfig.class);
            configWithDefaults = OidcConfig.getDefaultTenant(oidcConfig);
        }
        return configWithDefaults;
    }

    /**
     * @return current {@link Authentication} instance
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * @return current {@link Token} instance
     */
    public Token getToken() {
        return token;
    }

    /**
     * @return current {@link Logout} instance
     */
    public Logout getLogout() {
        return logout;
    }
}
