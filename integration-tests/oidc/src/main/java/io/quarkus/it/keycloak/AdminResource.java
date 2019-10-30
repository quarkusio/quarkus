package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claim;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/api/admin")
public class AdminResource {

    @Claim("preferred_username")
    String claim;

    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String admin() {
        return "granted:" + claim;
    }
}
