package io.quarkus.it.quartz;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/scheduler/count")
public class CountResource {

    @Inject
    Counter counter;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer getCount() {
        return counter.get();
    }
}
