package io.quarkus.oidc.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.OIDCException;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.jwt.JWTOptions;

@Recorder
public class OidcRecorder {

    private static final Logger LOG = Logger.getLogger(OidcRecorder.class);

    public void setup(OidcConfig config, RuntimeValue<Vertx> vertx, BeanContainer beanContainer) {
        final Vertx vertxValue = vertx.getValue();
        Map<String, TenantConfigContext> tenantsConfig = new HashMap<>();

        for (Map.Entry<String, OidcTenantConfig> tenant : config.namedTenants.entrySet()) {
            tenantsConfig.put(tenant.getKey(), createTenantContext(vertxValue, tenant.getValue()));
        }

        DefaultTenantConfigResolver resolver = beanContainer.instance(DefaultTenantConfigResolver.class);

        resolver.setDefaultTenant(createTenantContext(vertxValue, config.defaultTenant));
        resolver.setTenantsConfig(tenantsConfig);
        resolver.setTenantConfigContextFactory(new Function<OidcTenantConfig, TenantConfigContext>() {
            @Override
            public TenantConfigContext apply(OidcTenantConfig config) {
                return createTenantContext(vertxValue, config);
            }
        });
    }

    private TenantConfigContext createTenantContext(Vertx vertx, OidcTenantConfig oidcConfig) {
        OAuth2ClientOptions options = new OAuth2ClientOptions();

        if (!oidcConfig.getAuthServerUrl().isPresent()) {
            return null;
        }

        // Base IDP server URL
        options.setSite(oidcConfig.getAuthServerUrl().get());
        // RFC7662 introspection service address
        if (oidcConfig.getIntrospectionPath().isPresent()) {
            options.setIntrospectionPath(oidcConfig.getIntrospectionPath().get());
        }

        // RFC7662 JWKS service address
        if (oidcConfig.getJwksPath().isPresent()) {
            options.setJwkPath(oidcConfig.getJwksPath().get());
        }

        if (oidcConfig.getClientId().isPresent()) {
            options.setClientID(oidcConfig.getClientId().get());
        }

        if (oidcConfig.getCredentials().secret.isPresent()) {
            options.setClientSecret(oidcConfig.getCredentials().secret.get());
        }
        if (oidcConfig.getPublicKey().isPresent()) {
            options.addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setPublicKey(oidcConfig.getPublicKey().get()));
        }
        if (oidcConfig.getToken().issuer.isPresent()) {
            options.setValidateIssuer(false);
        }

        if (oidcConfig.getToken().getExpirationGrace().isPresent()) {
            JWTOptions jwtOptions = new JWTOptions();
            jwtOptions.setLeeway(oidcConfig.getToken().getExpirationGrace().get());
            options.setJWTOptions(jwtOptions);
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
                CompletableFuture<OAuth2Auth> cf = new CompletableFuture<>();
                KeycloakAuth.discover(vertx, options, new Handler<AsyncResult<OAuth2Auth>>() {
                    @Override
                    public void handle(AsyncResult<OAuth2Auth> event) {
                        if (event.failed()) {
                            cf.completeExceptionally(toOidcException(event.cause()));
                        } else {
                            cf.complete(event.result());
                        }
                    }
                });

                auth = cf.join();
                break;
            } catch (OIDCException ex) {
                if (i + 1 < connectionRetryCount) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException iex) {
                        // continue connecting
                    }

                } else {
                    throw ex;
                }
            }
        }

        return new TenantConfigContext(auth, oidcConfig);
    }

    protected static OIDCException toOidcException(Throwable cause) {
        final String message = "OIDC server is not available at the 'quarkus.oidc.auth-server-url' URL. "
                + "Please make sure it is correct. Note it has to end with a realm value if you work with Keycloak, for example:"
                + " 'https://localhost:8180/auth/realms/quarkus'";
        return new OIDCException(message, cause);
    }

}
