package io.quarkus.test.integration.mtls;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.smallrye.mutiny.Uni;

@Path("test")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class TestResource {

    @GET
    @Path("names")
    public Uni<List<String>> getNames() {
        return Uni.createFrom().item(List.of("a", "b", "c", "d"));
    }

}
