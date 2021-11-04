package io.quarkus.resteasy.test.security;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
