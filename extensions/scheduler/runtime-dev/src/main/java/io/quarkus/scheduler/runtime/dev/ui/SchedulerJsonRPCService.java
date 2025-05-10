package io.quarkus.scheduler.runtime.dev.ui;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.ScheduledJobPaused;
import io.quarkus.scheduler.ScheduledJobResumed;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.SchedulerPaused;
import io.quarkus.scheduler.SchedulerResumed;
import io.quarkus.scheduler.SuccessfulExecution;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.common.runtime.ScheduledInvoker;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;
import io.quarkus.scheduler.common.runtime.SchedulerContext;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class SchedulerJsonRPCService {

    private static final Logger LOG = Logger.getLogger(SchedulerJsonRPCService.class);
    private static final String SCHEDULER_ID = "quarkus_scheduler";

    private final BroadcastProcessor<JsonObject> runningStatus;
    private final BroadcastProcessor<JsonObject> log;
    private final Instance<SchedulerContext> context;
    private final Instance<Scheduler> scheduler;
    private final Instance<Vertx> vertx;

    public SchedulerJsonRPCService(Instance<SchedulerContext> context, Instance<Scheduler> scheduler, Instance<Vertx> vertx) {
        runningStatus = BroadcastProcessor.create();
        log = BroadcastProcessor.create();
        this.context = context;
        this.scheduler = scheduler;
        this.vertx = vertx;
    }

    void onPause(@Observes SchedulerPaused e) {
        runningStatus.onNext(newRunningStatus(SCHEDULER_ID, false));
    }

    void onResume(@Observes SchedulerResumed e) {
        runningStatus.onNext(newRunningStatus(SCHEDULER_ID, true));
    }

    void onPause(@Observes ScheduledJobPaused e) {
        runningStatus.onNext(newRunningStatus(e.getTrigger().getId(), false));
    }

    void onResume(@Observes ScheduledJobResumed e) {
        runningStatus.onNext(newRunningStatus(e.getTrigger().getId(), true));
    }

    void onJobSuccess(@Observes SuccessfulExecution e) {
        log.onNext(newExecutionLog(e.getExecution().getTrigger(), true, null,
                isUserDefinedIdentity(e.getExecution().getTrigger().getId())));
    }

    void onJobFailure(@Observes FailedExecution e) {
        log.onNext(newExecutionLog(e.getExecution().getTrigger(), false, e.getException().getMessage(),
                isUserDefinedIdentity(e.getExecution().getTrigger().getId())));
    }

    public Multi<JsonObject> streamLog() {
        return log;
    }

    public Multi<JsonObject> streamRunningStatus() {
        return runningStatus;
    }

    @NonBlocking
    public JsonObject getData() {
        SchedulerContext c = context.get();
        Scheduler s = scheduler.get();

        JsonObject ret = new JsonObject();
        ret.put("schedulerRunning", s.isRunning());

        JsonArray methodsJson = new JsonArray();
        ret.put("methods", methodsJson);
        for (ScheduledMethod metadata : c.getScheduledMethods()) {
            JsonObject methodJson = new JsonObject();
            methodJson.put("declaringClassName", metadata.getDeclaringClassName());
            methodJson.put("methodName", metadata.getMethodName());
            methodJson.put("methodDescription", metadata.getMethodDescription());
            JsonArray schedulesJson = new JsonArray();
            for (Scheduled schedule : metadata.getSchedules()) {
                JsonObject scheduleJson = new JsonObject();
                if (!schedule.identity().isBlank()) {
                    putConfigLookup("identity", schedule.identity(), scheduleJson);
                    scheduleJson.put("running", !s.isPaused(schedule.identity()));
                }
                String cron = schedule.cron();
                if (!cron.isBlank()) {
                    putConfigLookup("cron", cron, scheduleJson);
                } else {
                    putConfigLookup("every", schedule.every(), scheduleJson);
                }
                if (schedule.delay() > 0) {
                    scheduleJson.put("delay", schedule.delay());
                    scheduleJson.put("delayUnit", schedule.delayUnit().toString().toLowerCase());
                } else if (!schedule.delayed().isBlank()) {
                    putConfigLookup("delayed", schedule.delayed(), scheduleJson);
                }
                schedulesJson.add(scheduleJson);
            }
            methodJson.put("schedules", schedulesJson);
            methodsJson.add(methodJson);
        }
        return ret;
    }

    @NonBlocking
    public JsonObject pauseScheduler() {
        Scheduler s = scheduler.get();
        if (!s.isRunning()) {
            return newFailure("Scheduler is already paused");
        }
        s.pause();
        LOG.info("Scheduler paused via Dev UI");
        return newSuccess("Scheduler was paused");
    }

    @NonBlocking
    public JsonObject resumeScheduler() {
        Scheduler s = scheduler.get();
        if (s.isRunning()) {
            return newFailure("Scheduler is already running");
        }
        s.resume();
        LOG.info("Scheduler resumed via Dev UI");
        return newSuccess("Scheduler was resumed");
    }

    @NonBlocking
    public JsonObject pauseJob(String identity) {
        Scheduler s = scheduler.get();
        if (s.isPaused(identity)) {
            return newFailure("Job with identity " + identity + " is already paused");
        }
        s.pause(identity);
        LOG.infof("Paused job with identity '%s' via Dev UI", identity);
        return newSuccess("Job with identity " + identity + " was paused");
    }

    @NonBlocking
    public JsonObject resumeJob(String identity) {
        Scheduler s = scheduler.get();
        if (!s.isPaused(identity)) {
            return newFailure("Job with identity " + identity + " is not paused");
        }
        s.resume(identity);
        LOG.infof("Resumed job with identity '%s' via Dev UI", identity);
        return newSuccess("Job with identity " + identity + " was resumed");
    }

    @NonBlocking
    public JsonObject executeJob(String methodDescription) {
        SchedulerContext c = context.get();
        for (ScheduledMethod metadata : c.getScheduledMethods()) {
            if (metadata.getMethodDescription().equals(methodDescription)) {
                Context vdc = VertxContext.getOrCreateDuplicatedContext(vertx.get());
                VertxContextSafetyToggle.setContextSafe(vdc, true);
                try {
                    ScheduledInvoker invoker = c
                            .createInvoker(metadata.getInvokerClassName());
                    if (invoker.isBlocking()) {
                        vdc.executeBlocking(() -> {
                            try {
                                invoker.invoke(new DevUIScheduledExecution());
                            } catch (Exception ignored) {
                            }
                            return null;
                        }, false);
                    } else {
                        vdc.runOnContext(x -> {
                            try {
                                invoker.invoke(new DevUIScheduledExecution());
                            } catch (Exception ignored) {
                            }
                        });
                    }
                    LOG.infof("Invoked scheduled method %s via Dev UI", methodDescription);
                } catch (Exception e) {
                    LOG.error(
                            "Unable to invoke a @Scheduled method: "
                                    + metadata.getMethodDescription(),
                            e);
                }
                return newSuccess("Invoked scheduled method " + methodDescription + " via Dev UI");
            }
        }
        return newFailure("Scheduled method not found " + methodDescription);
    }

    private JsonObject newSuccess(String message) {
        return new JsonObject()
                .put("success", true)
                .put("message", message);
    }

    private JsonObject newFailure(String message) {
        return new JsonObject()
                .put("success", false)
                .put("message", message);
    }

    private JsonObject newRunningStatus(String id, boolean running) {
        return new JsonObject()
                .put("id", id)
                .put("running", running);
    }

    private JsonObject newExecutionLog(Trigger trigger, boolean success, String message, boolean userDefinedIdentity) {
        JsonObject log = new JsonObject()
                .put("timestamp", LocalDateTime.now().toString())
                .put("success", success);
        String description = trigger.getMethodDescription();
        if (description != null) {
            log.put("triggerMethodDescription", description);
            if (userDefinedIdentity) {
                log.put("triggerIdentity", trigger.getId());
            }
        } else {
            // Always add identity if no method description is available
            log.put("triggerIdentity", trigger.getId());
        }
        if (message != null) {
            log.put("message", message);
        }
        return log;
    }

    private boolean isUserDefinedIdentity(String identity) {
        for (ScheduledMethod metadata : context.get().getScheduledMethods()) {
            for (Scheduled schedule : metadata.getSchedules()) {
                if (identity.equals(schedule.identity())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void putConfigLookup(String key, String value, JsonObject scheduleJson) {
        scheduleJson.put(key, value);
        String configLookup = SchedulerUtils.lookUpPropertyValue(value);
        if (!value.equals(configLookup)) {
            scheduleJson.put(key + "Config", configLookup);
        }
    }

    private static class DevUIScheduledExecution implements ScheduledExecution {

        private final Instant now;

        DevUIScheduledExecution() {
            super();
            this.now = Instant.now();
        }

        @Override
        public Trigger getTrigger() {
            return new Trigger() {

                @Override
                public String getId() {
                    return "dev-console";
                }

                @Override
                public Instant getNextFireTime() {
                    return null;
                }

                @Override
                public Instant getPreviousFireTime() {
                    return now;
                }

                @Override
                public boolean isOverdue() {
                    return false;
                }

            };
        }

        @Override
        public Instant getFireTime() {
            return now;
        }

        @Override
        public Instant getScheduledFireTime() {
            return now;
        }

    }

}
