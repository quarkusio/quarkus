package io.quarkus.scheduler;

/**
 * This event is fired synchronously and asynchronously when an execution of a scheduled method is successful.
 */
public class SuccessfulExecution {

    private final ScheduledExecution execution;

    public SuccessfulExecution(ScheduledExecution execution) {
        this.execution = execution;
    }

    public ScheduledExecution getExecution() {
        return execution;
    }

    @Override
    public String toString() {
        return "Success execution of [" + execution.getTrigger().getId() + "]";
    }

}
