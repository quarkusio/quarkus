package io.quarkus.it.spring.scheduled;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/spring/scheduler/count")
public class CountResource {

    @Inject
    Counter counter;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCount() {
        return "count:" + counter.get();
    }
}
