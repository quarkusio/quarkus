package io.quarkus.scheduler.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.scheduler.Scheduled;

/**
 *
 * @author Martin Kouba
 */
@Recorder
public class SchedulerDeploymentRecorder {

    public static final String SCHEDULES_KEY = "schedules";
    public static final String INVOKER_KEY = "invoker";
    public static final String DESC_KEY = "desc";

    @SuppressWarnings("unchecked")
    public void registerSchedules(List<Map<String, Object>> configurations, BeanContainer container) {
        SchedulerConfiguration schedulerConfig = container.instance(SchedulerConfiguration.class);
        for (Map<String, Object> config : configurations) {
            schedulerConfig.register(config.get(INVOKER_KEY).toString(), config.get(DESC_KEY).toString(),
                    (List<Scheduled>) config.get(SCHEDULES_KEY));
        }
    }

    public void registerConfiguration(SchedulerRuntimeConfig schedulerRuntimeConfig) {
        SchedulerConfigHolder.holdRuntimeConfig(schedulerRuntimeConfig);
    }

    public void registerConfiguration(SchedulerBuildTimeConfig schedulerBuildTimeConfig) {
        SchedulerConfigHolder.holdBuildtimeConfig(schedulerBuildTimeConfig);
    }
}
