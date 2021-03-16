package io.quarkus.scheduler.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.cronutils.model.CronType;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SchedulerRecorder {

    public Supplier<Object> createContext(SchedulerConfig config, ExecutorService executorService,
            List<ScheduledMethodMetadata> scheduledMethods) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return new SchedulerContext() {

                    @Override
                    public ExecutorService getExecutor() {
                        return executorService;
                    }

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
