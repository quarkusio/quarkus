package io.quarkus.oidc.redis.token.state.manager.deployment;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/unprotected")
public class UnprotectedResource {

    @GET
    public String getName() {
        return "unprotected";
    }
}
