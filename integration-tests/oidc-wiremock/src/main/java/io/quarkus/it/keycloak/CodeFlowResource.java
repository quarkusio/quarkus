package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/code-flow")
public class CodeFlowResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Authenticated
    public String access() {
        return identity.getPrincipal().getName();
    }

    @GET
    @Path("/post-logout")
    public String postLogout(@QueryParam("clientId") String clientId) {
        return "Welcome, clientId: " + clientId;
    }
}
