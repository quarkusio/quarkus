package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class SecurityEventListener {

    public void event(@Observes AuthenticationFailureEvent event) {
        RoutingContext vertxContext = (RoutingContext) event.getEventProperties()
                .get(RoutingContext.class.getName());
        AuthenticationFailedException ex = (AuthenticationFailedException) event.getAuthenticationFailure();
        if ("expired".equals(ex.getAttribute(OidcConstants.ACCESS_TOKEN_VALUE))) {
            vertxContext.response().setStatusCode(401);
            vertxContext.response().end("Token: expired");
        }
    }

}
