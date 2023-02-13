package io.quarkus.it.quartz;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/scheduler/disabled")
public class DisabledMethodResource {

    @Inject
    DisabledScheduledMethods disabledScheduledMethods;

    @GET
    @Path("cron")
    @Produces(MediaType.TEXT_PLAIN)
    public String getCronCount() {
        return disabledScheduledMethods.valueSetByCronScheduledMethod;
    }

    @GET
    @Path("every")
    @Produces(MediaType.TEXT_PLAIN)
    public String getEveryCount() {
        return disabledScheduledMethods.valueSetByEveryScheduledMethod;
    }
}
