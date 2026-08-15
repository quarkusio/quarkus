package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/code-flow-issuer")
public class CodeFlowIssuerResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/code-flow-iss-present")
    @Authenticated
    public String codeFlowIssPresent() {
        return identity.getPrincipal().getName();
    }

    @GET
    @Path("/code-flow-iss-mismatch")
    @Authenticated
    public String codeFlowIssMismatch() {
        return identity.getPrincipal().getName();
    }

    @GET
    @Path("/code-flow-iss-missing")
    @Authenticated
    public String codeFlowIssMissing() {
        return identity.getPrincipal().getName();
    }
}
