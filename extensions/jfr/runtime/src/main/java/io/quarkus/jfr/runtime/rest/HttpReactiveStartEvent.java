package io.quarkus.jfr.runtime.rest;

import io.quarkus.jfr.runtime.RequestId;
import io.quarkus.jfr.runtime.RequestIdRelational;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

@Label("HTTP Reactive Start")
@Category({ "Quarkus", "REST" })
@StackTrace(false)
public class HttpReactiveStartEvent extends AbstractHttpReactiveStartEvent<RequestId> {

    @RequestIdRelational
    protected String requestId;

    @Override
    public void setRequestId(RequestId requestId) {
        this.requestId = requestId.id;
    }
}
