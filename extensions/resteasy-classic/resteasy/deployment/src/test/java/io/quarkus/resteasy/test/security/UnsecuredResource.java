package io.quarkus.resteasy.test.security;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Path("/unsecured")
public class UnsecuredResource extends UnsecuredParentResource implements UnsecuredResourceInterface {
    @Path("/defaultSecurity")
    @GET
    public String defaultSecurity() {
        return "defaultSecurity";
    }

    @Override
    public String defaultSecurityInterface() {
        return UnsecuredResourceInterface.super.defaultSecurityInterface();
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

    @Path("/authenticated")
    @GET
    @Authenticated
    public String authenticated() {
        return "authenticated";
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
