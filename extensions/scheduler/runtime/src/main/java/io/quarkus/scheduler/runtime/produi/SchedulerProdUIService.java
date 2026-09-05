package io.quarkus.scheduler.runtime.produi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledJobPaused;
import io.quarkus.scheduler.ScheduledJobResumed;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.SchedulerPaused;
import io.quarkus.scheduler.SchedulerResumed;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;
import io.quarkus.scheduler.common.runtime.SchedulerContext;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

/**
 * Read-only view of the scheduler shared by Dev UI and Prod UI. It exposes only
 * the scheduled methods, their triggers and the running/paused status - no
 * pause, resume or manual execution - so it is safe to serve in production. The
 * mutating actions live in the Dev-only {@code SchedulerJsonRPCService}. Returns
 * plain maps/lists so no JSON library is needed on the runtime classpath.
 */
@ApplicationScoped
public class SchedulerProdUIService {

    private static final String SCHEDULER_ID = "quarkus_scheduler";

    private final BroadcastProcessor<Map<String, Object>> runningStatus;
    private final Instance<SchedulerContext> context;
    private final Instance<Scheduler> scheduler;

    public SchedulerProdUIService(Instance<SchedulerContext> context, Instance<Scheduler> scheduler) {
        this.runningStatus = BroadcastProcessor.create();
        this.context = context;
        this.scheduler = scheduler;
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

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Stream the running/paused status of the scheduler and its jobs")
    public Multi<Map<String, Object>> streamRunningStatus() {
        return runningStatus;
    }

    @NonBlocking
    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get information on the scheduler and its scheduled methods")
    public Map<String, Object> getData() {
        SchedulerContext c = context.get();
        Scheduler s = scheduler.get();

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("schedulerRunning", s.isRunning());

        List<Map<String, Object>> methodsJson = new ArrayList<>();
        ret.put("methods", methodsJson);
        for (ScheduledMethod metadata : c.getScheduledMethods()) {
            Map<String, Object> methodJson = new LinkedHashMap<>();
            methodJson.put("declaringClassName", metadata.getDeclaringClassName());
            methodJson.put("methodName", metadata.getMethodName());
            methodJson.put("methodDescription", metadata.getMethodDescription());
            List<Map<String, Object>> schedulesJson = new ArrayList<>();
            for (Scheduled schedule : metadata.getSchedules()) {
                Map<String, Object> scheduleJson = new LinkedHashMap<>();
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
                if (!schedule.description().isBlank()) {
                    putConfigLookup("description", schedule.description(), scheduleJson);
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

    private Map<String, Object> newRunningStatus(String id, boolean running) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("id", id);
        status.put("running", running);
        return status;
    }

    private void putConfigLookup(String key, String value, Map<String, Object> scheduleJson) {
        scheduleJson.put(key, value);
        String configLookup = SchedulerUtils.lookUpPropertyValue(value);
        if (!value.equals(configLookup)) {
            scheduleJson.put(key + "Config", configLookup);
        }
    }
}
