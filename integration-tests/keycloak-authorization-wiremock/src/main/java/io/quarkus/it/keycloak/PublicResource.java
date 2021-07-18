package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
