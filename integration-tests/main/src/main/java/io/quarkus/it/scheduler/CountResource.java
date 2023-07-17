package io.quarkus.it.scheduler;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/scheduler/count")
public class CountResource {

    @Inject
    Counter counter;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCount() {
        return "count:" + counter.get();
    }
}
