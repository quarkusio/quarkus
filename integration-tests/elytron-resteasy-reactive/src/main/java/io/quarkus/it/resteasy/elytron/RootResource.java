package io.quarkus.it.resteasy.elytron;

import static io.vertx.core.Context.isOnEventLoopThread;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import io.smallrye.mutiny.Uni;

@Path("/")
public class RootResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean approval(@Context SecurityContext sec) {
        if (sec.getUserPrincipal().getName() == null) {
            throw new RuntimeException("Failed to get user principal");
        }
        return isOnEventLoopThread();
    }

    @GET
    @Path("/async/uni")
    @RolesAllowed("employees")
    public Uni<Boolean> uniAsync(@Context SecurityContext sec) {
        return Uni.createFrom().item(isOnEventLoopThread());
    }
}
