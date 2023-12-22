package io.quarkus.jfr.runtime.http;

import io.quarkus.jfr.runtime.RequestId;
import jdk.jfr.Event;
import jdk.jfr.Timespan;

public abstract class AbstractHttpReactiveEndEvent extends Event {

    @Timespan
    protected long processDuration;

    abstract public void setRequestId(RequestId requestId);

    public void setProcessDuration(long processDuration) {
        this.processDuration = processDuration;
    }
}
