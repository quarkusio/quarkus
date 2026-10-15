package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/spiffe/code-flow")
@Authenticated
public class SpiffeCodeFlowResource {

    @Inject
    SecurityIdentity identity;

    @GET
    public String get() {
        return identity.getPrincipal().getName();
    }
}
