package io.quarkus.jfr.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.mutiny.Uni;

@Path("/app")
@ApplicationScoped
public class AppResource {

    @GET
    @Path("/reactive")
    public Uni<String> reactive() {
        return Uni.createFrom().item("");
    }

    @GET
    @Path("blocking")
    public String blocking() {
        return "";
    }
}
