package io.quarkus.it.quartz;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/scheduler/count")
public class CountResource {

    @Inject
    Counter counter;

    @Inject
    ManualScheduledCounter manualScheduledCounter;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer getCount() {
        return counter.get();
    }

    @GET
    @Path("fix-8555")
    @Produces(MediaType.TEXT_PLAIN)
    public Integer fix8555() {
        return manualScheduledCounter.get();
    }
}
