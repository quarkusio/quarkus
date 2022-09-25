package io.quarkus.qute.debug;

import java.io.Serializable;

/**
 * The event indicates that the execution of the debuggee has stopped due to
 * some condition.
 * <p>
 * This can be caused by a break point previously set, a stepping request has
 * completed, by executing a debugger statement etc.
 * <p>
 */
public class StoppedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum StoppedReason {
        STEP, //
        BREAKPOINT, //
        EXCEPTION, //
        PAUSE;
    }

    private final long threadId;

    private final StoppedReason reason;

    public StoppedEvent(long threadId, StoppedReason reason) {
        this.threadId = threadId;
        this.reason = reason;
    }

    /**
     * Returns the thread which was stopped.
     *
     * @return the thread which was stopped.
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * Returns the reason for the event.
     *
     * @return the reason for the event.
     */
    public StoppedReason getReason() {
        return reason;
    }

}
