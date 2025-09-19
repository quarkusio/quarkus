package io.quarkus.qute.debug;

import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;

/**
 * Represents a "stopped" event in the Qute debugger.
 * <p>
 * This event indicates that the execution of the debuggee has been paused due to
 * a specific condition, such as hitting a breakpoint, completing a step, or
 * encountering an exception.
 * </p>
 * <p>
 * It mirrors the DAP ({@link org.eclipse.lsp4j.debug}) <code>stopped</code> event
 * and provides a simplified model for handling pause notifications in the Qute debugger.
 * </p>
 *
 * @see org.eclipse.lsp4j.debug.StoppedEventArguments
 */
public class StoppedEvent {

    /**
     * Represents the possible reasons why the debuggee execution stopped.
     * <p>
     * Each reason corresponds to a standard value defined by the
     * Debug Adapter Protocol (DAP).
     * </p>
     */
    public enum StoppedReason {

        /**
         * The execution stopped because a step operation (e.g., step over, step into) was completed.
         */
        STEP(StoppedEventArgumentsReason.STEP),

        /**
         * The execution stopped because a breakpoint was hit.
         */
        BREAKPOINT(StoppedEventArgumentsReason.BREAKPOINT),

        /**
         * The execution stopped due to an exception being thrown.
         */
        EXCEPTION(StoppedEventArgumentsReason.EXCEPTION),

        /**
         * The execution stopped because the user explicitly paused the debuggee.
         */
        PAUSE(StoppedEventArgumentsReason.PAUSE);

        private final String reason;

        StoppedReason(String reason) {
            this.reason = reason;
        }

        /**
         * Returns the string representation of this stop reason as expected by the DAP.
         *
         * @return the DAP-compatible reason string
         */
        public String getReason() {
            return reason;
        }
    }

    private final int threadId;
    private final StoppedReason reason;

    /**
     * Creates a new {@link StoppedEvent}.
     *
     * @param threadId the unique identifier of the thread that was stopped
     * @param reason the reason why the thread was stopped
     */
    public StoppedEvent(int threadId, StoppedReason reason) {
        this.threadId = threadId;
        this.reason = reason;
    }

    /**
     * Returns the unique identifier of the thread that was stopped.
     *
     * @return the thread ID
     */
    public int getThreadId() {
        return threadId;
    }

    /**
     * Returns the reason why the execution was stopped.
     *
     * @return the {@link StoppedReason} that caused the stop
     */
    public StoppedReason getStoppedReason() {
        return reason;
    }

    /**
     * Returns the reason in a format compatible with the DAP.
     * <p>
     * This is useful when sending the event back to the client.
     * </p>
     *
     * @return the DAP reason string, never {@code null}
     */
    public String getReason() {
        return getStoppedReason().getReason();
    }
}
