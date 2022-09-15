package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api")
public class PublicResource {

    @GET
    @Path("public")
    public void serve() {
        // no-op
    }

    @GET
    @Path("public-enforcing")
    public void serveEnforcing() {
        // no-op
    }

    @GET
    @Path("public-token")
    public void serveToken() {
        // no-op
    }
}
