package io.quarkus.resteasy.reactive.server.test.security;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.smallrye.common.annotation.NonBlocking;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Path("/permitAll")
@PermitAll
public class PermitAllResource {
    @Path("/defaultSecurity")
    @GET
    public String defaultSecurity() {
        return "defaultSecurity";
    }

    @NonBlocking
    @Path("/defaultSecurityNonBlocking")
    @GET
    public String defaultSecurityNonBlocking() {
        return "defaultSecurityNonBlocking";
    }

    @Path("/sub")
    public UnsecuredSubResource sub() {
        return new UnsecuredSubResource();
    }

}
