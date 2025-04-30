package io.quarkus.it.opentelemetry.scheduler;

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
    JobDefinitionCounter jobDefinitionCounter;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer getCount() {
        return counter.get();
    }

    @GET
    @Path("job-definition")
    @Produces(MediaType.TEXT_PLAIN)
    public Integer getJobDefinitionCount() {
        return jobDefinitionCounter.get();
    }
}
