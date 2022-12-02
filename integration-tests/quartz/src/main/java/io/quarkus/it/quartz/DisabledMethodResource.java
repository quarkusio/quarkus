package io.quarkus.it.quartz;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
