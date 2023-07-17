package io.quarkus.it.quartz;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;

@Path("/scheduler/instance-id")
public class FixedInstanceIdResource {

    @Inject
    Scheduler quartzScheduler;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getInstanceId() throws SchedulerException {
        return quartzScheduler.getSchedulerInstanceId();
    }

}
