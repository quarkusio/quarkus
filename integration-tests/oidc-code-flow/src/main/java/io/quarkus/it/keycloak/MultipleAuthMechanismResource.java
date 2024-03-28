package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.CodeFlow;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.vertx.http.runtime.security.Basic;

@Path("multiple-auth-mech")
public class MultipleAuthMechanismResource {

    @Basic
    @PermissionsAllowed("permission1")
    @GET
    @Path("basic")
    public String basicAuthMech() {
        return "basicAuthMech";
    }

    @CodeFlow
    @GET
    @Path("code-flow")
    public String codeFlowAuthMech() {
        return "codeFlowAuthMech";
    }
}
