package io.quarkus.qute.debug;

import java.io.Serializable;

/**
 * Thread event thrown when a debuggee Thread is started or exited.
 *
 */
public class ThreadEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ThreadStatus {
        STARTED, //
        EXITED;
    }

    private final long threadId;

    private final ThreadStatus threadStatus;

    public ThreadEvent(long threadId, ThreadStatus threadStatus) {
        this.threadId = threadId;
        this.threadStatus = threadStatus;
    }

    public long getThreadId() {
        return threadId;
    }

    public ThreadStatus getThreadStatus() {
        return threadStatus;
    }

}
