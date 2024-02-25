package io.quarkus.resteasy.reactive.server.test.security;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.smallrye.common.annotation.NonBlocking;

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

    @NonBlocking
    @Path("/defaultSecurityNonBlocking")
    @GET
    public String defaultSecurityNonBlocking() {
        return "defaultSecurityNonBlocking";
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

    @Override
    public String interfaceOverriddenDeclaredOnInterface() {
        return "implementor-response";
    }

    @GET
    @Path("/interface-overridden-declared-on-implementor")
    @Override
    public String interfaceOverriddenDeclaredOnImplementor() {
        return "implementor-response";
    }
}
