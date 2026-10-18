package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.vertx.http.security.AuthorizationPolicy;

@AuthorizationPolicy(name = "forbid-all-but-viewer")
@Path("forbid-viewer-class-level-policy")
public class ForbidViewerClassLevelPolicyResource {

    @GET
    public String principal(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

}
