package io.quarkus.it.quartz;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.quartz.JobKey;
import org.quartz.SchedulerException;

import io.quarkus.scheduler.Scheduler;

@Path("/scheduler/jobs")
public class JobResource {

    @Inject
    org.quartz.Scheduler quartzScheduler;

    @GET
    @Path("{name}/exists")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean exists(@PathParam("name") String name) throws SchedulerException {
        // JobKey is always built using the identity and "io.quarkus.scheduler.Scheduler" as the group name
        return quartzScheduler.checkExists(new JobKey(name, Scheduler.class.getName()));
    }

}
