package io.quarkus.virtual.threads;

/**
 * The default execution mode used for blocking methods that do not declare an explicit execution model
 * annotation, such as {@code @Blocking}, {@code @NonBlocking} or {@code @RunOnVirtualThread}.
 */
public enum DefaultExecutionMode {

    /**
     * Blocking methods without an explicit execution model annotation are executed on a worker thread.
     */
    WORKER,

    /**
     * Blocking methods without an explicit execution model annotation are executed on a virtual thread.
     */
    VIRTUAL_THREAD;

    public boolean isVirtualThread() {
        return this == VIRTUAL_THREAD;
    }
}
