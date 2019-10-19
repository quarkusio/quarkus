package io.quarkus.resteasy.runtime.standalone;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;

public class QuarkusResteasySecurityContext implements SecurityContext {

    private final HttpServerRequest request;

    public QuarkusResteasySecurityContext(HttpServerRequest request) {
        this.request = request;
    }

    @Override
    public Principal getUserPrincipal() {
        SecurityIdentity user = CurrentIdentityAssociation.current();
        return user.isAnonymous() ? null : user.getPrincipal();
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
