package io.quarkus.oidc.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/unprotected")
public class UnprotectedResource {

    @GET
    public String getName() {
        return "unprotected";
    }
}
