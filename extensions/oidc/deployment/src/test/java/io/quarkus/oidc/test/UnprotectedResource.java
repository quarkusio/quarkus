package io.quarkus.oidc.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/unprotected")
public class UnprotectedResource {

    @GET
    public String getName() {
        return "unprotected";
    }
}
