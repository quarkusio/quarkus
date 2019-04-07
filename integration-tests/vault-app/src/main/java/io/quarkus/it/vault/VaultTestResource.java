package io.quarkus.it.vault;

import static javax.ws.rs.core.MediaType.*;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/vault")
public class VaultTestResource {

    @Inject
    VaultTestService vaultTestService;

    @GET
    @Produces(TEXT_PLAIN)
    public String test() {
        return vaultTestService.test();
    }

}
