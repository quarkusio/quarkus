package io.quarkus.scheduler.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SchedulerRecorder {

    public void initialize(SchedulerConfig config, List<ScheduledMethodMetadata> scheduledMethods, ExecutorService executor,
            BeanContainer container) {
        SchedulerSupport support = container.instance(SchedulerSupport.class);
        support.initialize(config, scheduledMethods, executor);
    }

}
