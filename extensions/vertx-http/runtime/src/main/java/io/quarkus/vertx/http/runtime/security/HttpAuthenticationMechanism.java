package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.CompletionStage;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

/**
 * An interface that performs HTTP based authentication
 */
public interface HttpAuthenticationMechanism {

    CompletionStage<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager);

    CompletionStage<Boolean> sendChallenge(RoutingContext context);

}
