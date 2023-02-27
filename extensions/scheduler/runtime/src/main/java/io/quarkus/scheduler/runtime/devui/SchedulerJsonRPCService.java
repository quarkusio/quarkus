package io.quarkus.scheduler.runtime.devui;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.common.runtime.ScheduledInvoker;
import io.quarkus.scheduler.common.runtime.ScheduledMethodMetadata;
import io.quarkus.scheduler.common.runtime.SchedulerContext;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SchedulerJsonRPCService {

    private static final Logger LOG = Logger.getLogger(SchedulerJsonRPCService.class);

    private static final Duration UPDATE_INTERVAL = Duration.ofSeconds(10);

    private final BroadcastProcessor<JsonObject> runningStatus = BroadcastProcessor.create();

    @Inject
    Instance<SchedulerContext> context;

    @Inject
    Instance<Scheduler> scheduler;

    @Inject
    Instance<Vertx> vertx;

    public Multi<JsonObject> streamRunningStatus() {
        SchedulerContext sc = context.get();
        Set<String> identities = new HashSet<>();
        for (ScheduledMethodMetadata metadata : sc.getScheduledMethods()) {
            for (Scheduled scheduled : metadata.getSchedules()) {
                if (!scheduled.identity().isBlank()) {
                    identities.add(scheduled.identity());
                }
            }
        }
        Multi.createFrom().ticks().every(UPDATE_INTERVAL).subscribe().with(tick -> {
            Scheduler s = scheduler.get();
            JsonObject result = new JsonObject();
            result.put("q_scheduler", s.isRunning());
            for (String identity : identities) {
                result.put(identity, !s.isPaused(identity));
            }
            runningStatus.onNext(result);
        });
        return runningStatus;
    }

    public JsonArray getScheduledMethods() {
        JsonArray methodsJson = new JsonArray();
        SchedulerContext c = context.get();
        for (ScheduledMethodMetadata metadata : c.getScheduledMethods()) {
            JsonObject methodJson = new JsonObject();
            methodJson.put("declaringClassName", metadata.getDeclaringClassName());
            methodJson.put("methodName", metadata.getMethodName());
            methodJson.put("methodDescription", metadata.getMethodDescription());
            JsonArray schedulesJson = new JsonArray();
            for (Scheduled schedule : metadata.getSchedules()) {
                JsonObject scheduleJson = new JsonObject();
                if (!schedule.identity().isBlank()) {
                    putConfigLookup("identity", schedule.identity(), scheduleJson);
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
        return methodsJson;
    }

    public JsonObject pauseScheduler() {
        Scheduler s = scheduler.get();
        if (!s.isRunning()) {
            return new JsonObject().put("success", false).put("message",
                    "Scheduler is already paused");
        }
        s.pause();
        LOG.info("Scheduler paused via Dev UI");
        return new JsonObject().put("success", true).put("message",
                "Scheduler was paused");
    }

    public JsonObject resumeScheduler() {
        Scheduler s = scheduler.get();
        if (s.isRunning()) {
            return new JsonObject().put("success", false).put("message",
                    "Scheduler is already running");
        }
        s.resume();
        LOG.info("Scheduler resumed via Dev UI");
        return new JsonObject().put("success", true).put("message",
                "Scheduler was resumed");
    }

    public JsonObject pauseJob(String identity) {
        Scheduler s = scheduler.get();
        if (s.isPaused(identity)) {
            return new JsonObject().put("success", false).put("message",
                    "Job with identity " + identity + " is already paused");
        }
        s.pause(identity);
        LOG.infof("Paused job with identity '%s' via Dev UI", identity);
        return new JsonObject().put("success", true).put("message",
                "Job with identity " + identity + " was paused");
    }

    public JsonObject resumeJob(String identity) {
        Scheduler s = scheduler.get();
        if (!s.isPaused(identity)) {
            return new JsonObject().put("success", false).put("message",
                    "Job with identity " + identity + " is not paused");
        }
        s.resume(identity);
        LOG.infof("Resumed job with identity '%s' via Dev UI", identity);
        return new JsonObject().put("success", true).put("message",
                "Job with identity " + identity + " was resumed");
    }

    public JsonObject executeJob(String methodDescription) {
        SchedulerContext c = context.get();
        for (ScheduledMethodMetadata metadata : c.getScheduledMethods()) {
            if (metadata.getMethodDescription().equals(methodDescription)) {
                Context vdc = VertxContext.getOrCreateDuplicatedContext(vertx.get());
                VertxContextSafetyToggle.setContextSafe(vdc, true);
                try {
                    ScheduledInvoker invoker = c
                            .createInvoker(metadata.getInvokerClassName());
                    if (invoker.isBlocking()) {
                        vdc.executeBlocking(p -> {
                            try {
                                invoker.invoke(new DevUIScheduledExecution());
                            } catch (Exception ignored) {
                            } finally {
                                p.complete();
                            }
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
                return new JsonObject().put("success", true).put("message",
                        "Invoked scheduled method " + methodDescription + " via Dev UI");
            }
        }
        return new JsonObject().put("success", false).put("message", "Scheduled method not found " + methodDescription);
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
