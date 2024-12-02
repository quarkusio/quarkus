package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.SecurityEvent.AUTH_SERVER_URL;
import static io.quarkus.oidc.SecurityEvent.Type.OIDC_SERVER_AVAILABLE;
import static io.quarkus.oidc.SecurityEvent.Type.OIDC_SERVER_NOT_AVAILABLE;
import static io.quarkus.oidc.runtime.OidcConfig.getDefaultTenant;
import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.CreationException;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcTlsSupport;
import io.quarkus.oidc.common.runtime.config.OidcCommonConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.Strategy;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;

@Recorder
public class OidcRecorder {

    private static final Logger LOG = Logger.getLogger(OidcRecorder.class);
    private static final String SECURITY_EVENTS_ENABLED_CONFIG_KEY = "quarkus.security.events.enabled";

    private static final Set<String> tenantsExpectingServerAvailableEvents = ConcurrentHashMap.newKeySet();
    private static volatile boolean userInfoInjectionPointDetected = false;

    public Supplier<DefaultTokenIntrospectionUserInfoCache> setupTokenCache(OidcConfig config, Supplier<Vertx> vertx) {
        return new Supplier<DefaultTokenIntrospectionUserInfoCache>() {
            @Override
            public DefaultTokenIntrospectionUserInfoCache get() {
                return new DefaultTokenIntrospectionUserInfoCache(config, vertx.get());
            }
        };
    }

    public Supplier<TenantConfigBean> createTenantConfigBean(OidcConfig config, Supplier<Vertx> vertx,
            Supplier<TlsConfigurationRegistry> registrySupplier,
            boolean userInfoInjectionPointDetected) {
        return new Supplier<TenantConfigBean>() {
            @Override
            public TenantConfigBean get() {
                return setup(config, vertx.get(), OidcTlsSupport.of(registrySupplier), userInfoInjectionPointDetected);
            }
        };
    }

    public void initTenantConfigBean() {
        try {
            // makes sure that config of static tenants is validated during app startup and create static tenant contexts
            Arc.container().instance(TenantConfigBean.class).get();
        } catch (CreationException wrapper) {
            if (wrapper.getCause() instanceof RuntimeException runtimeException) {
                // so that users see ConfigurationException etc. without noise
                throw runtimeException;
            }
            throw wrapper;
        }
    }

    public TenantConfigBean setup(OidcConfig config, Vertx vertxValue, OidcTlsSupport tlsSupport,
            boolean userInfoInjectionPointDetected) {
        OidcRecorder.userInfoInjectionPointDetected = userInfoInjectionPointDetected;

        var defaultTenant = OidcTenantConfig.of(getDefaultTenant(config));
        String defaultTenantId = defaultTenant.tenantId().get();
        var defaultTenantInitializer = createStaticTenantContextCreator(vertxValue, defaultTenant,
                !config.namedTenants().isEmpty(), defaultTenantId, tlsSupport);
        TenantConfigContext defaultTenantContext = createStaticTenantContext(vertxValue, defaultTenant,
                !config.namedTenants().isEmpty(), defaultTenantId, tlsSupport, defaultTenantInitializer);

        Map<String, TenantConfigContext> staticTenantsConfig = new HashMap<>();
        for (var tenant : config.namedTenants().entrySet()) {
            if (OidcConfig.DEFAULT_TENANT_KEY.equals(tenant.getKey())) {
                continue;
            }
            var namedTenantConfig = OidcTenantConfig.of(tenant.getValue());
            OidcCommonUtils.verifyConfigurationId(defaultTenantId, tenant.getKey(), namedTenantConfig.tenantId());
            var staticTenantInitializer = createStaticTenantContextCreator(vertxValue, namedTenantConfig, false,
                    tenant.getKey(), tlsSupport);
            staticTenantsConfig.put(tenant.getKey(),
                    createStaticTenantContext(vertxValue, namedTenantConfig, false, tenant.getKey(), tlsSupport,
                            staticTenantInitializer));
        }

        return new TenantConfigBean(staticTenantsConfig, defaultTenantContext,
                new TenantConfigBean.TenantContextFactory() {
                    @Override
                    public Uni<TenantConfigContext> create(OidcTenantConfig config) {
                        return createDynamicTenantContext(vertxValue, config, tlsSupport);
                    }
                });
    }

    private Uni<TenantConfigContext> createDynamicTenantContext(Vertx vertx,
            OidcTenantConfig oidcConfig, OidcTlsSupport tlsSupport) {

        var tenantId = oidcConfig.tenantId().orElseThrow();
        if (oidcConfig.logout().backchannel().path().isPresent()) {
            throw new ConfigurationException(
                    "BackChannel Logout is currently not supported for dynamic tenants");
        }
        return createTenantContext(vertx, oidcConfig, false, tenantId, tlsSupport)
                .onFailure().transform(new Function<Throwable, Throwable>() {
                    @Override
                    public Throwable apply(Throwable t) {
                        return logTenantConfigContextFailure(t, tenantId);
                    }
                });
    }

    private TenantConfigContext createStaticTenantContext(Vertx vertx,
            OidcTenantConfig oidcConfig, boolean checkNamedTenants, String tenantId,
            OidcTlsSupport tlsSupport, Supplier<Uni<TenantConfigContext>> staticTenantCreator) {

        Uni<TenantConfigContext> uniContext = createTenantContext(vertx, oidcConfig, checkNamedTenants, tenantId, tlsSupport);
        try {
            return uniContext.onFailure()
                    .recoverWithItem(new Function<Throwable, TenantConfigContext>() {
                        @Override
                        public TenantConfigContext apply(Throwable t) {
                            if (t instanceof OIDCException) {
                                LOG.warnf("Tenant '%s': '%s'."
                                        + " OIDC server is not available yet, an attempt to connect will be made during the first request."
                                        + " Access to resources protected by this tenant may fail"
                                        + " if OIDC server will not become available",
                                        tenantId, t.getMessage());
                                return TenantConfigContext.createNotReady(null, oidcConfig, staticTenantCreator);
                            }
                            logTenantConfigContextFailure(t, tenantId);
                            if (t instanceof ConfigurationException
                                    && !oidcConfig.authServerUrl().isPresent()
                                    && LaunchMode.DEVELOPMENT == LaunchMode.current()) {
                                // Let it start if it is a DEV mode and auth-server-url has not been configured yet
                                return TenantConfigContext.createNotReady(null, oidcConfig, staticTenantCreator);
                            }
                            // fail in all other cases
                            throw new OIDCException(t);
                        }
                    })
                    .await().atMost(oidcConfig.connectionTimeout());
        } catch (TimeoutException t2) {
            LOG.warnf("Tenant '%s': OIDC server is not available after a %d seconds timeout, an attempt to connect will be made"
                    + " during the first request. Access to resources protected by this tenant may fail if OIDC server"
                    + " will not become available", tenantId, oidcConfig.connectionTimeout().getSeconds());
            return TenantConfigContext.createNotReady(null, oidcConfig, staticTenantCreator);
        }
    }

    private Supplier<Uni<TenantConfigContext>> createStaticTenantContextCreator(Vertx vertx, OidcTenantConfig oidcConfig,
            boolean checkNamedTenants, String tenantId, OidcTlsSupport tlsSupport) {
        return new Supplier<Uni<TenantConfigContext>>() {
            @Override
            public Uni<TenantConfigContext> get() {
                return createTenantContext(vertx, oidcConfig, checkNamedTenants, tenantId, tlsSupport)
                        .onFailure().transform(new Function<Throwable, Throwable>() {
                            @Override
                            public Throwable apply(Throwable t) {
                                return logTenantConfigContextFailure(t, tenantId);
                            }
                        });
            }
        };
    }

    private static Throwable logTenantConfigContextFailure(Throwable t, String tenantId) {
        LOG.debugf(
                "'%s' tenant is not initialized: '%s'. Access to resources protected by this tenant will fail.",
                tenantId, t.getMessage());
        return t;
    }

    @SuppressWarnings("resource")
    private Uni<TenantConfigContext> createTenantContext(Vertx vertx, OidcTenantConfig oidcTenantConfig,
            boolean checkNamedTenants, String tenantId, OidcTlsSupport tlsSupport) {
        final OidcTenantConfig oidcConfig = OidcUtils.resolveProviderConfig(oidcTenantConfig);

        if (!oidcConfig.tenantEnabled()) {
            LOG.debugf("'%s' tenant configuration is disabled", tenantId);
            return Uni.createFrom().item(TenantConfigContext.createReady(new OidcProvider(null, null, null, null), oidcConfig));
        }

        if (oidcConfig.authServerUrl().isEmpty()) {
            if (oidcConfig.publicKey().isPresent() && oidcConfig.certificateChain().trustStoreFile().isPresent()) {
                throw new ConfigurationException("Both public key and certificate chain verification modes are enabled");
            }
            if (oidcConfig.publicKey().isPresent()) {
                return Uni.createFrom().item(createTenantContextFromPublicKey(oidcConfig));
            }

            if (oidcConfig.certificateChain().trustStoreFile().isPresent()) {
                return Uni.createFrom().item(createTenantContextToVerifyCertChain(oidcConfig));
            }
        }

        try {
            if (oidcConfig.authServerUrl().isEmpty()) {
                if (DEFAULT_TENANT_ID.equals(oidcConfig.tenantId().get())) {
                    ArcContainer container = Arc.container();
                    if (container != null
                            && (container.instance(TenantConfigResolver.class).isAvailable() || checkNamedTenants)) {
                        LOG.debugf("Default tenant is not configured and will be disabled"
                                + " because either 'TenantConfigResolver' which will resolve tenant configurations is registered"
                                + " or named tenants are configured.");
                        oidcConfig.tenantEnabled = false;
                        return Uni.createFrom()
                                .item(TenantConfigContext.createReady(new OidcProvider(null, null, null, null), oidcConfig));
                    }
                }
                throw new ConfigurationException(
                        "'" + getConfigPropertyForTenant(tenantId, "auth-server-url") + "' property must be configured");
            }
            OidcCommonUtils.verifyEndpointUrl(oidcConfig.authServerUrl().get());
            OidcCommonUtils.verifyCommonConfiguration(oidcConfig, OidcUtils.isServiceApp(oidcConfig), true);
        } catch (ConfigurationException t) {
            return Uni.createFrom().failure(t);
        }

        if (oidcConfig.roles().source().orElse(null) == Source.userinfo && !enableUserInfo(oidcConfig)) {
            throw new ConfigurationException(
                    "UserInfo is not required but UserInfo is expected to be the source of authorization roles");
        }
        if (oidcConfig.token().verifyAccessTokenWithUserInfo().orElse(false) && !OidcUtils.isWebApp(oidcConfig)
                && !enableUserInfo(oidcConfig)) {
            throw new ConfigurationException(
                    "UserInfo is not required but 'verifyAccessTokenWithUserInfo' is enabled");
        }
        if (!oidcConfig.authentication().idTokenRequired().orElse(true) && !enableUserInfo(oidcConfig)) {
            throw new ConfigurationException(
                    "UserInfo is not required but it will be needed to verify a code flow access token");
        }

        if (!oidcConfig.discoveryEnabled().orElse(true)) {
            if (!OidcUtils.isServiceApp(oidcConfig)) {
                if (oidcConfig.authorizationPath().isEmpty() || oidcConfig.tokenPath().isEmpty()) {
                    String authorizationPathProperty = getConfigPropertyForTenant(tenantId, "authorization-path");
                    String tokenPathProperty = getConfigPropertyForTenant(tenantId, "token-path");
                    throw new ConfigurationException(
                            "'web-app' applications must have '" + authorizationPathProperty + "' and '" + tokenPathProperty
                                    + "' properties "
                                    + "set when the discovery is disabled.",
                            Set.of(authorizationPathProperty, tokenPathProperty));
                }
            }
            // JWK and introspection endpoints have to be set for both 'web-app' and 'service' applications
            if (oidcConfig.jwksPath().isEmpty() && oidcConfig.introspectionPath().isEmpty()) {
                if (!oidcConfig.authentication().idTokenRequired().orElse(true)
                        && oidcConfig.authentication().userInfoRequired().orElse(false)) {
                    LOG.debugf("tenant %s supports only UserInfo", oidcConfig.tenantId().get());
                } else {
                    throw new ConfigurationException(
                            "Either 'jwks-path' or 'introspection-path' properties must be set when the discovery is disabled.",
                            Set.of("quarkus.oidc.jwks-path", "quarkus.oidc.introspection-path"));
                }
            }
            if (oidcConfig.authentication().userInfoRequired().orElse(false) && oidcConfig.userInfoPath().isEmpty()) {
                String configProperty = getConfigPropertyForTenant(tenantId, "user-info-path");
                throw new ConfigurationException(
                        "UserInfo is required but '" + configProperty + "' is not configured.",
                        Set.of(configProperty));
            }
        }

        if (OidcUtils.isServiceApp(oidcConfig)) {
            if (oidcConfig.token().refreshExpired()) {
                throw new ConfigurationException(
                        "The '" + getConfigPropertyForTenant(tenantId, "token.refresh-expired")
                                + "' property can only be enabled for " + ApplicationType.WEB_APP
                                + " application types");
            }
            if (oidcConfig.token().refreshTokenTimeSkew().isPresent()) {
                throw new ConfigurationException(
                        "The '" + getConfigPropertyForTenant(tenantId, "token.refresh-token-time-skew")
                                + "' property can only be enabled for " + ApplicationType.WEB_APP
                                + " application types");
            }
            if (oidcConfig.logout().path().isPresent()) {
                throw new ConfigurationException(
                        "The '" + getConfigPropertyForTenant(tenantId, "logout.path") + "' property can only be enabled for "
                                + ApplicationType.WEB_APP + " application types");
            }
            if (oidcConfig.roles().source().isPresent() && oidcConfig.roles().source().get() == Source.idtoken) {
                throw new ConfigurationException(
                        "The '" + getConfigPropertyForTenant(tenantId, "roles.source")
                                + "' property can only be set to 'idtoken' for " + ApplicationType.WEB_APP
                                + " application types");
            }
        } else {
            if (oidcConfig.token().refreshTokenTimeSkew().isPresent()) {
                oidcConfig.token.setRefreshExpired(true);
            }
        }

        if (oidcConfig.tokenStateManager().strategy() != Strategy.KEEP_ALL_TOKENS) {

            if (oidcConfig.authentication().userInfoRequired().orElse(false)
                    || oidcConfig.roles().source().orElse(null) == Source.userinfo) {
                throw new ConfigurationException(
                        "UserInfo is required but DefaultTokenStateManager is configured to not keep the access token");
            }
            if (oidcConfig.roles().source().orElse(null) == Source.accesstoken) {
                throw new ConfigurationException(
                        "Access token is required to check the roles but DefaultTokenStateManager is configured to not keep the access token");
            }
        }

        if (oidcConfig.token().verifyAccessTokenWithUserInfo().orElse(false)) {
            if (!oidcConfig.discoveryEnabled().orElse(true)) {
                if (oidcConfig.userInfoPath().isEmpty()) {
                    throw new ConfigurationException(
                            "UserInfo path is missing but 'verifyAccessTokenWithUserInfo' is enabled");
                }
                if (oidcConfig.introspectionPath().isPresent()) {
                    throw new ConfigurationException(
                            "Introspection path is configured and 'verifyAccessTokenWithUserInfo' is enabled, these options are mutually exclusive");
                }
            }
        }

        if (!oidcConfig.token().issuedAtRequired() && oidcConfig.token().age().isPresent()) {
            String tokenIssuedAtRequired = getConfigPropertyForTenant(tenantId, "token.issued-at-required");
            String tokenAge = getConfigPropertyForTenant(tenantId, "token.age");
            throw new ConfigurationException(
                    "The '" + tokenIssuedAtRequired + "' can only be set to false if '" + tokenAge + "' is not set." +
                            " Either set '" + tokenIssuedAtRequired + "' to true or do not set '" + tokenAge + "'.",
                    Set.of(tokenIssuedAtRequired, tokenAge));
        }

        return createOidcProvider(oidcConfig, vertx, tlsSupport)
                .onItem().transform(new Function<OidcProvider, TenantConfigContext>() {
                    @Override
                    public TenantConfigContext apply(OidcProvider p) {
                        return TenantConfigContext.createReady(p, oidcConfig);
                    }
                });
    }

    private static String getConfigPropertyForTenant(String tenantId, String configSubKey) {
        if (DEFAULT_TENANT_ID.equals(tenantId)) {
            return "quarkus.oidc." + configSubKey;
        } else {
            return "quarkus.oidc." + tenantId + "." + configSubKey;
        }
    }

    private static boolean enableUserInfo(OidcTenantConfig oidcConfig) {
        Optional<Boolean> userInfoRequired = oidcConfig.authentication().userInfoRequired();
        if (userInfoRequired.isPresent()) {
            if (!userInfoRequired.get()) {
                return false;
            }
        } else {
            oidcConfig.authentication.setUserInfoRequired(true);
        }
        return true;
    }

    private static TenantConfigContext createTenantContextFromPublicKey(OidcTenantConfig oidcConfig) {
        if (!OidcUtils.isServiceApp(oidcConfig)) {
            throw new ConfigurationException("'public-key' property can only be used with the 'service' applications");
        }
        LOG.debug("'public-key' property for the local token verification is set,"
                + " no connection to the OIDC server will be created");

        return TenantConfigContext.createReady(
                new OidcProvider(oidcConfig.publicKey().get(), oidcConfig, readTokenDecryptionKey(oidcConfig)), oidcConfig);
    }

    private static TenantConfigContext createTenantContextToVerifyCertChain(OidcTenantConfig oidcConfig) {
        if (!OidcUtils.isServiceApp(oidcConfig)) {
            throw new ConfigurationException(
                    "Currently only 'service' applications can be used to verify tokens with inlined certificate chains");
        }

        return TenantConfigContext.createReady(
                new OidcProvider(null, oidcConfig, readTokenDecryptionKey(oidcConfig)), oidcConfig);
    }

    public static Optional<ProxyOptions> toProxyOptions(OidcCommonConfig.Proxy proxyConfig) {
        return OidcCommonUtils.toProxyOptions(proxyConfig);
    }

    protected static OIDCException toOidcException(Throwable cause, String authServerUrl, String tenantId) {
        final String message = OidcCommonUtils.formatConnectionErrorMessage(authServerUrl);
        LOG.warn(message);
        fireOidcServerNotAvailableEvent(authServerUrl, tenantId);
        return new OIDCException("OIDC Server is not available", cause);
    }

    protected static Uni<OidcProvider> createOidcProvider(OidcTenantConfig oidcConfig, Vertx vertx,
            OidcTlsSupport tlsSupport) {
        return createOidcClientUni(oidcConfig, vertx, tlsSupport)
                .flatMap(new Function<OidcProviderClient, Uni<? extends OidcProvider>>() {
                    @Override
                    public Uni<OidcProvider> apply(OidcProviderClient client) {
                        if (oidcConfig.jwks().resolveEarly()
                                && client.getMetadata().getJsonWebKeySetUri() != null
                                && !oidcConfig.token().requireJwtIntrospectionOnly()) {
                            return getJsonWebSetUni(client, oidcConfig).onItem()
                                    .transform(new Function<JsonWebKeySet, OidcProvider>() {
                                        @Override
                                        public OidcProvider apply(JsonWebKeySet jwks) {
                                            return new OidcProvider(client, oidcConfig, jwks,
                                                    readTokenDecryptionKey(oidcConfig));
                                        }
                                    });
                        } else {
                            return Uni.createFrom()
                                    .item(new OidcProvider(client, oidcConfig, null, readTokenDecryptionKey(oidcConfig)));
                        }
                    }
                });
    }

    private static Key readTokenDecryptionKey(OidcTenantConfig oidcConfig) {
        if (oidcConfig.token().decryptionKeyLocation().isPresent()) {
            try {
                Key key = null;

                String keyContent = KeyUtils.readKeyContent(oidcConfig.token().decryptionKeyLocation().get());
                if (keyContent != null) {
                    List<JsonWebKey> keys = KeyUtils.loadJsonWebKeys(keyContent);
                    if (keys != null && keys.size() == 1 &&
                            (keys.get(0).getAlgorithm() == null
                                    || keys.get(0).getAlgorithm().equals(KeyEncryptionAlgorithm.RSA_OAEP.getAlgorithm()))
                            && ("enc".equals(keys.get(0).getUse()) || keys.get(0).getUse() == null)) {
                        key = PublicJsonWebKey.class.cast(keys.get(0)).getPrivateKey();
                    }
                }
                if (key == null) {
                    key = KeyUtils.decodeDecryptionPrivateKey(keyContent);
                }
                return key;
            } catch (Exception ex) {
                throw new ConfigurationException(
                        String.format("Token decryption key for tenant %s can not be read from %s",
                                oidcConfig.tenantId().get(), oidcConfig.token().decryptionKeyLocation().get()),
                        ex);
            }
        } else {
            return null;
        }
    }

    protected static Uni<JsonWebKeySet> getJsonWebSetUni(OidcProviderClient client, OidcTenantConfig oidcConfig) {
        if (!oidcConfig.discoveryEnabled().orElse(true)) {
            String tenantId = oidcConfig.tenantId().orElse(DEFAULT_TENANT_ID);
            if (shouldFireOidcServerAvailableEvent(tenantId)) {
                return getJsonWebSetUniWhenDiscoveryDisabled(client, oidcConfig)
                        .invoke(new Runnable() {
                            @Override
                            public void run() {
                                fireOidcServerAvailableEvent(oidcConfig.authServerUrl().get(), tenantId);
                            }
                        });
            }
            return getJsonWebSetUniWhenDiscoveryDisabled(client, oidcConfig);
        } else {
            return client.getJsonWebKeySet(null);
        }
    }

    private static Uni<JsonWebKeySet> getJsonWebSetUniWhenDiscoveryDisabled(OidcProviderClient client,
            OidcTenantConfig oidcConfig) {
        final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
        return client.getJsonWebKeySet(null).onFailure(OidcCommonUtils.oidcEndpointNotAvailable())
                .retry()
                .withBackOff(OidcCommonUtils.CONNECTION_BACKOFF_DURATION, OidcCommonUtils.CONNECTION_BACKOFF_DURATION)
                .expireIn(connectionDelayInMillisecs)
                .onFailure()
                .transform(new Function<Throwable, Throwable>() {
                    @Override
                    public Throwable apply(Throwable t) {
                        return toOidcException(t, oidcConfig.authServerUrl().get(),
                                oidcConfig.tenantId().orElse(DEFAULT_TENANT_ID));
                    }
                })
                .onFailure()
                .invoke(client::close);
    }

    protected static Uni<OidcProviderClient> createOidcClientUni(OidcTenantConfig oidcConfig, Vertx vertx,
            OidcTlsSupport tlsSupport) {

        String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);

        WebClientOptions options = new WebClientOptions();
        options.setFollowRedirects(oidcConfig.followRedirects());
        OidcCommonUtils.setHttpClientOptions(oidcConfig, options, tlsSupport.forConfig(oidcConfig.tls()));
        var mutinyVertx = new io.vertx.mutiny.core.Vertx(vertx);
        WebClient client = WebClient.create(mutinyVertx, options);

        Map<OidcEndpoint.Type, List<OidcRequestFilter>> oidcRequestFilters = OidcCommonUtils.getOidcRequestFilters();
        Map<OidcEndpoint.Type, List<OidcResponseFilter>> oidcResponseFilters = OidcCommonUtils.getOidcResponseFilters();

        Uni<OidcConfigurationMetadata> metadataUni = null;
        if (!oidcConfig.discoveryEnabled().orElse(true)) {
            metadataUni = Uni.createFrom().item(createLocalMetadata(oidcConfig, authServerUriString));
        } else {
            final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
            OidcRequestContextProperties contextProps = new OidcRequestContextProperties(
                    Map.of(OidcUtils.TENANT_ID_ATTRIBUTE, oidcConfig.tenantId().orElse(OidcUtils.DEFAULT_TENANT_ID)));
            metadataUni = OidcCommonUtils
                    .discoverMetadata(client, oidcRequestFilters, contextProps, oidcResponseFilters, authServerUriString,
                            connectionDelayInMillisecs,
                            mutinyVertx,
                            oidcConfig.useBlockingDnsLookup())
                    .onItem()
                    .transform(new Function<JsonObject, OidcConfigurationMetadata>() {
                        @Override
                        public OidcConfigurationMetadata apply(JsonObject json) {
                            return new OidcConfigurationMetadata(json, createLocalMetadata(oidcConfig, authServerUriString),
                                    OidcCommonUtils.getDiscoveryUri(authServerUriString));
                        }
                    });
        }
        return metadataUni.onItemOrFailure()
                .transformToUni(new BiFunction<OidcConfigurationMetadata, Throwable, Uni<? extends OidcProviderClient>>() {

                    @Override
                    public Uni<OidcProviderClient> apply(OidcConfigurationMetadata metadata, Throwable t) {
                        String tenantId = oidcConfig.tenantId().orElse(DEFAULT_TENANT_ID);
                        if (t != null) {
                            client.close();
                            return Uni.createFrom().failure(toOidcException(t, authServerUriString, tenantId));
                        }
                        if (shouldFireOidcServerAvailableEvent(tenantId)) {
                            fireOidcServerAvailableEvent(authServerUriString, tenantId);
                        }
                        if (metadata == null) {
                            client.close();
                            return Uni.createFrom().failure(new ConfigurationException(
                                    "OpenId Connect Provider configuration metadata is not configured and can not be discovered"));
                        }
                        if (oidcConfig.logout().path().isPresent()) {
                            if (oidcConfig.endSessionPath().isEmpty() && metadata.getEndSessionUri() == null) {
                                client.close();
                                return Uni.createFrom().failure(new ConfigurationException(
                                        "The application supports RP-Initiated Logout but the OpenID Provider does not advertise the end_session_endpoint"));
                            }
                        }
                        if (userInfoInjectionPointDetected && metadata.getUserInfoUri() != null) {
                            enableUserInfo(oidcConfig);
                        }
                        if (oidcConfig.authentication().userInfoRequired().orElse(false) && metadata.getUserInfoUri() == null) {
                            client.close();
                            return Uni.createFrom().failure(new ConfigurationException(
                                    "UserInfo is required but the OpenID Provider UserInfo endpoint is not configured."
                                            + " Use 'quarkus.oidc.user-info-path' if the discovery is disabled."));
                        }
                        return Uni.createFrom()
                                .item(new OidcProviderClient(client, vertx, metadata, oidcConfig, oidcRequestFilters,
                                        oidcResponseFilters));
                    }

                });
    }

    private static OidcConfigurationMetadata createLocalMetadata(OidcTenantConfig oidcConfig, String authServerUriString) {
        String tokenUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.tokenPath());
        String introspectionUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString,
                oidcConfig.introspectionPath());
        String authorizationUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString,
                oidcConfig.authorizationPath());
        String jwksUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.jwksPath());
        String userInfoUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.userInfoPath());
        String endSessionUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.endSessionPath());
        String registrationUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.registrationPath());
        return new OidcConfigurationMetadata(tokenUri,
                introspectionUri, authorizationUri, jwksUri, userInfoUri, endSessionUri, registrationUri,
                oidcConfig.token().issuer().orElse(null));
    }

    private static void fireOidcServerNotAvailableEvent(String authServerUrl, String tenantId) {
        if (fireOidcServerEvent(authServerUrl, OIDC_SERVER_NOT_AVAILABLE)) {
            tenantsExpectingServerAvailableEvents.add(tenantId);
        }
    }

    private static void fireOidcServerAvailableEvent(String authServerUrl, String tenantId) {
        if (fireOidcServerEvent(authServerUrl, OIDC_SERVER_AVAILABLE)) {
            tenantsExpectingServerAvailableEvents.remove(tenantId);
        }
    }

    private static boolean shouldFireOidcServerAvailableEvent(String tenantId) {
        return tenantsExpectingServerAvailableEvents.contains(tenantId);
    }

    private static boolean fireOidcServerEvent(String authServerUrl, SecurityEvent.Type eventType) {
        if (ConfigProvider.getConfig().getOptionalValue(SECURITY_EVENTS_ENABLED_CONFIG_KEY, boolean.class).orElse(true)) {
            SecurityEventHelper.fire(
                    Arc.container().beanManager().getEvent().select(SecurityEvent.class),
                    new SecurityEvent(eventType, Map.of(AUTH_SERVER_URL, authServerUrl)));
            return true;
        }
        return false;
    }

    public Function<String, Consumer<RoutingContext>> tenantResolverInterceptorCreator() {
        return new Function<String, Consumer<RoutingContext>>() {
            @Override
            public Consumer<RoutingContext> apply(String tenantId) {
                return new Consumer<RoutingContext>() {
                    @Override
                    public void accept(RoutingContext routingContext) {
                        OidcTenantConfig tenantConfig = routingContext.get(OidcTenantConfig.class.getName());
                        if (tenantConfig != null) {
                            // authentication has happened before @Tenant annotation was matched with the HTTP request
                            String tenantUsedForAuth = tenantConfig.tenantId().orElse(null);
                            if (tenantId.equals(tenantUsedForAuth)) {
                                // @Tenant selects the same tenant as already selected
                                return;
                            } else {
                                // @Tenant selects the different tenant than already selected
                                throw new AuthenticationFailedException(
                                        """
                                                The '%1$s' selected with the @Tenant annotation must be used to authenticate
                                                the request but it was already authenticated with the '%2$s' tenant. It
                                                can happen if the '%1$s' is selected with an annotation but '%2$s' is
                                                resolved during authentication required by the HTTP Security Policy which
                                                is enforced before the JAX-RS chain is run. In such cases, please set the
                                                'quarkus.http.auth.permission."permissions".applies-to=JAXRS' to all HTTP
                                                Security Policies which secure the same REST endpoints as the ones
                                                where the '%1$s' tenant is resolved by the '@Tenant' annotation.
                                                """
                                                .formatted(tenantId, tenantUsedForAuth));
                            }
                        }

                        LOG.debugf("@Tenant annotation set a '%s' tenant id on the %s request path", tenantId,
                                routingContext.request().path());
                        routingContext.put(OidcUtils.TENANT_ID_SET_BY_ANNOTATION, tenantId);
                        routingContext.put(OidcUtils.TENANT_ID_ATTRIBUTE, tenantId);
                    }
                };
            }
        };
    }

    public Supplier<TenantIdentityProvider> createTenantIdentityProvider(String tenantName) {
        return new Supplier<TenantIdentityProvider>() {
            @Override
            public TenantIdentityProvider get() {
                return new TenantSpecificOidcIdentityProvider(tenantName);
            }
        };
    }

    private static final class TenantSpecificOidcIdentityProvider extends OidcIdentityProvider
            implements TenantIdentityProvider {

        private final String tenantId;
        private final BlockingSecurityExecutor blockingExecutor;

        private TenantSpecificOidcIdentityProvider(String tenantId) {
            super(Arc.container().instance(DefaultTenantConfigResolver.class).get(),
                    Arc.container().instance(BlockingSecurityExecutor.class).get());
            this.blockingExecutor = Arc.container().instance(BlockingSecurityExecutor.class).get();
            if (tenantId.equals(DEFAULT_TENANT_ID)) {
                OidcConfig config = Arc.container().instance(OidcConfig.class).get();
                this.tenantId = getDefaultTenant(config).tenantId().orElse(OidcUtils.DEFAULT_TENANT_ID);
            } else {
                this.tenantId = tenantId;
            }
        }

        @Override
        public Uni<SecurityIdentity> authenticate(AccessTokenCredential token) {
            return authenticate(new TokenAuthenticationRequest(token));
        }

        @Override
        protected Uni<TenantConfigContext> resolveTenantConfigContext(TokenAuthenticationRequest request,
                AuthenticationRequestContext context) {
            return tenantResolver.resolveContext(tenantId).onItem().ifNull().failWith(new Supplier<Throwable>() {
                @Override
                public Throwable get() {
                    return new OIDCException("Failed to resolve tenant context");
                }
            });
        }

        @Override
        protected Map<String, Object> getRequestData(TokenAuthenticationRequest request) {
            RoutingContext context = getRoutingContextAttribute(request);
            if (context != null) {
                return context.data();
            }
            return new HashMap<>();
        }

        private Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request) {
            return authenticate(request, new AuthenticationRequestContext() {
                @Override
                public Uni<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> function) {
                    return blockingExecutor.executeBlocking(function);
                }
            });
        }
    }
}
