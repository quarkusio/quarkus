package io.quarkus.scheduler.common.runtime;

import java.util.List;
import java.util.Objects;

import io.quarkus.scheduler.Scheduled;

public record ImmutableScheduledMethod(String invokerClassName, String declaringClassName, String methodName,
        List<Scheduled> schedules) implements ScheduledMethod {

    public ImmutableScheduledMethod {
        Objects.requireNonNull(invokerClassName);
        Objects.requireNonNull(declaringClassName);
        Objects.requireNonNull(methodName);
        schedules = List.copyOf(schedules);
    }

    public String getInvokerClassName() {
        return invokerClassName;
    }

    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<Scheduled> getSchedules() {
        return schedules;
    }

}
