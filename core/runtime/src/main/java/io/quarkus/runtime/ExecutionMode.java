package io.quarkus.runtime;

/**
 * The runtime execution mode.
 */
public enum ExecutionMode {

    /**
     * Static initializiation.
     */
    STATIC_INIT,

    /**
     * Runtime initialization.
     */
    RUNTIME_INIT,

    /**
     * The application is running.
     */
    RUNNING,

    UNSET,
    ;

    public static ExecutionMode current() {
        return ExecutionModeManager.getExecutionMode();
    }

}
