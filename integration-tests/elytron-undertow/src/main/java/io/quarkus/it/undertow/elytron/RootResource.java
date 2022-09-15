package io.quarkus.it.undertow.elytron;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/rest")
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
}
