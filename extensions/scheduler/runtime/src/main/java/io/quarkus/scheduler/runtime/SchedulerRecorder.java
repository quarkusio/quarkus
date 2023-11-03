package io.quarkus.scheduler.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.cronutils.model.CronType;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.scheduler.common.runtime.ImmutableScheduledMethod;
import io.quarkus.scheduler.common.runtime.MutableScheduledMethod;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;
import io.quarkus.scheduler.common.runtime.SchedulerContext;

@Recorder
public class SchedulerRecorder {

    public Supplier<Object> createContext(SchedulerConfig config,
            List<MutableScheduledMethod> scheduledMethods) {
        // Defensive design - make an immutable copy of the scheduled method metadata
        List<ScheduledMethod> metadata = immutableCopy(scheduledMethods);
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return new SchedulerContext() {

                    @Override
                    public CronType getCronType() {
                        return config.cronType;
                    }

                    @Override
                    public List<ScheduledMethod> getScheduledMethods() {
                        return metadata;
                    }
                };
            }
        };
    }

    private List<ScheduledMethod> immutableCopy(List<MutableScheduledMethod> scheduledMethods) {
        List<ScheduledMethod> metadata = new ArrayList<>(scheduledMethods.size());
        for (ScheduledMethod scheduledMethod : scheduledMethods) {
            metadata.add(new ImmutableScheduledMethod(scheduledMethod.getInvokerClassName(),
                    scheduledMethod.getDeclaringClassName(), scheduledMethod.getMethodName(), scheduledMethod.getSchedules()));
        }
        return List.copyOf(metadata);
    }
}
