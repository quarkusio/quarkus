package io.quarkus.rest.server.runtime;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

public class ResteasyReactiveSecurityContext implements SecurityContext {

    private final RoutingContext routingContext;

    public ResteasyReactiveSecurityContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    @Override
    public Principal getUserPrincipal() {
        QuarkusHttpUser user = (QuarkusHttpUser) routingContext.user();
        if (user == null || user.getSecurityIdentity().isAnonymous()) {
            return null;
        }
        return user.getSecurityIdentity().getPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        SecurityIdentity user = CurrentIdentityAssociation.current();
        if (role.equals("**")) {
            return !user.isAnonymous();
        }
        return user.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return routingContext.request().isSSL();
    }

    @Override
    public String getAuthenticationScheme() {
        String authorizationValue = routingContext.request().getHeader("Authorization");
        if (authorizationValue == null) {
            return null;
        } else {
            return authorizationValue.split(" ")[0].trim();
        }
    }
}
