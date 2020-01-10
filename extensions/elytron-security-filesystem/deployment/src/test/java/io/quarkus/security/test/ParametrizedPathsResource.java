package io.quarkus.security.test;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/parameterized-paths")
public class ParametrizedPathsResource {
    @GET
    @Path("/my/{path}/admin")
    @RolesAllowed("admin")
    public String admin(@PathParam("path") String path) {
        return "Admin accessed " + path;
    }

    @GET
    @Path("/my/{path}/view")
    @RolesAllowed("user")
    public String view(@PathParam("path") String path) {
        return "View accessed " + path;
    }
}
