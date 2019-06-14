package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/api/confidential")
public class ConfidentialResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String confidential() {
        return "confidential";
    }
}
