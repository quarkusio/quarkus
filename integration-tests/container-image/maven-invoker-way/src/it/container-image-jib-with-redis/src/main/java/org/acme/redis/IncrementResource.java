package org.acme.redis;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@Path("/increments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IncrementResource {

    @Inject
    IncrementService service;

    @GET
    public List<String> keys() {
        return service.keys();
    }

    @POST
    public Increment create(Increment increment) {
        service.set(increment.key, increment.value);
        return increment;
    }

    @GET
    @Path("/{key}")
    public Increment get(@PathParam("key") String key) {
        return new Increment(key, Integer.valueOf(service.get(key)));
    }

    @PUT
    @Path("/{key}")
    public void increment(@PathParam("key") String key, Integer value) {
        service.increment(key, value);
    }

    @DELETE
    @Path("/{key}")
    public void delete(@PathParam("key") String key) {
        service.del(key);
    }
}
