package io.quarkus.oidc.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.runtime.OidcTenantConfig.Credentials;
import io.quarkus.oidc.runtime.OidcTenantConfig.Credentials.Secret;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
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

    public void setup(OidcConfig config, Supplier<Vertx> vertx, BeanContainer beanContainer) {
        final Vertx vertxValue = vertx.get();

        // Default tenant configuration context
        TenantConfigContext defaultTenant = createTenantContext(vertxValue, config.defaultTenant, "Default");

        // Additional tenant configuration contexts
        Map<String, TenantConfigContext> tenantsConfig = new HashMap<>();

        for (Map.Entry<String, OidcTenantConfig> tenant : config.namedTenants.entrySet()) {
            if (config.defaultTenant.getTenantId().isPresent()
                    && tenant.getKey().equals(config.defaultTenant.getTenantId().get())) {
                throw new OIDCException("tenant-id '" + tenant.getKey() + "' duplicates the default tenant-id");
            }
            if (tenant.getValue().getTenantId().isPresent() && !tenant.getKey().equals(tenant.getValue().getTenantId().get())) {
                throw new OIDCException("Configuration has 2 different tenant-id values: '"
                        + tenant.getKey() + "' and '" + tenant.getValue().getTenantId().get() + "'");
            }
            tenantsConfig.put(tenant.getKey(), createTenantContext(vertxValue, tenant.getValue(), tenant.getKey()));
        }

        // Tenant configuration context factory
        Function<OidcTenantConfig, TenantConfigContext> tenantConfigContextFactory = new Function<OidcTenantConfig, TenantConfigContext>() {
            @Override
            public TenantConfigContext apply(OidcTenantConfig config) {
                // OidcTenantConfig resolved by TenantConfigResolver must have its optional tenantId
                // initialized which is also enforced by DefaultTenantConfigResolver
                return createTenantContext(vertxValue, config, config.getTenantId().get());
            }
        };

        DefaultTenantConfigResolver resolver = beanContainer.instance(DefaultTenantConfigResolver.class);
        resolver.completeInitialization(defaultTenant, tenantsConfig, tenantConfigContextFactory);
    }

    private TenantConfigContext createTenantContext(Vertx vertx, OidcTenantConfig oidcConfig, String tenantId) {
        if (!oidcConfig.tenantEnabled) {
            LOG.debugf("%s tenant configuration is disabled", tenantId);
            return null;
        }

        OAuth2ClientOptions options = new OAuth2ClientOptions();

        if (oidcConfig.getClientId().isPresent()) {
            options.setClientID(oidcConfig.getClientId().get());
        }

        if (oidcConfig.getToken().issuer.isPresent()) {
            options.setValidateIssuer(false);
        }

        if (oidcConfig.getToken().getExpirationGrace().isPresent()) {
            JWTOptions jwtOptions = new JWTOptions();
            jwtOptions.setLeeway(oidcConfig.getToken().getExpirationGrace().get());
            options.setJWTOptions(jwtOptions);
        }

        if (oidcConfig.getPublicKey().isPresent()) {
            if (oidcConfig.applicationType == ApplicationType.WEB_APP) {
                throw new ConfigurationException("'public-key' property can only be used with the 'service' applications");
            }
            LOG.info("'public-key' property for the local token verification is set,"
                    + " no connection to the OIDC server will be created");
            options.addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setPublicKey(oidcConfig.getPublicKey().get()));

            return new TenantConfigContext(new OAuth2AuthProviderImpl(vertx, options), oidcConfig);
        }

        if (!oidcConfig.getAuthServerUrl().isPresent() || !oidcConfig.getClientId().isPresent()) {
            throw new ConfigurationException(
                    "Both 'auth-server-url' and 'client-id' or alterntively 'public-key' must be configured"
                            + " when the quarkus-oidc extension is enabled");
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

        Credentials creds = oidcConfig.getCredentials();
        if (creds.secret.isPresent() && (creds.clientSecret.value.isPresent() || creds.clientSecret.method.isPresent())) {
            throw new ConfigurationException(
                    "'credentials.secret' and 'credentials.client-secret' properties are mutually exclusive");
        }
        // TODO: The workaround to support client_secret_post is added below and have to be removed once
        // it is supported again in VertX OAuth2.
        if (creds.secret.isPresent()
                || creds.clientSecret.value.isPresent()
                        && creds.clientSecret.method.orElseGet(() -> Secret.Method.BASIC) == Secret.Method.BASIC) {
            // If it is set for client_secret_post as well then VertX OAuth2 will only use client_secret_basic
            options.setClientSecret(creds.secret.orElseGet(() -> creds.clientSecret.value.get()));
        } else {
            // Avoid the client_secret set in CodeAuthenticationMechanism when client_secret_post is enabled
            // from being reset to null in VertX OAuth2
            options.setClientSecretParameterName(null);
        }

        Optional<ProxyOptions> proxyOpt = toProxyOptions(oidcConfig.getProxy());
        if (proxyOpt.isPresent()) {
            options.setProxyOptions(proxyOpt.get());
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

        return new TenantConfigContext(auth, oidcConfig);
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

}
