package io.quarkus.qute.debug;

import org.eclipse.lsp4j.debug.ThreadEventArgumentsReason;

/**
 * Thread event thrown when a debuggee Thread is started or exited.
 *
 */
public class ThreadEvent {

    public enum ThreadStatus {

        STARTED(ThreadEventArgumentsReason.STARTED), //
        EXITED(ThreadEventArgumentsReason.EXITED);

        private final String reason;

        ThreadStatus(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

    private final int threadId;

    private final ThreadStatus threadStatus;

    public ThreadEvent(int threadId, ThreadStatus threadStatus) {
        this.threadId = threadId;
        this.threadStatus = threadStatus;
    }

    public int getThreadId() {
        return threadId;
    }

    public ThreadStatus getThreadStatus() {
        return threadStatus;
    }

    public String getReason() {
        return getThreadStatus().getReason();
    }

}
