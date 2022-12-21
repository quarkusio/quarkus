package io.quarkus.it.quartz;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
