package io.quarkus.oidc.runtime;

import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.OidcTenantConfig.TokenStateManager.Strategy;
import io.quarkus.oidc.common.runtime.OidcCommonConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;

@Recorder
public class OidcRecorder {

    private static final Logger LOG = Logger.getLogger(OidcRecorder.class);
    private static final String DEFAULT_TENANT_ID = "Default";
    private static final Duration CONNECTION_BACKOFF_DURATION = Duration.ofSeconds(2);

    private static final Map<String, TenantConfigContext> dynamicTenantsConfig = new ConcurrentHashMap<>();

    public Supplier<TenantConfigBean> setup(OidcConfig config, Supplier<Vertx> vertx, TlsConfig tlsConfig) {
        final Vertx vertxValue = vertx.get();

        String defaultTenantId = config.defaultTenant.getTenantId().orElse(DEFAULT_TENANT_ID);
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
                                return createDynamicTenantContext(vertxValue, config, tlsConfig, config.getTenantId().get())
                                        .plug(u -> {
                                            if (!BlockingOperationControl.isBlockingAllowed()) {
                                                return u.runSubscriptionOn(ExecutorRecorder.getCurrent());
                                            }
                                            return u;
                                        });
                            }
                        },
                        ExecutorRecorder.getCurrent());
            }
        };
    }

    private Uni<TenantConfigContext> createDynamicTenantContext(Vertx vertx,
            OidcTenantConfig oidcConfig, TlsConfig tlsConfig, String tenantId) {

        if (!dynamicTenantsConfig.containsKey(tenantId)) {
            return createTenantContext(vertx, oidcConfig, tlsConfig, tenantId).onItem().transform(
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

        return createTenantContext(vertx, oidcConfig, tlsConfig, tenantId).await().indefinitely();
    }

    private Uni<TenantConfigContext> createTenantContext(Vertx vertx, OidcTenantConfig oidcConfig, TlsConfig tlsConfig,
            String tenantId) {
        if (!oidcConfig.tenantId.isPresent()) {
            oidcConfig.tenantId = Optional.of(tenantId);
        }
        if (!oidcConfig.tenantEnabled) {
            LOG.debugf("'%s' tenant configuration is disabled", tenantId);
            Uni.createFrom().item(new TenantConfigContext(new OidcProvider(null, null, null), oidcConfig));
        }

        if (oidcConfig.getPublicKey().isPresent()) {
            return Uni.createFrom().item(createTenantContextFromPublicKey(oidcConfig));
        }

        OidcCommonUtils.verifyCommonConfiguration(oidcConfig, true);

        if (!oidcConfig.discoveryEnabled) {
            if (oidcConfig.applicationType != ApplicationType.SERVICE) {
                if (!oidcConfig.authorizationPath.isPresent() || !oidcConfig.tokenPath.isPresent()) {
                    throw new OIDCException("'web-app' applications must have 'authorization-path' and 'token-path' properties "
                            + "set when the discovery is disabled.");
                }
            }
            // JWK and introspection endpoints have to be set for both 'web-app' and 'service' applications  
            if (!oidcConfig.jwksPath.isPresent() && !oidcConfig.introspectionPath.isPresent()) {
                throw new OIDCException(
                        "Either 'jwks-path' or 'introspection-path' properties must be set when the discovery is disabled.");
            }
        }

        if (ApplicationType.SERVICE.equals(oidcConfig.applicationType)) {
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

            if (oidcConfig.authentication.userInfoRequired || oidcConfig.roles.source.orElse(null) == Source.userinfo) {
                throw new ConfigurationException(
                        "UserInfo is required but DefaultTokenStateManager is configured to not keep the access token");
            }
            if (oidcConfig.roles.source.orElse(null) == Source.accesstoken) {
                throw new ConfigurationException(
                        "Access token is required to check the roles but DefaultTokenStateManager is configured to not keep the access token");
            }
        }

        return createOidcProvider(oidcConfig, tlsConfig, vertx)
                .onItem().transform(p -> new TenantConfigContext(p, oidcConfig));
    }

    private static TenantConfigContext createTenantContextFromPublicKey(OidcTenantConfig oidcConfig) {
        if (oidcConfig.applicationType != ApplicationType.SERVICE) {
            throw new ConfigurationException("'public-key' property can only be used with the 'service' applications");
        }
        LOG.debug("'public-key' property for the local token verification is set,"
                + " no connection to the OIDC server will be created");

        return new TenantConfigContext(new OidcProvider(oidcConfig.publicKey.get(), oidcConfig), oidcConfig);
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
        return new OIDCException(message, cause);
    }

    protected static Uni<OidcProvider> createOidcProvider(OidcTenantConfig oidcConfig, TlsConfig tlsConfig, Vertx vertx) {
        return createOidcClientUni(oidcConfig, tlsConfig, vertx).onItem()
                .transformToUni(new Function<OidcProviderClient, Uni<? extends OidcProvider>>() {
                    @Override
                    public Uni<OidcProvider> apply(OidcProviderClient client) {
                        if (client.getMetadata().getJsonWebKeySetUri() != null) {
                            return client.getJsonWebKeySet().onItem()
                                    .transform(new Function<JsonWebKeyCache, OidcProvider>() {

                                        @Override
                                        public OidcProvider apply(JsonWebKeyCache jwks) {
                                            return new OidcProvider(client, oidcConfig, jwks);
                                        }

                                    });
                        } else {
                            return Uni.createFrom().item(new OidcProvider(client, oidcConfig, null));
                        }
                    }
                });
    }

    protected static Uni<OidcProviderClient> createOidcClientUni(OidcTenantConfig oidcConfig,
            TlsConfig tlsConfig, Vertx vertx) {

        String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcConfig);

        WebClientOptions options = new WebClientOptions();

        URI authServerUri = URI.create(authServerUriString); // create uri for parse exception
        OidcCommonUtils.setHttpClientOptions(oidcConfig, tlsConfig, options);

        WebClient client = WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);

        Uni<OidcConfigurationMetadata> metadataUni = null;
        if (!oidcConfig.discoveryEnabled) {
            String tokenUri = OidcCommonUtils.getOidcEndpointUrl(authServerUri.toString(), oidcConfig.tokenPath);
            String introspectionUri = OidcCommonUtils.getOidcEndpointUrl(authServerUri.toString(),
                    oidcConfig.introspectionPath);
            String authorizationUri = OidcCommonUtils.getOidcEndpointUrl(authServerUri.toString(),
                    oidcConfig.authorizationPath);
            String jwksUri = OidcCommonUtils.getOidcEndpointUrl(authServerUri.toString(), oidcConfig.jwksPath);
            String userInfoUri = OidcCommonUtils.getOidcEndpointUrl(authServerUri.toString(), oidcConfig.userInfoPath);
            String endSessionUri = OidcCommonUtils.getOidcEndpointUrl(authServerUri.toString(), oidcConfig.endSessionPath);
            metadataUni = Uni.createFrom().item(new OidcConfigurationMetadata(tokenUri,
                    introspectionUri, authorizationUri, jwksUri, userInfoUri, endSessionUri,
                    oidcConfig.token.issuer.orElse(null)));
        } else {
            final long connectionRetryCount = OidcCommonUtils.getConnectionRetryCount(oidcConfig);
            final long expireInDelay = OidcCommonUtils.getConnectionDelayInMillis(oidcConfig);
            if (connectionRetryCount > 1) {
                LOG.infof("Connecting to IDP for up to %d times every 2 seconds", connectionRetryCount);
            }
            metadataUni = discoverMetadata(client, authServerUri.toString(), oidcConfig).onFailure(ConnectException.class)
                    .retry()
                    .withBackOff(CONNECTION_BACKOFF_DURATION, CONNECTION_BACKOFF_DURATION)
                    .expireIn(expireInDelay);
        }
        return metadataUni.onItemOrFailure()
                .transformToUni(new BiFunction<OidcConfigurationMetadata, Throwable, Uni<? extends OidcProviderClient>>() {

                    @Override
                    public Uni<OidcProviderClient> apply(OidcConfigurationMetadata metadata, Throwable t) {
                        if (t != null) {
                            return Uni.createFrom().failure(toOidcException(t, authServerUri.toString()));
                        }
                        if (metadata == null) {
                            return Uni.createFrom().failure(new ConfigurationException(
                                    "OpenId Connect Provider configuration metadata is not configured and can not be discovered"));
                        }
                        if (oidcConfig.logout.path.isPresent()) {
                            if (!oidcConfig.endSessionPath.isPresent() && metadata.getEndSessionUri() == null) {
                                return Uni.createFrom().failure(new ConfigurationException(
                                        "The application supports RP-Initiated Logout but the OpenID Provider does not advertise the end_session_endpoint"));
                            }
                        }
                        return Uni.createFrom().item(new OidcProviderClient(client, metadata, oidcConfig));
                    }
                });
    }

    private static Uni<OidcConfigurationMetadata> discoverMetadata(WebClient client, String authServerUrl,
            OidcTenantConfig oidcConfig) {
        final String discoveryUrl = authServerUrl + "/.well-known/openid-configuration";
        return client.getAbs(discoveryUrl).send().onItem().transform(resp -> {
            if (resp.statusCode() == 200) {
                return new OidcConfigurationMetadata(resp.bodyAsJsonObject());
            } else {
                return null;
            }
        });
    }

}
