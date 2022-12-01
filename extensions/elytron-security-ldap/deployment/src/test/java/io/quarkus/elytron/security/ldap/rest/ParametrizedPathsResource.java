package io.quarkus.elytron.security.ldap.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/parameterized-paths")
public class ParametrizedPathsResource {
    @GET
    @Path("/my/{path}/admin")
    @RolesAllowed("adminRole")
    public String admin(@PathParam("path") String path) {
        return "Admin accessed " + path;
    }

    @GET
    @Path("/my/{path}/view")
    @RolesAllowed("standardRole")
    public String view(@PathParam("path") String path) {
        return "View accessed " + path;
    }
}
