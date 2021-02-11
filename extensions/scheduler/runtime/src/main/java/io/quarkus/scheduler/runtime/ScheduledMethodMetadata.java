package io.quarkus.scheduler.runtime;

import java.util.List;

import io.quarkus.scheduler.Scheduled;

public class ScheduledMethodMetadata {

    private String invokerClassName;
    private String methodDescription;
    private List<Scheduled> schedules;

    public String getInvokerClassName() {
        return invokerClassName;
    }

    public void setInvokerClassName(String invokerClassName) {
        this.invokerClassName = invokerClassName;
    }

    public String getMethodDescription() {
        return methodDescription;
    }

    public void setMethodDescription(String description) {
        this.methodDescription = description;
    }

    public List<Scheduled> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Scheduled> schedules) {
        this.schedules = schedules;
    }

}