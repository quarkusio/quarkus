package io.quarkus.scheduler.runtime;

import java.util.List;

import io.quarkus.scheduler.Scheduled;

public class ScheduledMethodMetadata {

    private String invokerClassName;
    private String declaringClassName;
    private String methodName;
    private List<Scheduled> schedules;

    public String getInvokerClassName() {
        return invokerClassName;
    }

    public void setInvokerClassName(String invokerClassName) {
        this.invokerClassName = invokerClassName;
    }

    public String getMethodDescription() {
        return declaringClassName + "#" + methodName;
    }

    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public void setDeclaringClassName(String declaringClassName) {
        this.declaringClassName = declaringClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<Scheduled> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Scheduled> schedules) {
        this.schedules = schedules;
    }

}