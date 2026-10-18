package io.quarkus.oidc;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Represents an action that must be executed only once after a successful authorization code flow completion.
 */
public interface AuthenticationCompletionAction {

    record AuthenticationCompletionContext(RoutingContext routingContext, AuthorizationCodeTokens tokens,
            SecurityIdentity identity, OidcRequestContext<SecurityIdentity> requestContext) {
    }

    /**
     * Perform an authentication completion action
     *
     * @param authCompletionContext the authentication completion context
     */
    Uni<Void> action(AuthenticationCompletionContext authCompletionContext);
}