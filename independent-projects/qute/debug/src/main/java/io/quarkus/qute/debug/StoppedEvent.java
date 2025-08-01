package io.quarkus.qute.debug;

import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;

/**
 * The event indicates that the execution of the debuggee has stopped due to
 * some condition.
 * <p>
 * This can be caused by a break point previously set, a stepping request has
 * completed, by executing a debugger statement etc.
 * <p>
 */
public class StoppedEvent {

    public enum StoppedReason {

        STEP(StoppedEventArgumentsReason.STEP), //
        BREAKPOINT(StoppedEventArgumentsReason.BREAKPOINT), //
        EXCEPTION(StoppedEventArgumentsReason.EXCEPTION), //
        PAUSE(StoppedEventArgumentsReason.PAUSE);

        private final String reason;

        StoppedReason(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

    private final int threadId;

    private final StoppedReason reason;

    public StoppedEvent(int threadId, StoppedReason reason) {
        this.threadId = threadId;
        this.reason = reason;
    }

    /**
     * Returns the thread which was stopped.
     *
     * @return the thread which was stopped.
     */
    public int getThreadId() {
        return threadId;
    }

    /**
     * Returns the reason for the event.
     *
     * @return the reason for the event.
     */
    public StoppedReason getStoppedReason() {
        return reason;
    }

    public String getReason() {
        return getStoppedReason().getReason();
    }

}
