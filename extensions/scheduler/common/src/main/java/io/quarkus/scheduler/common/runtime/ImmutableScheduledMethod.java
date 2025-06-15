package io.quarkus.scheduler.common.runtime;

import java.util.List;
import java.util.Objects;

import io.quarkus.scheduler.Scheduled;

public final class ImmutableScheduledMethod implements ScheduledMethod {

    private final String invokerClassName;
    private final String declaringClassName;
    private final String methodName;
    private final List<Scheduled> schedules;

    public ImmutableScheduledMethod(String invokerClassName, String declaringClassName, String methodName,
            List<Scheduled> schedules) {
        this.invokerClassName = Objects.requireNonNull(invokerClassName);
        this.declaringClassName = Objects.requireNonNull(declaringClassName);
        this.methodName = Objects.requireNonNull(methodName);
        this.schedules = List.copyOf(schedules);
    }

    @Override
    public String getInvokerClassName() {
        return invokerClassName;
    }

    @Override
    public String getDeclaringClassName() {
        return declaringClassName;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public List<Scheduled> getSchedules() {
        return schedules;
    }

}
