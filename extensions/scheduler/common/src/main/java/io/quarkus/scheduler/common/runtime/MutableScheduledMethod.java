package io.quarkus.scheduler.common.runtime;

import java.util.List;

import io.quarkus.scheduler.Scheduled;

// This class is mutable so that it can be serialized in a recorder method
public class MutableScheduledMethod implements ScheduledMethod {

    private String invokerClassName;
    private String declaringClassName;
    private String methodName;
    private List<Scheduled> schedules;

    @Override
    public String getInvokerClassName() {
        return invokerClassName;
    }

    public void setInvokerClassName(String invokerClassName) {
        this.invokerClassName = invokerClassName;
    }

    @Override
    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public void setDeclaringClassName(String declaringClassName) {
        this.declaringClassName = declaringClassName;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public List<Scheduled> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Scheduled> schedules) {
        this.schedules = schedules;
    }

}
