package io.quarkus.qute.debug;

/**
 * Represents the possible states of the Qute debugger during a debugging session.
 * <p>
 * This enum is used to track the current lifecycle state of the debugger, from initialization
 * to execution, suspension, and termination.
 * </p>
 */
public enum DebuggerState {

    /**
     * The state is unknown, typically used as a default before the debugger has been initialized.
     */
    UNKNOWN,

    /**
     * The debugger has been initialized but has not yet started executing the debuggee.
     */
    INITIALIZED,

    /**
     * The debugger is actively running the debuggee without being paused or stopped.
     */
    RUNNING,

    /**
     * The debugger has temporarily suspended the execution of the debuggee,
     * usually due to hitting a breakpoint, completing a step, or a manual pause request.
     */
    SUSPENDED,

    /**
     * The debugger has completely stopped the execution and the session has ended.
     */
    STOPPED;
}
