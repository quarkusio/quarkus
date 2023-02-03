package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/code-flow-form-post")
public class CodeFlowFormPostResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Authenticated
    public String access() {
        return identity.getPrincipal().getName();
    }
}
