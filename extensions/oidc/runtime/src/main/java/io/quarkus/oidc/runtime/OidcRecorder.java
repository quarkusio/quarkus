package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.OidcTenantConfig.TokenStateManager.Strategy;
import io.quarkus.oidc.common.runtime.OidcCommonConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;

@Recorder
public class OidcRecorder {

    private static final Logger LOG = Logger.getLogger(OidcRecorder.class);

    private static final Map<String, TenantConfigContext> dynamicTenantsConfig = new ConcurrentHashMap<>();

    public Supplier<DefaultTokenIntrospectionUserInfoCache> setupTokenCache(OidcConfig config, Supplier<Vertx> vertx) {
        return () -> new DefaultTokenIntrospectionUserInfoCache(config, vertx.get());
    }

    public Supplier<TenantConfigBean> setup(OidcConfig config, Supplier<Vertx> vertx, TlsConfig tlsConfig) {
        final Vertx vertxValue = vertx.get();

        String defaultTenantId = config.defaultTenant.getTenantId().orElse(OidcUtils.DEFAULT_TENANT_ID);
        TenantConfigContext defaultTenantContext = createStaticTenantContext(vertxValue, config.defaultTenant, tlsConfig,
                defaultTenantId);

        Map<String, TenantConfigContext> staticTenantsConfig = new HashMap<>();
        for (Map.Entry<String, OidcTenantConfig> tenant : config.namedTenants.entrySet()) {
            OidcCommonUtils.verifyConfigurationId(defaultTenantId, tenant.getKey(), tenant.getValue().getTenantId());
            staticTenantsConfig.put(tenant.getKey(),
                    createStaticTenantContext(vertxValue, tenant.getValue(), tlsConfig, tenant.getKey()));
        }

        return new Supplier<TenantConfigBean>() {
            @Override
            public TenantConfigBean get() {
                return new TenantConfigBean(staticTenantsConfig, dynamicTenantsConfig, defaultTenantContext,
                        new Function<OidcTenantConfig, Uni<TenantConfigContext>>() {
                            @Override
                            public Uni<TenantConfigContext> apply(OidcTenantConfig config) {
                                return createDynamicTenantContext(vertxValue, config, tlsConfig, config.getTenantId().get());
                            }
                        },
                        ExecutorRecorder.getCurrent());
            }
        };
    }

    private Uni<TenantConfigContext> createDynamicTenantContext(Vertx vertx,
            OidcTenantConfig oidcConfig, TlsConfig tlsConfig, String tenantId) {

        if (oidcConfig.logout.backchannel.path.isPresent()) {
            throw new ConfigurationException(
                    "BackChannel Logout is currently not supported for dynamic tenants");
        }
        if (!dynamicTenantsConfig.containsKey(tenantId)) {
            Uni<TenantConfigContext> uniContext = createTenantContext(vertx, oidcConfig, tlsConfig, tenantId);
            uniContext.onFailure().transform(t -> logTenantConfigContextFailure(t, tenantId));
            return uniContext.onItem().transform(
                    new Function<TenantConfigContext, TenantConfigContext>() {
                        @Override
                        public TenantConfigContext apply(TenantConfigContext t) {
                            dynamicTenantsConfig.putIfAbsent(tenantId, t);
                            return t;
                        }
                    });
        } else {
            return Uni.createFrom().item(dynamicTenantsConfig.get(tenantId));
        }
    }

    private TenantConfigContext createStaticTenantContext(Vertx vertx,
            OidcTenantConfig oidcConfig, TlsConfig tlsConfig, String tenantId) {

        Uni<TenantConfigContext> uniContext = createTenantContext(vertx, oidcConfig, tlsConfig, tenantId);
        return uniContext.onFailure()
                .recoverWithItem(new Function<Throwable, TenantConfigContext>() {
                    @Override
                    public TenantConfigContext apply(Throwable t) {
                        if (t instanceof OIDCException) {
                            // OIDC server is not available yet - try to create the connection when the first request arrives
                            LOG.debugf("Tenant '%s': '%s'."
                                    + " Access to resources protected by this tenant may fail"
                                    + " if OIDC server will not become available",
                                    tenantId, t.getMessage());
                            return new TenantConfigContext(null, oidcConfig, false);
                        }
                        logTenantConfigContextFailure(t, tenantId);
                        if (t instanceof ConfigurationException
                                && !oidcConfig.authServerUrl.isPresent() && LaunchMode.DEVELOPMENT == LaunchMode.current()) {
                            // Let it start if it is a DEV mode and auth-server-url has not been configured yet
                            return new TenantConfigContext(null, oidcConfig, false);
                        }
                        // fail in all other cases
                        throw new OIDCException(t);
                    }
                })
                .await().indefinitely();
    }

    private static Throwable logTenantConfigContextFailure(Throwable t, String tenantId) {
        LOG.debugf(
                "'%s' tenant is not initialized: '%s'. Access to resources protected by this tenant will fail.",
                tenantId, t.getMessage());
        return t;
    }

    @SuppressWarnings("resource")
    private Uni<TenantConfigContext> createTenantContext(Vertx vertx, OidcTenantConfig oidcTenantConfig, TlsConfig tlsConfig,
            String tenantId) {
        if (!oidcTenantConfig.tenantId.isPresent()) {
            oidcTenantConfig.tenantId = Optional.of(tenantId);
        }

        final OidcTenantConfig oidcConfig = OidcUtils.resolveProviderConfig(oidcTenantConfig);

        if (!oidcConfig.tenantEnabled) {
            LOG.debugf("'%s' tenant configuration is disabled", tenantId);
            return Uni.createFrom().item(new TenantConfigContext(new OidcProvider(null, null, null, null), oidcConfig));
        }

        if (oidcConfig.getPublicKey().isPresent()) {
            return Uni.createFrom().item(createTenantContextFromPublicKey(oidcConfig));
        }

        try {
            verifyAuthServerUrl(oidcConfig);
            OidcCommonUtils.verifyCommonConfiguration(oidcConfig, isServiceApp(oidcConfig), true);
        } catch (ConfigurationException t) {
            return Uni.createFrom().failure(t);
        }

        if (!oidcConfig.discoveryEnabled.orElse(true)) {
            if (!isServiceApp(oidcConfig)) {
                if (!oidcConfig.authorizationPath.isPresent() || !oidcConfig.tokenPath.isPresent()) {
                    throw new ConfigurationException(
                            "'web-app' applications must have 'authorization-path' and 'token-path' properties "
                                    + "set when the discovery is disabled.",
                            Set.of("quarkus.oidc.authorization-path", "quarkus.oidc.token-path"));
                }
            }
            // JWK and introspection endpoints have to be set for both 'web-app' and 'service' applications
            if (!oidcConfig.jwksPath.isPresent() && !oidcConfig.introspectionPath.isPresent()) {
                if (!oidcConfig.authentication.isIdTokenRequired().orElse(true)
                        && oidcConfig.authentication.isUserInfoRequired().orElse(false)) {
                    LOG.debugf("tenant %s supports only UserInfo", oidcConfig.tenantId.get());
                } else {
                    throw new ConfigurationException(
                            "Either 'jwks-path' or 'introspection-path' properties must be set when the discovery is disabled.",
                            Set.of("quarkus.oidc.jwks-path", "quarkus.oidc.introspection-path"));
                }
            }
        }

        if (isServiceApp(oidcConfig)) {
            if (oidcConfig.token.refreshExpired) {
                throw new ConfigurationException(
                        "The 'token.refresh-expired' property can only be enabled for " + ApplicationType.WEB_APP
                                + " application types");
            }
            if (oidcConfig.logout.path.isPresent()) {
                throw new ConfigurationException(
                        "The 'logout.path' property can only be enabled for " + ApplicationType.WEB_APP
                                + " application types");
            }
            if (oidcConfig.roles.source.isPresent() && oidcConfig.roles.source.get() == Source.idtoken) {
                throw new ConfigurationException(
                        "The 'roles.source' property can only be set to 'idtoken' for " + ApplicationType.WEB_APP
                                + " application types");
            }
        }

        if (oidcConfig.tokenStateManager.strategy != Strategy.KEEP_ALL_TOKENS) {

            if (oidcConfig.authentication.isUserInfoRequired().orElse(false)
                    || oidcConfig.roles.source.orElse(null) == Source.userinfo) {
                throw new ConfigurationException(
                        "UserInfo is required but DefaultTokenStateManager is configured to not keep the access token");
            }
            if (oidcConfig.roles.source.orElse(null) == Source.accesstoken) {
                throw new ConfigurationException(
                        "Access token is required to check the roles but DefaultTokenStateManager is configured to not keep the access token");
            }
        }

        if (oidcConfig.token.verifyAccessTokenWithUserInfo) {
            if (!oidcConfig.authentication.isUserInfoRequired().orElse(false)) {
                throw new ConfigurationException(
                        "UserInfo is not required but 'verifyAccessTokenWithUserInfo' is enabled");
            }
            if (!oidcConfig.isDiscoveryEnabled().orElse(true)) {
                if (oidcConfig.userInfoPath.isEmpty()) {
                    throw new ConfigurationException(
                            "UserInfo path is missing but 'verifyAccessTokenWithUserInfo' is enabled");
                }
                if (oidcConfig.introspectionPath.isPresent()) {
                    throw new ConfigurationException(
                            "Introspection path is configured and 'verifyAccessTokenWithUserInfo' is enabled, these options are mutually exclusive");
                }
            }
        }

        return createOidcProvider(oidcConfig, tlsConfig, vertx)
                .onItem().transform(p -> new TenantConfigContext(p, oidcConfig));
    }

    private static TenantConfigContext createTenantContextFromPublicKey(OidcTenantConfig oidcConfig) {
        if (!isServiceApp(oidcConfig)) {
            throw new ConfigurationException("'public-key' property can only be used with the 'service' applications");
        }
        LOG.debug("'public-key' property for the local token verification is set,"
                + " no connection to the OIDC server will be created");

        return new TenantConfigContext(
                new OidcProvider(oidcConfig.publicKey.get(), oidcConfig, readTokenDecryptionKey(oidcConfig)), oidcConfig);
    }

    public void setSecurityEventObserved(boolean isSecurityEventObserved) {
        DefaultTenantConfigResolver bean = Arc.container().instance(DefaultTenantConfigResolver.class).get();
        bean.setSecurityEventObserved(isSecurityEventObserved);
    }

    public static Optional<ProxyOptions> toProxyOptions(OidcCommonConfig.Proxy proxyConfig) {
        return OidcCommonUtils.toProxyOptions(proxyConfig);
    }

    protected static OIDCException toOidcException(Throwable cause, String authServerUrl) {
        final String message = OidcCommonUtils.formatConnectionErrorMessage(authServerUrl);
        LOG.debug(message);
        return new OIDCException("OIDC Server is not available", cause);
    }

    protected static Uni<OidcProvider> createOidcProvider(OidcTenantConfig oidcConfig, TlsConfig tlsConfig, Vertx vertx) {
        return createOidcClientUni(oidcConfig, tlsConfig, vertx).onItem()
                .transformToUni(new Function<OidcProviderClient, Uni<? extends OidcProvider>>() {
                    @Override
                    public Uni<OidcProvider> apply(OidcProviderClient client) {
                        if (client.getMetadata().getJsonWebKeySetUri() != null
                                && !oidcConfig.token.requireJwtIntrospectionOnly) {
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
        if (oidcConfig.token.decryptionKeyLocation.isPresent()) {
            try {
                Key key = null;

                String keyContent = KeyUtils.readKeyContent(oidcConfig.token.decryptionKeyLocation.get());
                if (keyContent != null) {
                    List<JsonWebKey> keys = KeyUtils.loadJsonWebKeys(keyContent);
                    if (keys != null && keys.size() == 1 &&
                            (keys.get(0).getAlgorithm() == null
                                    || keys.get(0).getAlgorithm() == KeyEncryptionAlgorithm.RSA_OAEP.getAlgorithm())
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
                                oidcConfig.tenantId.get(), oidcConfig.token.decryptionKeyLocation.get()),
                        ex);
            }
        } else {
            return null;
        }
    }

    protected static Uni<JsonWebKeySet> getJsonWebSetUni(OidcProviderClient client, OidcTenantConfig oidcConfig) {
        if (!oidcConfig.isDiscoveryEnabled().orElse(true)) {
            final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
            return client.getJsonWebKeySet().onFailure(OidcCommonUtils.oidcEndpointNotAvailable())
                    .retry()
                    .withBackOff(OidcCommonUtils.CONNECTION_BACKOFF_DURATION, OidcCommonUtils.CONNECTION_BACKOFF_DURATION)
                    .expireIn(connectionDelayInMillisecs)
                    .onFailure()
                    .transform(t -> toOidcException(t, oidcConfig.authServerUrl.get()))
                    .onFailure()
                    .invoke(client::close);
        } else {
            return client.getJsonWebKeySet();
        }
    }

    protected static Uni<OidcProviderClient> createOidcClientUni(OidcTenantConfig oidcConfig,
            TlsConfig tlsConfig, Vertx vertx) {

        String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);

        WebClientOptions options = new WebClientOptions();

        OidcCommonUtils.setHttpClientOptions(oidcConfig, tlsConfig, options);

        WebClient client = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

        Uni<OidcConfigurationMetadata> metadataUni = null;
        if (!oidcConfig.discoveryEnabled.orElse(true)) {
            metadataUni = Uni.createFrom().item(createLocalMetadata(oidcConfig, authServerUriString));
        } else {
            final long connectionDelayInMillisecs = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
            metadataUni = OidcCommonUtils.discoverMetadata(client, authServerUriString, connectionDelayInMillisecs)
                    .onItem()
                    .transform(
                            json -> new OidcConfigurationMetadata(json, createLocalMetadata(oidcConfig, authServerUriString)));
        }
        return metadataUni.onItemOrFailure()
                .transformToUni(new BiFunction<OidcConfigurationMetadata, Throwable, Uni<? extends OidcProviderClient>>() {

                    @Override
                    public Uni<OidcProviderClient> apply(OidcConfigurationMetadata metadata, Throwable t) {
                        if (t != null) {
                            client.close();
                            return Uni.createFrom().failure(toOidcException(t, authServerUriString));
                        }
                        if (metadata == null) {
                            client.close();
                            return Uni.createFrom().failure(new ConfigurationException(
                                    "OpenId Connect Provider configuration metadata is not configured and can not be discovered"));
                        }
                        if (oidcConfig.logout.path.isPresent()) {
                            if (!oidcConfig.endSessionPath.isPresent() && metadata.getEndSessionUri() == null) {
                                client.close();
                                return Uni.createFrom().failure(new ConfigurationException(
                                        "The application supports RP-Initiated Logout but the OpenID Provider does not advertise the end_session_endpoint"));
                            }
                        }
                        return Uni.createFrom().item(new OidcProviderClient(client, metadata, oidcConfig));
                    }
                });
    }

    private static OidcConfigurationMetadata createLocalMetadata(OidcTenantConfig oidcConfig, String authServerUriString) {
        String tokenUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.tokenPath);
        String introspectionUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString,
                oidcConfig.introspectionPath);
        String authorizationUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString,
                oidcConfig.authorizationPath);
        String jwksUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.jwksPath);
        String userInfoUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.userInfoPath);
        String endSessionUri = OidcCommonUtils.getOidcEndpointUrl(authServerUriString, oidcConfig.endSessionPath);
        return new OidcConfigurationMetadata(tokenUri,
                introspectionUri, authorizationUri, jwksUri, userInfoUri, endSessionUri,
                oidcConfig.token.issuer.orElse(null));
    }

    private static boolean isServiceApp(OidcTenantConfig oidcConfig) {
        return ApplicationType.SERVICE.equals(oidcConfig.applicationType.orElse(ApplicationType.SERVICE));
    }

    private static void verifyAuthServerUrl(OidcCommonConfig oidcConfig) {
        if (!oidcConfig.getAuthServerUrl().isPresent()) {
            throw new ConfigurationException("'quarkus.oidc.auth-server-url' property must be configured");
        }
        OidcCommonUtils.verifyEndpointUrl(oidcConfig.getAuthServerUrl().get());
    }

}
