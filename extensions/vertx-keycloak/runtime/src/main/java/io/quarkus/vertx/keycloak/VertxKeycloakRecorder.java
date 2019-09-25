package io.quarkus.vertx.keycloak;

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

    public void setup(KeycloakConfig config, RuntimeValue<Vertx> vertx, BeanContainer beanContainer) {
        OAuth2ClientOptions options = new OAuth2ClientOptions();

        if (config.resource.isPresent()) {
            options.setClientID(config.resource.get());
        }

        if (config.credentials.secret.isPresent()) {
            options.setClientSecret(config.credentials.secret.get());
        }

        if (!config.publicClient) {
            options.setUseBasicAuthorizationHeader(true);
        }

        final String realm = config.realm;

        String siteUri = config.authServerUrl + "/realms/" + realm;
        options.setSite(siteUri);
        options.setAuthorizationPath("/realms/" + realm + "/protocol/openid-connect/auth");
        options.setTokenPath("/realms/" + realm + "/protocol/openid-connect/token");
        options.setRevocationPath(null);
        options.setLogoutPath("/realms/" + realm + "/protocol/openid-connect/logout");
        options.setUserInfoPath("/realms/" + realm + "/protocol/openid-connect/userinfo");
        // keycloak follows the RFC7662
        options.setIntrospectionPath("/realms/" + realm + "/protocol/openid-connect/token/introspect");
        // keycloak follows the RFC7517
        options.setJwkPath("/realms/" + realm + "/protocol/openid-connect/certs");

        if (config.realmPublicKey.isPresent()) {
            options.addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setPublicKey(config.realmPublicKey.get()));
        }

        //TODO: remove this
        byte[] bogus = new byte[512];
        new SecureRandom().nextBytes(bogus);

        options.addPubSecKey(
                new PubSecKeyOptions().setSymmetric(true).setPublicKey(Base64.getEncoder().encodeToString(bogus))
                        .setAlgorithm("HS512"));
        options.setFlow(OAuth2FlowType.AUTH_JWT);

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
        mechanism.setAuthServerURI(siteUri);
        mechanism.setConfig(config);

    }
}
