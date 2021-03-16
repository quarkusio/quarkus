package io.quarkus.it.resteasy.elytron;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import io.quarkus.security.Authenticated;

@Path("/")
public class RootResource {

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public String posts(String data, @Context SecurityContext sec) {
        if (data == null) {
            throw new RuntimeException("No post data");
        }
        if (sec.getUserPrincipal().getName() == null) {
            throw new RuntimeException("Failed to get user principal");
        }
        return "post success";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String approval(@Context SecurityContext sec) {
        if (sec.getUserPrincipal().getName() == null) {
            throw new RuntimeException("Failed to get user principal");
        }
        return "get success";
    }

    @GET
    @Path("/secure")
    @Authenticated
    public String getSecure() {
        return "secure";
    }

    @GET
    @Path("/user")
    @RolesAllowed("user")
    public String user(@Context SecurityContext sec) {
        return sec.getUserPrincipal().getName();
    }

}
