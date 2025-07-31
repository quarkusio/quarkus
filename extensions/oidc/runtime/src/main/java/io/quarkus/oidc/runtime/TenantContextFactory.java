package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.SecurityEvent.AUTH_SERVER_URL;
import static io.quarkus.oidc.SecurityEvent.Type.OIDC_SERVER_AVAILABLE;
import static io.quarkus.oidc.SecurityEvent.Type.OIDC_SERVER_NOT_AVAILABLE;
import static io.quarkus.oidc.runtime.OidcRecorder.LOG;
import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.quarkus.oidc.common.runtime.OidcTlsSupport;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;

final class TenantContextFactory {

    static volatile boolean userInfoInjectionPointDetected = false;

    private final Set<String> tenantsExpectingServerAvailableEvents;
    private final Vertx vertx;
    private final OidcTlsSupport tlsSupport;
    private final boolean securityEventsEnabled;

    TenantContextFactory(Vertx vertx, TlsConfigurationRegistry tlsConfigurationRegistry, boolean securityEventsEnabled) {
        this.vertx = vertx;
        this.tlsSupport = OidcTlsSupport.of(tlsConfigurationRegistry);
        this.securityEventsEnabled = securityEventsEnabled;
        this.tenantsExpectingServerAvailableEvents = ConcurrentHashMap.newKeySet();
    }

    TenantConfigContext createDefaultTenantConfig(Map<String, OidcTenantConfig> staticTenants, OidcTenantConfig defaultTenant) {
        String defaultTenantId = defaultTenant.tenantId().get();
        boolean foundNamedStaticTenants = !staticTenants.isEmpty();
        var defaultTenantInitializer = createStaticTenantContextCreator(defaultTenant, foundNamedStaticTenants,
                defaultTenantId);
        return createStaticTenantContext(defaultTenant, foundNamedStaticTenants, defaultTenantId, defaultTenantInitializer);
    }

    Map<String, TenantConfigContext> createStaticTenantConfigs(Map<String, OidcTenantConfig> staticTenants,
            OidcTenantConfig defaultTenant) {
        final String defaultTenantId = defaultTenant.tenantId().get();
        Map<String, TenantConfigContext> staticTenantsConfig = new HashMap<>();
        for (var tenant : staticTenants.entrySet()) {
            createStaticTenantConfig(defaultTenantId, tenant.getKey(), tenant.getValue(), staticTenantsConfig);
        }
        return Map.copyOf(staticTenantsConfig);
    }

    Uni<TenantConfigContext> createDynamic(OidcTenantConfig oidcConfig) {
        var tenantId = oidcConfig.tenantId().orElseThrow();
        if (OidcUtils.DEFAULT_TENANT_ID.equals(tenantId)) {
            throw new ConfigurationException("Dynamic tenant ID cannot be same as the default tenant ID: " + tenantId);
        }
        return createTenantContext(oidcConfig, false, tenantId)
                .onFailure().transform(new Function<Throwable, Throwable>() {
                    @Override
                    public Throwable apply(Throwable t) {
                        return logTenantConfigContextFailure(t, tenantId);
                    }
                });
    }

    private void createStaticTenantConfig(String defaultTenantId, String tenantKey, OidcTenantConfig namedTenantConfig,
            Map<String, TenantConfigContext> staticTenantsConfig) {
        OidcCommonUtils.verifyConfigurationId(defaultTenantId, tenantKey, namedTenantConfig.tenantId());
        var staticTenantInitializer = createStaticTenantContextCreator(namedTenantConfig, false, tenantKey);
        staticTenantsConfig.put(tenantKey,
                createStaticTenantContext(namedTenantConfig, false, tenantKey, staticTenantInitializer));
    }

    private TenantConfigContext createStaticTenantContext(
            OidcTenantConfig oidcConfig, boolean checkNamedTenants, String tenantId,
            Supplier<Uni<TenantConfigContext>> staticTenantCreator) {

        Uni<TenantConfigContext> uniContext = createTenantContext(oidcConfig, checkNamedTenants, tenantId);
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

    private Supplier<Uni<TenantConfigContext>> createStaticTenantContextCreator(OidcTenantConfig oidcConfig,
            boolean checkNamedTenants, String tenantId) {
        return new Supplier<Uni<TenantConfigContext>>() {
            @Override
            public Uni<TenantConfigContext> get() {
                return createTenantContext(oidcConfig, checkNamedTenants, tenantId)
                        .onFailure().transform(new Function<Throwable, Throwable>() {
                            @Override
                            public Throwable apply(Throwable t) {
                                return logTenantConfigContextFailure(t, tenantId);
                            }
                        });
            }
        };
    }

    private Throwable logTenantConfigContextFailure(Throwable t, String tenantId) {
        LOG.debugf(
                "'%s' tenant is not initialized: '%s'. Access to resources protected by this tenant will fail.",
                tenantId, t.getMessage());
        return t;
    }

    @SuppressWarnings("resource")
    private Uni<TenantConfigContext> createTenantContext(OidcTenantConfig oidcTenantConfig,
            boolean checkNamedTenants, String tenantId) {
        final OidcTenantConfig oidcConfig = OidcUtils.resolveProviderConfig(oidcTenantConfig);

        if (!oidcConfig.tenantEnabled()) {
            LOG.debugf("'%s' tenant configuration is disabled", tenantId);
            return Uni.createFrom().item(TenantConfigContext.createReady(new OidcProvider(null, null, null), oidcConfig));
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
                                .item(TenantConfigContext.createReady(new OidcProvider(null, null, null), oidcConfig));
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

        if (oidcConfig.roles().source().orElse(null) == io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source.userinfo
                && !enableUserInfo(oidcConfig)) {
            throw new ConfigurationException(
                    "UserInfo is not required but UserInfo is expected to be the source of authorization roles");
        }
        if (oidcConfig.token().verifyAccessTokenWithUserInfo().orElse(false) && !OidcUtils.isWebApp(oidcConfig)
                && !enableUserInfo(oidcConfig)) {
            String propertyName = getConfigPropertyForTenant(tenantId, "token.verify-access-token-with-user-info");
            throw new ConfigurationException("UserInfo is not required but '%s' is enabled".formatted(propertyName));
        }
        if (!oidcConfig.authentication().idTokenRequired().orElse(true) && OidcUtils.isWebApp(oidcConfig)
                && StepUpAuthenticationPolicy.isEnabled()) {
            String propertyName = getConfigPropertyForTenant(tenantId, "authentication.id-token-required");
            // this can be false alarm in case Quarkus application have multiple tenants and 'acr' values are not
            // required for this tenant, which we cannot know
            LOG.warnf("Step Up Authentication is not supported for tenant '%s', because the internal IdToken is"
                    + " generated by Quarkus. Please see the '%s' configuration property documentation for more information",
                    tenantId, propertyName);
        }
        if (!oidcConfig.authentication().idTokenRequired().orElse(true) && !enableUserInfo(oidcConfig)) {
            throw new ConfigurationException(
                    "UserInfo is not required for OIDC tenant '%s' but it will be needed to verify a code flow access token"
                            .formatted(tenantId));
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
                                + "' property can only be enabled for "
                                + io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.WEB_APP
                                + " application types");
            }
            if (oidcConfig.token().refreshTokenTimeSkew().isPresent()) {
                throw new ConfigurationException(
                        "The '" + getConfigPropertyForTenant(tenantId, "token.refresh-token-time-skew")
                                + "' property can only be enabled for "
                                + io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.WEB_APP
                                + " application types");
            }
            if (oidcConfig.logout().path().isPresent()) {
                throw new ConfigurationException(
                        "The '" + getConfigPropertyForTenant(tenantId, "logout.path") + "' property can only be enabled for "
                                + io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.WEB_APP + " application types");
            }
            if (oidcConfig.roles().source().isPresent()
                    && oidcConfig.roles().source().get() == io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source.idtoken) {
                throw new ConfigurationException(
                        "The '" + getConfigPropertyForTenant(tenantId, "roles.source")
                                + "' property can only be set to 'idtoken' for "
                                + io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.WEB_APP
                                + " application types");
            }
        } else {
            if (oidcConfig.token().refreshTokenTimeSkew().isPresent()) {
                oidcConfig.token.setRefreshExpired(true);
            }
        }

        if (oidcConfig.tokenStateManager()
                .strategy() != io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.Strategy.KEEP_ALL_TOKENS) {

            if (oidcConfig.authentication().userInfoRequired().orElse(false)
                    || oidcConfig.roles().source()
                            .orElse(null) == io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source.userinfo) {
                throw new ConfigurationException(
                        "UserInfo is required but DefaultTokenStateManager is configured to not keep the access token");
            }
            if (oidcConfig.roles().source().orElse(null) == io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source.accesstoken) {
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

        return createOidcProvider(oidcConfig)
                .onItem().transform(new Function<OidcProvider, TenantConfigContext>() {
                    @Override
                    public TenantConfigContext apply(OidcProvider p) {
                        return TenantConfigContext.createReady(p, oidcConfig);
                    }
                });
    }

    private String getConfigPropertyForTenant(String tenantId, String configSubKey) {
        if (DEFAULT_TENANT_ID.equals(tenantId)) {
            return "quarkus.oidc." + configSubKey;
        } else {
            return "quarkus.oidc." + tenantId + "." + configSubKey;
        }
    }

    private boolean enableUserInfo(OidcTenantConfig oidcConfig) {
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

    private TenantConfigContext createTenantContextFromPublicKey(OidcTenantConfig oidcConfig) {
        if (!OidcUtils.isServiceApp(oidcConfig)) {
            throw new ConfigurationException("'public-key' property can only be used with the 'service' applications");
        }
        LOG.debug("'public-key' property for the local token verification is set,"
                + " no connection to the OIDC server will be created");

        return TenantConfigContext.createReady(
                new OidcProvider(oidcConfig.publicKey().get(), oidcConfig), oidcConfig);
    }

    private TenantConfigContext createTenantContextToVerifyCertChain(OidcTenantConfig oidcConfig) {
        if (!OidcUtils.isServiceApp(oidcConfig)) {
            throw new ConfigurationException(
                    "Currently only 'service' applications can be used to verify tokens with inlined certificate chains");
        }

        return TenantConfigContext.createReady(
                new OidcProvider(null, oidcConfig), oidcConfig);
    }

    private OIDCException toOidcException(Throwable cause, String authServerUrl, String tenantId) {
        final String message = OidcCommonUtils.formatConnectionErrorMessage(authServerUrl);
        LOG.warn(message);
        fireOidcServerNotAvailableEvent(authServerUrl, tenantId);
        return new OIDCException("OIDC Server is not available", cause);
    }

    private Uni<OidcProvider> createOidcProvider(OidcTenantConfig oidcConfig) {
        return createOidcClientUni(oidcConfig)
                .flatMap(new Function<OidcProviderClientImpl, Uni<? extends OidcProvider>>() {
                    @Override
                    public Uni<OidcProvider> apply(OidcProviderClientImpl client) {
                        if (oidcConfig.jwks().resolveEarly()
                                && client.getMetadata().getJsonWebKeySetUri() != null
                                && !oidcConfig.token().requireJwtIntrospectionOnly()) {
                            return getJsonWebSetUni(client, oidcConfig).onItem()
                                    .transform(new Function<JsonWebKeySet, OidcProvider>() {
                                        @Override
                                        public OidcProvider apply(JsonWebKeySet jwks) {
                                            return new OidcProvider(client, oidcConfig, jwks);
                                        }
                                    });
                        } else {
                            return Uni.createFrom()
                                    .item(new OidcProvider(client, oidcConfig, null));
                        }
                    }
                });
    }

    private Uni<JsonWebKeySet> getJsonWebSetUni(OidcProviderClientImpl client, OidcTenantConfig oidcConfig) {
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

    private Uni<JsonWebKeySet> getJsonWebSetUniWhenDiscoveryDisabled(OidcProviderClientImpl client,
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

    private Uni<OidcProviderClientImpl> createOidcClientUni(OidcTenantConfig oidcConfig) {

        String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);

        WebClientOptions options = new WebClientOptions();
        options.setFollowRedirects(oidcConfig.followRedirects());
        OidcCommonUtils.setHttpClientOptions(oidcConfig, options, tlsSupport.forConfig(oidcConfig.tls()));
        var mutinyVertx = new io.vertx.mutiny.core.Vertx(vertx);
        WebClient client = WebClient.create(mutinyVertx, options);

        OidcFilterStorage oidcFilterStorage = OidcFilterStorage.get();

        Uni<OidcConfigurationMetadata> metadataUni = null;
        if (!oidcConfig.discoveryEnabled().orElse(true)) {
            metadataUni = Uni.createFrom().item(createLocalMetadata(oidcConfig, authServerUriString));
        } else {
            final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
            OidcRequestContextProperties contextProps = new OidcRequestContextProperties(
                    Map.of(OidcUtils.TENANT_ID_ATTRIBUTE, oidcConfig.tenantId().orElse(OidcUtils.DEFAULT_TENANT_ID),
                            OidcUtils.OIDC_AUTH_MECHANISM, OidcUtils.getOidcAuthMechanism(oidcConfig)));
            metadataUni = OidcCommonUtils
                    .discoverMetadata(client, contextProps, authServerUriString, connectionDelayInMillisecs,
                            mutinyVertx, oidcConfig.useBlockingDnsLookup(), oidcFilterStorage)
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
                .transformToUni(new BiFunction<OidcConfigurationMetadata, Throwable, Uni<? extends OidcProviderClientImpl>>() {

                    @Override
                    public Uni<OidcProviderClientImpl> apply(OidcConfigurationMetadata metadata, Throwable t) {
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
                                .item(new OidcProviderClientImpl(client, vertx, metadata, oidcConfig, oidcFilterStorage));
                    }

                });
    }

    private OidcConfigurationMetadata createLocalMetadata(OidcTenantConfig oidcConfig, String authServerUriString) {
        String tokenUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.tokenPath());
        String introspectionUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString,
                oidcConfig.introspectionPath());
        String authorizationUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString,
                oidcConfig.authorizationPath());
        String jwksUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.jwksPath());
        String userInfoUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.userInfoPath());
        String endSessionUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.endSessionPath());
        String registrationUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.registrationPath());
        String revocationUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.revokePath());
        return new OidcConfigurationMetadata(tokenUri,
                introspectionUri, authorizationUri, jwksUri, userInfoUri, endSessionUri, registrationUri, revocationUri,
                oidcConfig.token().issuer().orElse(null));
    }

    private void fireOidcServerNotAvailableEvent(String authServerUrl, String tenantId) {
        if (fireOidcServerEvent(authServerUrl, OIDC_SERVER_NOT_AVAILABLE)) {
            tenantsExpectingServerAvailableEvents.add(tenantId);
        }
    }

    private void fireOidcServerAvailableEvent(String authServerUrl, String tenantId) {
        if (fireOidcServerEvent(authServerUrl, OIDC_SERVER_AVAILABLE)) {
            tenantsExpectingServerAvailableEvents.remove(tenantId);
        }
    }

    private boolean shouldFireOidcServerAvailableEvent(String tenantId) {
        return tenantsExpectingServerAvailableEvents.contains(tenantId);
    }

    private boolean fireOidcServerEvent(String authServerUrl, SecurityEvent.Type eventType) {
        if (securityEventsEnabled) {
            SecurityEventHelper.fire(
                    Arc.container().beanManager().getEvent().select(SecurityEvent.class),
                    new SecurityEvent(eventType, Map.of(AUTH_SERVER_URL, authServerUrl)));
            return true;
        }
        return false;
    }
}
