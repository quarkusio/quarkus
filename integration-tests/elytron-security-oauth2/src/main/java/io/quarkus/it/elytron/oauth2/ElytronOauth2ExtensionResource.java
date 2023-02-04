package io.quarkus.it.elytron.oauth2;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api")
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
