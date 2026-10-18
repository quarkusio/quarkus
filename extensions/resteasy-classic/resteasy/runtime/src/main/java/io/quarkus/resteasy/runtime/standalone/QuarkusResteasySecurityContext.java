package io.quarkus.resteasy.runtime.standalone;

import java.security.Principal;

import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class QuarkusResteasySecurityContext implements SecurityContext {

    private final HttpServerRequest request;
    private final RoutingContext routingContext;

    public QuarkusResteasySecurityContext(HttpServerRequest request, RoutingContext routingContext) {
        this.request = request;
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
        return request.isSSL();
    }

    @Override
    public String getAuthenticationScheme() {
        String authorizationValue = request.getHeader("Authorization");
        if (authorizationValue == null) {
            return null;
        } else {
            return authorizationValue.split(" ")[0].trim();
        }
    }
}
