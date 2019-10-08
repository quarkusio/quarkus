package io.quarkus.oidc;

import java.security.SecureRandom;
import java.util.Base64;
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
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;

@Recorder
public class VertxKeycloakRecorder {

    public void setup(OidcConfig config, RuntimeValue<Vertx> vertx, BeanContainer beanContainer) {
        OAuth2ClientOptions options = new OAuth2ClientOptions();

        // Base IDP server URL
        options.setSite(config.authServerUrl);
        // RFC7662 introspection service address
        options.setIntrospectionPath(config.introspectionPath);

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

        //TODO: remove this temporary code block
        byte[] bogus = new byte[512];
        new SecureRandom().nextBytes(bogus);

        options.addPubSecKey(
                new PubSecKeyOptions().setSymmetric(true).setPublicKey(Base64.getEncoder().encodeToString(bogus))
                        .setAlgorithm("HS512"));
        options.setFlow(OAuth2FlowType.AUTH_JWT);
        // End of the temporary code block

        CompletableFuture<OAuth2Auth> cf = new CompletableFuture<>();
        KeycloakAuth.discover(vertx.getValue(), options, new Handler<AsyncResult<OAuth2Auth>>() {
            @Override
            public void handle(AsyncResult<OAuth2Auth> event) {
                if (event.failed()) {
                    cf.completeExceptionally(event.cause());
                } else {
                    cf.complete(event.result());
                }
            }
        });

        OAuth2Auth auth = cf.join();
        beanContainer.instance(VertxOAuth2IdentityProvider.class).setAuth(auth);
        VertxOAuth2AuthenticationMechanism mechanism = beanContainer.instance(VertxOAuth2AuthenticationMechanism.class);
        mechanism.setAuth(auth);
        mechanism.setAuthServerURI(config.authServerUrl);
        mechanism.setConfig(config);

    }
}
