package io.quarkus.oidc.runtime;

import java.util.concurrent.CompletableFuture;

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

@Recorder
public class OidcRecorder {

    private static final Logger LOG = Logger.getLogger(OidcRecorder.class);

    public void setup(OidcConfig config, OidcBuildTimeConfig btConfig, RuntimeValue<Vertx> vertx, BeanContainer beanContainer) {
        OAuth2ClientOptions options = new OAuth2ClientOptions();

        // Base IDP server URL
        options.setSite(config.authServerUrl);
        // RFC7662 introspection service address
        if (config.introspectionPath.isPresent()) {
            options.setIntrospectionPath(config.introspectionPath.get());
        }

        // RFC7662 JWKS service address
        if (config.jwksPath.isPresent()) {
            options.setJwkPath(config.jwksPath.get());
        }

        if (config.clientId.isPresent()) {
            options.setClientID(config.clientId.get());
        }

        if (config.credentials.secret.isPresent()) {
            options.setClientSecret(config.credentials.secret.get());
        }
        if (config.publicKey.isPresent()) {
            options.addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setPublicKey(config.publicKey.get()));
        }

        final long connectionDelayInSecs = config.connectionDelay.isPresent() ? config.connectionDelay.get().toMillis() / 1000
                : 0;
        final long connectionRetryCount = connectionDelayInSecs > 1 ? connectionDelayInSecs / 2 : 1;
        if (connectionRetryCount > 1) {
            LOG.infof("Connecting to IDP for up to %d times every 2 seconds", connectionRetryCount);
        }

        OAuth2Auth auth = null;
        for (long i = 0; i < connectionRetryCount; i++) {
            try {
                CompletableFuture<OAuth2Auth> cf = new CompletableFuture<>();
                KeycloakAuth.discover(vertx.getValue(), options, new Handler<AsyncResult<OAuth2Auth>>() {
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

        OidcIdentityProvider identityProvider = beanContainer.instance(OidcIdentityProvider.class);
        identityProvider.setAuth(auth);
        identityProvider.setConfig(config);
        AbstractOidcAuthenticationMechanism mechanism = null;

        if (OidcBuildTimeConfig.ApplicationType.SERVICE.equals(btConfig.applicationType)) {
            mechanism = beanContainer.instance(BearerAuthenticationMechanism.class);
        } else if (OidcBuildTimeConfig.ApplicationType.WEB_APP.equals(btConfig.applicationType)) {
            mechanism = beanContainer.instance(CodeAuthenticationMechanism.class);
        }

        mechanism.setAuth(auth, config);
    }

    protected static OIDCException toOidcException(Throwable cause) {
        final String message = "OIDC server is not available at the 'quarkus.oidc.auth-server-url' URL. "
                + "Please make sure it is correct. Note it has to end with a realm value if you work with Keycloak, for example:"
                + " 'https://localhost:8180/auth/realms/quarkus'";
        return new OIDCException(message, cause);
    }

}
