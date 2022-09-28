package io.quarkus.scheduler.runtime;

import java.util.List;
import java.util.function.Supplier;

import com.cronutils.model.CronType;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.scheduler.common.runtime.ScheduledMethodMetadata;
import io.quarkus.scheduler.common.runtime.SchedulerContext;

@Recorder
public class SchedulerRecorder {

    public Supplier<Object> createContext(SchedulerConfig config,
            List<ScheduledMethodMetadata> scheduledMethods) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return new SchedulerContext() {

                    @Override
                    public CronType getCronType() {
                        return config.cronType;
                    }

                    @Override
                    public List<ScheduledMethodMetadata> getScheduledMethods() {
                        return scheduledMethods;
                    }
                };
            }
        };
    }
}
