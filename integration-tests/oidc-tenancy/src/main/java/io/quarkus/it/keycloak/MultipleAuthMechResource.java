package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;

@Path("multiple-auth-mech")
public class MultipleAuthMechResource {

    @GET
    @Path("bearer/policy")
    public String bearerPolicy(@Context SecurityContext sec) {
        return sec.getUserPrincipal().getName();
    }

    @GET
    @Path("basic/policy")
    public String basicPolicy(@Context SecurityContext sec) {
        return sec.getUserPrincipal().getName();
    }

    @BearerTokenAuthentication
    @GET
    @Path("bearer/annotation")
    public String bearerAnnotation(@Context SecurityContext sec) {
        return sec.getUserPrincipal().getName();
    }

    @BasicAuthentication
    @GET
    @Path("basic/annotation")
    public String basicAnnotation(@Context SecurityContext sec) {
        return sec.getUserPrincipal().getName();
    }
}
