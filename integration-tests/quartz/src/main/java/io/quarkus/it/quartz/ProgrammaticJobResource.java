package io.quarkus.it.quartz;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Scheduler.JobDefinition;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.common.runtime.SyntheticScheduled;
import io.smallrye.mutiny.Uni;

@Path("/scheduler/programmatic")
public class ProgrammaticJobResource {

    static final AtomicInteger SYNC_COUNTER = new AtomicInteger();
    static final AtomicInteger ASYNC_COUNTER = new AtomicInteger();

    @Inject
    QuartzScheduler scheduler;

    @POST
    @Path("register")
    @Produces(MediaType.TEXT_PLAIN)
    public String register() throws SchedulerException {
        JobDefinition syncJob = scheduler.newJob("sync").setInterval("1s");
        try {
            syncJob.setTask(se -> {
            });
            throw new AssertionError();
        } catch (IllegalStateException expected) {
        }
        try {
            syncJob.setTask(se -> {
            }, true);
            throw new AssertionError();
        } catch (IllegalStateException expected) {
        }
        try {
            syncJob.setSkipPredicate(se -> true);
            throw new AssertionError();
        } catch (IllegalStateException expected) {
        }
        syncJob.setTask(SyncJob.class).schedule();
        // Verify the serialized metadata
        // JobKey is always built using the identity and "io.quarkus.scheduler.Scheduler" as the group name
        JobDetail syncJobDetail = scheduler.getScheduler().getJobDetail(new JobKey("sync", Scheduler.class.getName()));
        if (syncJobDetail == null) {
            return "Syn job detail not found";
        }
        SyntheticScheduled syncMetadata = SyntheticScheduled
                .fromJson(syncJobDetail.getJobDataMap().get("scheduled_metadata").toString());
        if (!syncMetadata.every().equals("1s")) {
            return "Sync interval not set";
        }
        if (!SyncJob.class.getName()
                .equals(syncJobDetail.getJobDataMap().getOrDefault("execution_metadata_task_class", "").toString())) {
            return "execution_metadata_task_class not set";
        }

        JobDefinition asyncJob = scheduler.newJob("async").setInterval("1s");
        try {
            asyncJob.setAsyncTask(se -> null);
            throw new AssertionError();
        } catch (IllegalStateException expected) {
        }
        asyncJob.setAsyncTask(AsyncJob.class).schedule();

        // Verify the serialized metadata
        // JobKey is always built using the identity and "io.quarkus.scheduler.Scheduler" as the group name
        JobDetail asynJobDetail = scheduler.getScheduler().getJobDetail(new JobKey("async", Scheduler.class.getName()));
        if (asynJobDetail == null) {
            return "Job detail not found";
        }
        SyntheticScheduled asyncMetadata = SyntheticScheduled
                .fromJson(asynJobDetail.getJobDataMap().get("scheduled_metadata").toString());
        if (!asyncMetadata.every().equals("1s")) {
            return "Interval not set";
        }
        if (!AsyncJob.class.getName()
                .equals(asynJobDetail.getJobDataMap().getOrDefault("execution_metadata_async_task_class", "").toString())) {
            return "execution_metadata_async_task_class not set";
        }
        return "OK";
    }

    @GET
    @Path("sync")
    @Produces(MediaType.TEXT_PLAIN)
    public Integer getSyncCount() throws SchedulerException {
        // Assert that triggers are available
        Trigger trigger = scheduler.getScheduledJob("sync");
        if (trigger == null) {
            throw new IllegalStateException();
        }
        org.quartz.Trigger quartzTrigger = scheduler.getScheduler()
                .getTrigger(new TriggerKey("sync", Scheduler.class.getName()));
        if (quartzTrigger == null) {
            throw new IllegalStateException();
        }
        // Return the number of executions
        return SYNC_COUNTER.get();
    }

    @GET
    @Path("async")
    @Produces(MediaType.TEXT_PLAIN)
    public Integer getAsyncCount() throws SchedulerException {
        // Assert that triggers are available
        Trigger trigger = scheduler.getScheduledJob("async");
        if (trigger == null) {
            throw new IllegalStateException();
        }
        org.quartz.Trigger quartzTrigger = scheduler.getScheduler()
                .getTrigger(new TriggerKey("async", Scheduler.class.getName()));
        if (quartzTrigger == null) {
            throw new IllegalStateException();
        }
        // Return the number of executions
        return ASYNC_COUNTER.get();
    }

    @RegisterForReflection
    public static class SyncJob implements Consumer<ScheduledExecution> {

        @Override
        public void accept(ScheduledExecution t) {
            SYNC_COUNTER.incrementAndGet();
        }

    }

    @RegisterForReflection
    public static class AsyncJob implements Function<ScheduledExecution, Uni<Void>> {

        @Override
        public Uni<Void> apply(ScheduledExecution t) {
            ASYNC_COUNTER.incrementAndGet();
            return Uni.createFrom().voidItem();
        }

    }

}
