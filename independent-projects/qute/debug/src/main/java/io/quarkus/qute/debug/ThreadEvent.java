package io.quarkus.qute.debug;

import org.eclipse.lsp4j.debug.ThreadEventArgumentsReason;

/**
 * Represents a thread event in the Qute debugger.
 * <p>
 * This event is emitted when a debuggee thread is either started or exited.
 * It mirrors the DAP ({@link org.eclipse.lsp4j.debug}) {@code thread} event and
 * provides an easy-to-use model for handling thread lifecycle notifications
 * within the Qute debugger.
 * </p>
 *
 * @see org.eclipse.lsp4j.debug.ThreadEventArguments
 */
public class ThreadEvent {

    /**
     * Represents the possible statuses of a thread during its lifecycle in the debugger.
     * <p>
     * Each status corresponds to a reason defined by the Debug Adapter Protocol (DAP).
     * </p>
     */
    public enum ThreadStatus {

        /**
         * Indicates that the thread has been started.
         */
        STARTED(ThreadEventArgumentsReason.STARTED),

        /**
         * Indicates that the thread has exited.
         */
        EXITED(ThreadEventArgumentsReason.EXITED);

        private final String reason;

        ThreadStatus(String reason) {
            this.reason = reason;
        }

        /**
         * Returns the string representation of this status as expected by the DAP.
         *
         * @return the DAP-compatible reason string
         */
        public String getReason() {
            return reason;
        }
    }

    private final int threadId;
    private final ThreadStatus threadStatus;

    /**
     * Creates a new {@link ThreadEvent}.
     *
     * @param threadId the unique identifier of the thread in the debuggee
     * @param threadStatus the status of the thread (started or exited)
     */
    public ThreadEvent(int threadId, ThreadStatus threadStatus) {
        this.threadId = threadId;
        this.threadStatus = threadStatus;
    }

    /**
     * Returns the unique identifier of the thread.
     *
     * @return the thread ID
     */
    public int getThreadId() {
        return threadId;
    }

    /**
     * Returns the status of the thread.
     *
     * @return the current {@link ThreadStatus} of the thread
     */
    public ThreadStatus getThreadStatus() {
        return threadStatus;
    }

    /**
     * Returns the reason for the thread event in a format compatible with the DAP.
     * <p>
     * This is typically used when sending events to the client via the
     * Debug Adapter Protocol.
     * </p>
     *
     * @return the DAP reason string, never {@code null}
     */
    public String getReason() {
        return getThreadStatus().getReason();
    }
}
