package io.quarkus.security.jpa.reactive;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

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
