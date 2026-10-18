package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AuthorizationCodeFlow;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;

@Path("multiple-auth-mech")
public class MultipleAuthMechanismResource {

    @BasicAuthentication
    @PermissionsAllowed("permission1")
    @GET
    @Path("basic")
    public String basicAuthMech() {
        return "basicAuthMech";
    }

    @AuthorizationCodeFlow
    @GET
    @Path("code-flow")
    public String codeFlowAuthMech() {
        return "codeFlowAuthMech";
    }
}
