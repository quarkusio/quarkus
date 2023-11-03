package io.quarkus.resteasy.test.security;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Path("/unsecured")
public class UnsecuredResource {
    @Path("/defaultSecurity")
    @GET
    public String defaultSecurity() {
        return "defaultSecurity";
    }

    @Path("/permitAllPathParam/{index}")
    @GET
    @PermitAll
    public String permitAllPathParam(@PathParam("index") int i) {
        return "permitAll";
    }

    @Path("/permitAll")
    @GET
    @PermitAll
    public String permitAll() {
        return "permitAll";
    }

    @Path("/denyAll")
    @GET
    @DenyAll
    public String denyAll() {
        return "denyAll";
    }

    @Path("/sub")
    public UnsecuredSubResource sub() {
        return new UnsecuredSubResource();
    }

    @PermitAll
    @Path("/permitAllSub")
    public UnsecuredSubResource permitAllSub() {
        return new UnsecuredSubResource();
    }
}
