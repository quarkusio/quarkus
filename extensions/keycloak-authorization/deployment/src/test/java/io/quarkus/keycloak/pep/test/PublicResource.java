package io.quarkus.keycloak.pep.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api/public")
public class PublicResource {

    @Path("/serve")
    @GET
    public String serve() {
        return "serve";
    }

}
