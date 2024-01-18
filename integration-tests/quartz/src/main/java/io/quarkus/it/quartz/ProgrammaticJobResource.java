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

import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Scheduler.JobDefinition;
import io.quarkus.scheduler.Trigger;
import io.smallrye.mutiny.Uni;

@Path("/scheduler/programmatic")
public class ProgrammaticJobResource {

    static final AtomicInteger SYNC_COUNTER = new AtomicInteger();
    static final AtomicInteger ASYNC_COUNTER = new AtomicInteger();

    @Inject
    QuartzScheduler scheduler;

    @POST
    @Path("register")
    public void register() {
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

        JobDefinition asyncJob = scheduler.newJob("async").setInterval("1s");
        try {
            asyncJob.setAsyncTask(se -> null);
            throw new AssertionError();
        } catch (IllegalStateException expected) {
        }
        asyncJob.setAsyncTask(AsyncJob.class).schedule();
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
