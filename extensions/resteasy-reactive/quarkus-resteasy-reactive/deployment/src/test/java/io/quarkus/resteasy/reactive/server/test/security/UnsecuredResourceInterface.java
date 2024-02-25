package io.quarkus.resteasy.reactive.server.test.security;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

public interface UnsecuredResourceInterface {

    @Path("/defaultSecurityInterface")
    @GET
    default String defaultSecurityInterface() {
        return "defaultSecurityInterface";
    }

    @RolesAllowed({ "admin", "user" })
    @GET
    @Path("/interface-annotated")
    default String interfaceAnnotated() {
        return "interface-annotated";
    }

    @RolesAllowed({ "admin", "user" })
    @GET
    @Path("/interface-overridden-declared-on-interface")
    default String interfaceOverriddenDeclaredOnInterface() {
        // this interface is overridden without @GET and @Path
        return "interface-overridden-declared-on-interface";
    }

    @RolesAllowed({ "admin", "user" })
    @GET
    @Path("/interface-overridden-declared-on-implementor")
    default String interfaceOverriddenDeclaredOnImplementor() {
        // this interface is overridden with @GET and @Path
        return "interface-overridden-declared-on-implementor";
    }

}