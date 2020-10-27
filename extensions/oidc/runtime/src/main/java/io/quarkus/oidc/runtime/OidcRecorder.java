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
import io.quarkus.oidc.OidcTenantConfig.Credentials;
import io.quarkus.oidc.OidcTenantConfig.Credentials.Secret;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.OidcTenantConfig.Tls.Verification;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.jwt.JWTOptions;

@Recorder
public class OidcRecorder {

    private static final Logger LOG = Logger.getLogger(OidcRecorder.class);

    private static final Map<String, TenantConfigContext> dynamicTenantsConfig = new ConcurrentHashMap<>();

    public Supplier<TenantConfigBean> setup(OidcConfig config, Supplier<Vertx> vertx, TlsConfig tlsConfig) {
        final Vertx vertxValue = vertx.get();
        Map<String, TenantConfigContext> staticTenantsConfig = new HashMap<>();

        for (Map.Entry<String, OidcTenantConfig> tenant : config.namedTenants.entrySet()) {
            if (config.defaultTenant.getTenantId().isPresent()
                    && tenant.getKey().equals(config.defaultTenant.getTenantId().get())) {
                throw new OIDCException("tenant-id '" + tenant.getKey() + "' duplicates the default tenant-id");
            }
            if (tenant.getValue().getTenantId().isPresent() && !tenant.getKey().equals(tenant.getValue().getTenantId().get())) {
                throw new OIDCException("Configuration has 2 different tenant-id values: '"
                        + tenant.getKey() + "' and '" + tenant.getValue().getTenantId().get() + "'");
            }
            staticTenantsConfig.put(tenant.getKey(),
                    createTenantContext(vertxValue, tenant.getValue(), tlsConfig, tenant.getKey()));
        }

        TenantConfigContext tenantContext = createTenantContext(vertxValue, config.defaultTenant, tlsConfig, "Default");

        return new Supplier<TenantConfigBean>() {
            @Override
            public TenantConfigBean get() {
                return new TenantConfigBean(staticTenantsConfig, dynamicTenantsConfig, tenantContext,
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
            LOG.debugf("%s tenant configuration is disabled", tenantId);
            return new TenantConfigContext(null, oidcConfig);
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

        if (!oidcConfig.getAuthServerUrl().isPresent()) {
            throw new ConfigurationException(
                    "'auth-server-url' is not present. Both 'auth-server-url' and 'client-id' or alternatively 'public-key' must be configured"
                            + " when the quarkus-oidc extension is enabled");
        }

        if (!oidcConfig.getClientId().isPresent()) {
            throw new ConfigurationException(
                    "'client-id' is not present. Both 'auth-server-url' and 'client-id' or alternatively 'public-key' must be configured"
                            + " when the quarkus-oidc extension is enabled");
        }

        // Base IDP server URL
        String authServerUrl = oidcConfig.getAuthServerUrl().get();
        if (authServerUrl.endsWith("/")) {
            authServerUrl = authServerUrl.substring(0, authServerUrl.length() - 1);
        }
        options.setSite(authServerUrl);

        if (!oidcConfig.discoveryEnabled) {
            if (oidcConfig.applicationType != ApplicationType.SERVICE) {
                if (!oidcConfig.authorizationPath.isPresent() || !oidcConfig.tokenPath.isPresent()) {
                    throw new OIDCException("'web-app' applications must have 'authorization-path' and 'token-path' properties "
                            + "set when the discovery is disabled.");
                }
                // These endpoints can only be used with the code flow
                if (oidcConfig.getAuthorizationPath().isPresent()) {
                    options.setAuthorizationPath(authServerUrl + prependSlash(oidcConfig.getAuthorizationPath().get()));
                }

                if (oidcConfig.getTokenPath().isPresent()) {
                    options.setTokenPath(authServerUrl + prependSlash(oidcConfig.getTokenPath().get()));
                }
            }

            if (oidcConfig.getUserInfoPath().isPresent()) {
                options.setUserInfoPath(authServerUrl + prependSlash(oidcConfig.getUserInfoPath().get()));
            }

            // JWK and introspection endpoints have to be set for both 'web-app' and 'service' applications  
            if (!oidcConfig.jwksPath.isPresent() && !oidcConfig.introspectionPath.isPresent()) {
                throw new OIDCException(
                        "Either 'jwks-path' or 'introspection-path' properties must be set when the discovery is disabled.");
            }

            if (oidcConfig.getIntrospectionPath().isPresent()) {
                options.setIntrospectionPath(authServerUrl + prependSlash(oidcConfig.getIntrospectionPath().get()));
            }

            if (oidcConfig.getJwksPath().isPresent()) {
                options.setJwkPath(authServerUrl + prependSlash(oidcConfig.getJwksPath().get()));
            }

        }

        Credentials creds = oidcConfig.getCredentials();
        if (creds.secret.isPresent() && creds.clientSecret.value.isPresent()) {
            throw new ConfigurationException(
                    "'credentials.secret' and 'credentials.client-secret' properties are mutually exclusive");
        }
        if ((creds.secret.isPresent() || creds.clientSecret.value.isPresent()) && creds.jwt.secret.isPresent()) {
            throw new ConfigurationException(
                    "Use only 'credentials.secret' or 'credentials.client-secret' or 'credentials.jwt.secret' property");
        }

        if (ApplicationType.SERVICE.equals(oidcConfig.applicationType)) {
            if (oidcConfig.token.refreshExpired) {
                throw new RuntimeException(
                        "The 'token.refresh-expired' property can only be enabled for " + ApplicationType.WEB_APP
                                + " application types");
            }
            if (oidcConfig.logout.path.isPresent()) {
                throw new RuntimeException(
                        "The 'logout.path' property can only be enabled for " + ApplicationType.WEB_APP
                                + " application types");
            }
            if (oidcConfig.roles.source.isPresent() && oidcConfig.roles.source.get() == Source.idtoken) {
                throw new RuntimeException(
                        "The 'roles.source' property can only be set to 'idtoken' for " + ApplicationType.WEB_APP
                                + " application types");
            }
        }

        // TODO: The workaround to support client_secret_post is added below and have to be removed once
        // it is supported again in VertX OAuth2.
        if (creds.secret.isPresent() || creds.clientSecret.value.isPresent()
                && creds.clientSecret.method.orElseGet(() -> Secret.Method.BASIC) == Secret.Method.BASIC) {
            // If it is set for client_secret_post as well then VertX OAuth2 will only use client_secret_basic
            options.setClientSecret(creds.secret.orElseGet(() -> creds.clientSecret.value.get()));
        } else {
            // Avoid VertX OAuth2 setting a null client_secret form parameter if it is client_secret_post or client_secret_jwt
            options.setClientSecretParameterName(null);
        }

        Optional<ProxyOptions> proxyOpt = toProxyOptions(oidcConfig.getProxy());
        if (proxyOpt.isPresent()) {
            options.setProxyOptions(proxyOpt.get());
        }

        boolean trustAll = oidcConfig.tls.verification.isPresent() ? oidcConfig.tls.verification.get() == Verification.NONE
                : tlsConfig.trustAll;
        if (trustAll) {
            options.setTrustAll(true);
            options.setVerifyHost(false);
        }

        final long connectionDelayInSecs = oidcConfig.getConnectionDelay().isPresent()
                ? oidcConfig.getConnectionDelay().get().toMillis() / 1000
                : 0;
        final long connectionRetryCount = connectionDelayInSecs > 1 ? connectionDelayInSecs / 2 : 1;
        if (connectionRetryCount > 1) {
            LOG.infof("Connecting to IDP for up to %d times every 2 seconds", connectionRetryCount);
        }

        OAuth2Auth auth = null;
        for (long i = 0; i < connectionRetryCount; i++) {
            try {
                if (oidcConfig.discoveryEnabled) {
                    auth = discoverOidcEndpoints(vertx, options);
                } else {
                    auth = setOidcEndpoints(vertx, options);
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

        String endSessionEndpoint = OAuth2AuthProviderImpl.class.cast(auth).getConfig().getLogoutPath();

        if (oidcConfig.logout.path.isPresent()) {
            if (!oidcConfig.endSessionPath.isPresent() && endSessionEndpoint == null) {
                throw new RuntimeException(
                        "The application supports RP-Initiated Logout but the OpenID Provider does not advertise the end_session_endpoint");
            }
        }

        auth.missingKeyHandler(new JwkSetRefreshHandler(auth, oidcConfig.token.forcedJwkRefreshInterval));
        return new TenantConfigContext(auth, oidcConfig);
    }

    private static String prependSlash(String path) {
        return !path.startsWith("/") ? "/" + path : path;
    }

    private static OAuth2Auth discoverOidcEndpoints(Vertx vertx, OAuth2ClientOptions options) {
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super OAuth2Auth>>() {
            public void accept(UniEmitter<? super OAuth2Auth> uniEmitter) {
                KeycloakAuth.discover(vertx, options, new Handler<AsyncResult<OAuth2Auth>>() {
                    @Override
                    public void handle(AsyncResult<OAuth2Auth> event) {
                        if (event.failed()) {
                            uniEmitter.fail(toOidcException(event.cause()));
                        } else {
                            uniEmitter.complete(event.result());
                        }
                    }
                });
            }
        }).await().indefinitely();
    }

    private static OAuth2Auth setOidcEndpoints(Vertx vertx, OAuth2ClientOptions options) {
        if (options.getJwkPath() != null) {
            return Uni.createFrom().emitter(new Consumer<UniEmitter<? super OAuth2Auth>>() {
                @SuppressWarnings("deprecation")
                @Override
                public void accept(UniEmitter<? super OAuth2Auth> uniEmitter) {
                    OAuth2Auth auth = OAuth2Auth.create(vertx, options);
                    auth.loadJWK(res -> {
                        if (res.failed()) {
                            uniEmitter.fail(toOidcException(res.cause()));
                        }
                        uniEmitter.complete(auth);
                    });
                }
            }).await().indefinitely();
        } else {
            return OAuth2Auth.create(vertx, options);
        }
    }

    @SuppressWarnings("deprecation")
    private static TenantConfigContext createdTenantContextFromPublicKey(OAuth2ClientOptions options,
            OidcTenantConfig oidcConfig) {
        if (oidcConfig.applicationType != ApplicationType.SERVICE) {
            throw new ConfigurationException("'public-key' property can only be used with the 'service' applications");
        }
        LOG.debug("'public-key' property for the local token verification is set,"
                + " no connection to the OIDC server will be created");
        options.addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("RS256")
                .setPublicKey(oidcConfig.getPublicKey().get()));

        return new TenantConfigContext(new OAuth2AuthProviderImpl(null, options), oidcConfig);
    }

    protected static OIDCException toOidcException(Throwable cause) {
        final String message = "OIDC server is not available at the 'quarkus.oidc.auth-server-url' URL. "
                + "Please make sure it is correct. Note it has to end with a realm value if you work with Keycloak, for example:"
                + " 'https://localhost:8180/auth/realms/quarkus'";
        return new OIDCException(message, cause);
    }

    protected static Optional<ProxyOptions> toProxyOptions(OidcTenantConfig.Proxy proxyConfig) {
        // Proxy is enabled if (at least) "host" is configured.
        if (!proxyConfig.host.isPresent()) {
            return Optional.empty();
        }
        JsonObject jsonOptions = new JsonObject();
        jsonOptions.put("host", proxyConfig.host.get());
        jsonOptions.put("port", proxyConfig.port);
        if (proxyConfig.username.isPresent()) {
            jsonOptions.put("username", proxyConfig.username.get());
        }
        if (proxyConfig.password.isPresent()) {
            jsonOptions.put("password", proxyConfig.password.get());
        }
        return Optional.of(new ProxyOptions(jsonOptions));
    }

    public void setSecurityEventObserved(boolean isSecurityEventObserved) {
        DefaultTenantConfigResolver bean = Arc.container().instance(DefaultTenantConfigResolver.class).get();
        bean.setSecurityEventObserved(isSecurityEventObserved);
    }
}
