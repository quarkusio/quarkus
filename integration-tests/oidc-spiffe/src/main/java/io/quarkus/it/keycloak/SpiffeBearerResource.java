package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/spiffe/bearer")
public class SpiffeBearerResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Authenticated
    public String get() {
        return identity.getPrincipal().getName();
    }
}
