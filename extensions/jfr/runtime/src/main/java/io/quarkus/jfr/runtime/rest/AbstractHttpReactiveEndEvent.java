package io.quarkus.jfr.runtime.rest;

import io.quarkus.jfr.runtime.RequestId;
import jdk.jfr.Event;
import jdk.jfr.Timespan;

public abstract class AbstractHttpReactiveEndEvent<ID extends RequestId> extends Event {

    @Timespan
    protected long processDuration;

    abstract public void setRequestId(ID requestId);

    public void setProcessDuration(long processDuration) {
        this.processDuration = processDuration;
    }
}
