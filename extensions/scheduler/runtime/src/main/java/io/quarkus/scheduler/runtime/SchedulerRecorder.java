package io.quarkus.scheduler.runtime;

import java.util.ArrayList;
import java.util.List;

import com.cronutils.model.CronType;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;
import io.quarkus.scheduler.common.runtime.ScheduledMethodMetadata;
import io.quarkus.scheduler.common.runtime.SchedulerContext;

public class SchedulerRecorder {

    public static SchedulerContext createContext(CronType cronType, List<ScheduledMethodMetadata> scheduledMethods,
            boolean forceSchedulerStart, String autoImplementation) {
        List<ScheduledMethod> converted = new ArrayList<>(scheduledMethods.size());
        for (ScheduledMethodMetadata scheduledMethod : scheduledMethods) {
            converted.add(scheduledMethod.toScheduledMethod());
        }
        List<ScheduledMethod> metadata = List.copyOf(converted);
        return new SchedulerContext() {

            @Override
            public CronType getCronType() {
                return cronType;
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
}
