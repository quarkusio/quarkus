package io.quarkus.scheduler;

/**
 * This event is fired synchronously and asynchronously when an execution of a scheduled method throw an exception.
 */
public class FailedExecution {

    private final ScheduledExecution execution;
    private final Throwable exception;

    public FailedExecution(ScheduledExecution execution) {
        this(execution, null);
    }

    public FailedExecution(ScheduledExecution execution, Throwable exception) {
        this.execution = execution;
        this.exception = exception;
    }

    public ScheduledExecution getExecution() {
        return execution;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Failed execution of [").append(execution.getTrigger().getId())
                .append("]");
        if (exception != null) {
            builder.append(": ").append(exception.getMessage());
        }
        return builder.toString();
    }

}
