package io.quarkus.oidc.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/lose-token-state")
public class MissingTokenStateResource {

    @Inject
    MissingTokenStateManager tokenStateManager;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String loseTokenState() {
        tokenStateManager.loseTokenState();
        return "token state lost";
    }
}
