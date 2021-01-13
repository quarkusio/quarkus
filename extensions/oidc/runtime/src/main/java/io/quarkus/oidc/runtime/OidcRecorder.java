package io.quarkus.oidc.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.OidcTenantConfig.TokenStateManager.Strategy;
import io.quarkus.oidc.common.runtime.OidcCommonConfig;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.Vertx;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.jwt.JWTOptions;

@Recorder
public class OidcRecorder {

    private static final Logger LOG = Logger.getLogger(OidcRecorder.class);
    private static final String DEFAULT_TENANT_ID = "Default";

    private static final Map<String, TenantConfigContext> dynamicTenantsConfig = new ConcurrentHashMap<>();

    public Supplier<TenantConfigBean> setup(OidcConfig config, Supplier<Vertx> vertx, TlsConfig tlsConfig) {
        final Vertx vertxValue = vertx.get();

        String defaultTenantId = config.defaultTenant.getTenantId().orElse(DEFAULT_TENANT_ID);
        TenantConfigContext defaultTenantContext = createTenantContext(vertxValue, config.defaultTenant, tlsConfig,
                defaultTenantId);

        Map<String, TenantConfigContext> staticTenantsConfig = new HashMap<>();
        for (Map.Entry<String, OidcTenantConfig> tenant : config.namedTenants.entrySet()) {
            OidcCommonUtils.verifyConfigurationId(defaultTenantId, tenant.getKey(), tenant.getValue().getTenantId());
            staticTenantsConfig.put(tenant.getKey(),
                    createTenantContext(vertxValue, tenant.getValue(), tlsConfig, tenant.getKey()));
        }

        return new Supplier<TenantConfigBean>() {
            @Override
            public TenantConfigBean get() {
                return new TenantConfigBean(staticTenantsConfig, dynamicTenantsConfig, defaultTenantContext,
                        new Function<OidcTenantConfig, Uni<TenantConfigContext>>() {
                            @Override
                            public Uni<TenantConfigContext> apply(OidcTenantConfig config) {

                                return Uni.createFrom().emitter(new Consumer<UniEmitter<? super TenantConfigContext>>() {
                                    @Override
                                    public void accept(UniEmitter<? super TenantConfigContext> uniEmitter) {
                                        if (BlockingOperationControl.isBlockingAllowed()) {
                                            createDynamicTenantContext(uniEmitter, vertxValue, config, tlsConfig,
                                                    config.getTenantId().get());
                                        } else {
                                            ExecutorRecorder.getCurrent().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    createDynamicTenantContext(uniEmitter, vertxValue, config, tlsConfig,
                                                            config.getTenantId().get());
                                                }
                                            });
                                        }
                                    }
                                });

                            }
                        },
                        ExecutorRecorder.getCurrent());
            }
        };
    }

    private void createDynamicTenantContext(UniEmitter<? super TenantConfigContext> uniEmitter, Vertx vertx,
            OidcTenantConfig oidcConfig, TlsConfig tlsConfig, String tenantId) {
        try {
            if (!dynamicTenantsConfig.containsKey(tenantId)) {
                dynamicTenantsConfig.putIfAbsent(tenantId, createTenantContext(vertx, oidcConfig, tlsConfig, tenantId));
            }
            uniEmitter.complete(dynamicTenantsConfig.get(tenantId));
        } catch (Throwable t) {
            uniEmitter.fail(t);
        }
    }

    private TenantConfigContext createTenantContext(Vertx vertx, OidcTenantConfig oidcConfig, TlsConfig tlsConfig,
            String tenantId) {
        if (!oidcConfig.tenantId.isPresent()) {
            oidcConfig.tenantId = Optional.of(tenantId);
        }
        if (!oidcConfig.tenantEnabled) {
            LOG.debugf("'%s' tenant configuration is disabled", tenantId);
            return new TenantConfigContext(new OidcRuntimeClient(null), oidcConfig);
        }

        OAuth2ClientOptions options = new OAuth2ClientOptions();

        if (oidcConfig.getClientId().isPresent()) {
            options.setClientID(oidcConfig.getClientId().get());
        }

        if (oidcConfig.getToken().issuer.isPresent()) {
            options.setValidateIssuer(false);
        }

        if (oidcConfig.getToken().getLifespanGrace().isPresent()) {
            JWTOptions jwtOptions = new JWTOptions();
            jwtOptions.setLeeway(oidcConfig.getToken().getLifespanGrace().getAsInt());
            options.setJWTOptions(jwtOptions);
        }

        if (oidcConfig.getPublicKey().isPresent()) {
            return createdTenantContextFromPublicKey(options, oidcConfig);
        }

        OidcCommonUtils.verifyCommonConfiguration(oidcConfig);

        // Base IDP server URL
        String authServerUrl = OidcCommonUtils.getAuthServerUrl(oidcConfig);
        options.setSite(authServerUrl);

        if (!oidcConfig.discoveryEnabled) {
            if (oidcConfig.applicationType != ApplicationType.SERVICE) {
                if (!oidcConfig.authorizationPath.isPresent() || !oidcConfig.tokenPath.isPresent()) {
                    throw new OIDCException("'web-app' applications must have 'authorization-path' and 'token-path' properties "
                            + "set when the discovery is disabled.");
                }
                // These endpoints can only be used with the code flow
                options.setAuthorizationPath(OidcCommonUtils.getOidcEndpointUrl(authServerUrl, oidcConfig.authorizationPath));
                options.setTokenPath(OidcCommonUtils.getOidcEndpointUrl(authServerUrl, oidcConfig.tokenPath));
            }

            if (oidcConfig.getUserInfoPath().isPresent()) {
                options.setUserInfoPath(OidcCommonUtils.getOidcEndpointUrl(authServerUrl, oidcConfig.userInfoPath));
            }

            // JWK and introspection endpoints have to be set for both 'web-app' and 'service' applications  
            if (!oidcConfig.jwksPath.isPresent() && !oidcConfig.introspectionPath.isPresent()) {
                throw new OIDCException(
                        "Either 'jwks-path' or 'introspection-path' properties must be set when the discovery is disabled.");
            }

            if (oidcConfig.getIntrospectionPath().isPresent()) {
                options.setIntrospectionPath(OidcCommonUtils.getOidcEndpointUrl(authServerUrl, oidcConfig.introspectionPath));
            }

            if (oidcConfig.getJwksPath().isPresent()) {
                options.setJwkPath(OidcCommonUtils.getOidcEndpointUrl(authServerUrl, oidcConfig.jwksPath));
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

        // TODO: The workaround to support client_secret_post is added below and have to be removed once
        // it is supported again in VertX OAuth2.
        Credentials creds = oidcConfig.getCredentials();
        if (OidcCommonUtils.isClientSecretBasicAuthRequired(creds)) {
            // If it is set for client_secret_post as well then VertX OAuth2 will only use client_secret_basic
            options.setClientSecret(OidcCommonUtils.clientSecret(creds));
        } else {
            // Avoid VertX OAuth2 setting a null client_secret form parameter if it is client_secret_post or client_secret_jwt
            options.setClientSecretParameterName(null);
        }

        OidcCommonUtils.setHttpClientOptions(oidcConfig, tlsConfig, options);

        final long connectionRetryCount = OidcCommonUtils.getConnectionRetryCount(oidcConfig);
        if (connectionRetryCount > 1) {
            LOG.infof("Connecting to IDP for up to %d times every 2 seconds", connectionRetryCount);
        }

        OidcRuntimeClient client = null;
        for (long i = 0; i < connectionRetryCount; i++) {
            try {
                if (oidcConfig.discoveryEnabled) {
                    client = OidcRuntimeClient.discoverOidcEndpoints(vertx, options, oidcConfig);
                } else {
                    client = OidcRuntimeClient.setOidcEndpoints(vertx, options, oidcConfig);
                }

                break;
            } catch (Throwable throwable) {
                while (throwable instanceof CompletionException && throwable.getCause() != null) {
                    throwable = throwable.getCause();
                }
                if (throwable instanceof OIDCException) {
                    if (i + 1 < connectionRetryCount) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException iex) {
                            // continue connecting
                        }
                    } else {
                        throw (OIDCException) throwable;
                    }
                } else {
                    throw new OIDCException(throwable);
                }
            }
        }

        if (oidcConfig.logout.path.isPresent()) {
            if (!oidcConfig.endSessionPath.isPresent() && client.getLogoutPath() == null) {
                throw new RuntimeException(
                        "The application supports RP-Initiated Logout but the OpenID Provider does not advertise the end_session_endpoint");
            }
        }

        return new TenantConfigContext(client, oidcConfig);
    }

    private static TenantConfigContext createdTenantContextFromPublicKey(OAuth2ClientOptions options,
            OidcTenantConfig oidcConfig) {
        if (oidcConfig.applicationType != ApplicationType.SERVICE) {
            throw new ConfigurationException("'public-key' property can only be used with the 'service' applications");
        }
        LOG.debug("'public-key' property for the local token verification is set,"
                + " no connection to the OIDC server will be created");
        return new TenantConfigContext(OidcRuntimeClient.createClientWithPublicKey(options, oidcConfig.publicKey.get()),
                oidcConfig);
    }

    public void setSecurityEventObserved(boolean isSecurityEventObserved) {
        DefaultTenantConfigResolver bean = Arc.container().instance(DefaultTenantConfigResolver.class).get();
        bean.setSecurityEventObserved(isSecurityEventObserved);
    }

    public static Optional<ProxyOptions> toProxyOptions(OidcCommonConfig.Proxy proxyConfig) {
        return OidcCommonUtils.toProxyOptions(proxyConfig);
    }
}
