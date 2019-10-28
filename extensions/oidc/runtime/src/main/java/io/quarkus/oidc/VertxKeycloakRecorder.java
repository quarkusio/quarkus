package io.quarkus.oidc;

import java.util.concurrent.CompletableFuture;

import io.quarkus.arc.runtime.BeanContainer;
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
public class VertxKeycloakRecorder {

    public void setup(OidcConfig config, RuntimeValue<Vertx> vertx, BeanContainer beanContainer) {
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

        OAuth2Auth auth = cf.join();
        VertxOAuth2IdentityProvider identityProvider = beanContainer.instance(VertxOAuth2IdentityProvider.class);
        identityProvider.setAuth(auth);
        identityProvider.setConfig(config);
        VertxOAuth2AuthenticationMechanism mechanism = beanContainer.instance(VertxOAuth2AuthenticationMechanism.class);
        mechanism.setAuth(auth);
        mechanism.setAuthServerURI(config.authServerUrl);
    }

    protected static OIDCException toOidcException(Throwable cause) {
        final String message = "OIDC server is not available at the 'quarkus.oidc.auth-server-url' URL. "
                + "Please make sure it is correct. Note it has to end with a realm value if you work with Keycloak, for example:"
                + " 'https://localhost:8180/auth/realms/quarkus'";
        return new OIDCException(message, cause);
    }

}
