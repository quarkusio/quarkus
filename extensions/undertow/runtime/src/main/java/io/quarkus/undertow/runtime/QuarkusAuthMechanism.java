package io.quarkus.undertow.runtime;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.undertow.httpcore.StatusCodes;
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
        try {
            SecurityIdentity identity = QuarkusHttpUser.getSecurityIdentityBlocking(context, null);
            if (identity != null && !identity.isAnonymous()) {
                //associate the identity
                securityContext.authenticationComplete(new QuarkusUndertowAccount(identity), "Quarkus",
                        false);
                return AuthenticationMechanismOutcome.AUTHENTICATED;
            }
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        } catch (AuthenticationFailedException e) {
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        VertxHttpExchange delegate = (VertxHttpExchange) exchange.getDelegate();
        RoutingContext context = (RoutingContext) delegate.getContext();
        HttpAuthenticator authenticator = context.get(HttpAuthenticator.class.getName());
        if (authenticator == null) {
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.endExchange();
            return new ChallengeResult(true, exchange.getStatusCode());
        }
        authenticator.sendChallenge(context).await().indefinitely();
        exchange.endExchange();
        return new ChallengeResult(true, exchange.getStatusCode());
    }
}
