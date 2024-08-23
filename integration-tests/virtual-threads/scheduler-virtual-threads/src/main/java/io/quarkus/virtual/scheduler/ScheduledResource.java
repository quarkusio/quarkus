package io.quarkus.virtual.scheduler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/")
public class ScheduledResource {

    Set<String> executions = new CopyOnWriteArraySet<>();
    Set<String> programmaticExecutions = new CopyOnWriteArraySet<>();

    public void init(@Observes StartupEvent ev, Scheduler scheduler) {
        scheduler.newJob("my-programmatic-job")
                .setInterval("1s")
                .setTask(ex -> {
                    VirtualThreadsAssertions.assertEverything();
                    // Quarkus specific - each VT has a unique name
                    programmaticExecutions.add(Thread.currentThread().getName());
                }, true)
                .schedule();
    }

    @Scheduled(every = "1s")
    @RunOnVirtualThread
    void run() {
        VirtualThreadsAssertions.assertEverything();
        // Quarkus specific - each VT has a unique name
        executions.add(Thread.currentThread().getName());
    }

    @GET
    public Set<String> getExecutions() {
        return executions;
    }

    @GET
    @Path("/programmatic")
    public Set<String> getProgrammaticExecutions() {
        return programmaticExecutions;
    }
}
