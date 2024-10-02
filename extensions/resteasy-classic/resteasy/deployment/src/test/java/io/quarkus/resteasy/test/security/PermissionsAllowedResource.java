package io.quarkus.resteasy.test.security;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/permissions")
public class PermissionsAllowedResource {

    @Path("/string-meta-annotation")
    @StringPermissionsAllowedMetaAnnotation
    @GET
    public String stringMetaAnnotation() {
        return "admin";
    }

    @CreateOrUpdate
    @Path("/custom-perm-with-args-meta-annotation/{goodbye}")
    @POST
    public String farewellMetaAnnotation(@PathParam("goodbye") String goodbye, @HeaderParam("toWhom") String toWhom,
            @CookieParam("day") int day, String place) {
        return String.join(" ", new String[] { goodbye, toWhom, Integer.toString(day), place });
    }
}
