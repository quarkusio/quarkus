package io.quarkus.micrometer.opentelemetry.deployment.common;

public class GuardedResult {

    private boolean complete;
    private NullPointerException withException;

    public synchronized Object get() {
        while (!complete) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Intentionally empty
            }
        }

        if (withException == null) {
            return new Object();
        }

        throw withException;
    }

    public synchronized void complete() {
        complete(null);
    }

    public synchronized void complete(NullPointerException withException) {
        this.complete = true;
        this.withException = withException;
        notifyAll();
    }

}
