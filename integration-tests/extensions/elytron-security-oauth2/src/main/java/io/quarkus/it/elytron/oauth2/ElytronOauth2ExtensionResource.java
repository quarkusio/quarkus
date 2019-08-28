package io.quarkus.it.elytron.oauth2;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ElytronOauth2ExtensionResource {

    @GET
    @Path("/anonymous")
    public String anonymous() {
        return "anonymous";
    }

    @GET
    @Path("/authenticated")
    @RolesAllowed("READER")
    public String authenticated() {
        return "authenticated";
    }

    @GET
    @Path("/forbidden")
    @RolesAllowed("WRITER")
    public String forbidden() {
        return "forbidden";
    }

}
