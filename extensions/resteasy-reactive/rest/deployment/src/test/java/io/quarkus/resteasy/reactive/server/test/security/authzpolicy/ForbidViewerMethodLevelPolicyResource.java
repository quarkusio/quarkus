package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.security.AuthorizationPolicy;

@Path("forbid-viewer-method-level-policy")
public class ForbidViewerMethodLevelPolicyResource {

    @Inject
    SecurityIdentity securityIdentity;

    @AuthorizationPolicy(name = "forbid-all-but-viewer")
    @GET
    public String principal(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @Path("unsecured")
    @GET
    public String unsecured() {
        return securityIdentity.getPrincipal().getName();
    }

}
