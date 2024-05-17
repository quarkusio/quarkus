package io.quarkus.resteasy.test.security;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/permitAll")
@PermitAll
public class PermitAllResource {
    @Path("/defaultSecurity")
    @GET
    public String defaultSecurity() {
        return "defaultSecurity";
    }

    @Path("/sub")
    public UnsecuredSubResource sub() {
        return new UnsecuredSubResource();
    }

}
