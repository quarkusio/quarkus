package io.quarkus.security.webauthn.test;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkus.security.webauthn.WebAuthn;
import io.quarkus.vertx.http.runtime.security.Basic;
import io.smallrye.mutiny.Uni;

@Path("/multiple-auth-mech")
public class MultipleAuthMechResource {

    @Basic
    @Path("basic")
    @POST
    public Uni<String> enforceBasicAuthMechanism() {
        return Uni.createFrom().item("basic");
    }

    @WebAuthn
    @Path("webauth")
    @POST
    public Uni<String> enforceWebAuthMechanism() {
        return Uni.createFrom().item("webauth");
    }

}
