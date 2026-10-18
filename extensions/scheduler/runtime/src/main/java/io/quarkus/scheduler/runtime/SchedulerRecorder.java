package io.quarkus.scheduler.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.cronutils.model.CronType;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.common.runtime.ImmutableScheduledMethod;
import io.quarkus.scheduler.common.runtime.MutableScheduledMethod;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;
import io.quarkus.scheduler.common.runtime.SchedulerContext;

@Recorder
public class SchedulerRecorder {
    private final SchedulerConfig schedulerConfig;

    public SchedulerRecorder(final SchedulerConfig schedulerConfig) {
        this.schedulerConfig = schedulerConfig;
    }

    public Supplier<Object> createContext(List<MutableScheduledMethod> scheduledMethods, boolean forceSchedulerStart,
            String autoImplementation) {
        // Defensive design - make an immutable copy of the scheduled method metadata
        List<ScheduledMethod> metadata = immutableCopy(scheduledMethods);
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return new SchedulerContext() {

                    @Override
                    public CronType getCronType() {
                        return schedulerConfig.cronType();
                    }

                    @Override
                    public List<ScheduledMethod> getScheduledMethods() {
                        return metadata;
                    }

                    @Override
                    public boolean forceSchedulerStart() {
                        return forceSchedulerStart;
                    }

                    @Override
                    public List<ScheduledMethod> getScheduledMethods(String implementation) {
                        List<ScheduledMethod> ret = new ArrayList<>(metadata.size());
                        for (ScheduledMethod method : metadata) {
                            for (Scheduled scheduled : method.getSchedules()) {
                                if (matchesImplementation(scheduled, implementation)) {
                                    ret.add(method);
                                }
                            }
                        }
                        return ret;
                    }

                    @Override
                    public boolean matchesImplementation(Scheduled scheduled, String implementation) {
                        return scheduled.executeWith().equals(implementation) || ((autoImplementation.equals(implementation))
                                && scheduled.executeWith().equals(Scheduled.AUTO));
                    }

                    @Override
                    public String autoImplementation() {
                        return autoImplementation;
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
