package io.quarkus.undertow.runtime;

import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.vertx.VertxHttpExchange;
import io.vertx.ext.web.RoutingContext;

public class QuarkusAuthMechanism implements AuthenticationMechanism {

    public static final QuarkusAuthMechanism INSTANCE = new QuarkusAuthMechanism();

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        VertxHttpExchange delegate = (VertxHttpExchange) exchange.getDelegate();
        RoutingContext context = (RoutingContext) delegate.getContext();
        if (context.user() != null) {
            //associate the identity
            QuarkusHttpUser user = (QuarkusHttpUser) context.user();
            securityContext.authenticationComplete(new QuarkusUndertowAccount(user.getSecurityIdentity()), "Quarkus", false);
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        }
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        VertxHttpExchange delegate = (VertxHttpExchange) exchange.getDelegate();
        RoutingContext context = (RoutingContext) delegate.getContext();
        HttpAuthenticator authenticator = context.get(HttpAuthenticator.class.getName());
        authenticator.sendChallenge(context, new Runnable() {
            @Override
            public void run() {
                exchange.endExchange();
            }
        }).toCompletableFuture().join();
        return new ChallengeResult(true, exchange.getStatusCode());
    }
}
