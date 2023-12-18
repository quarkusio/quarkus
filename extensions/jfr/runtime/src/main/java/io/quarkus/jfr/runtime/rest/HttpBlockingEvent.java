package io.quarkus.jfr.runtime.rest;

import io.quarkus.jfr.runtime.RequestId;
import io.quarkus.jfr.runtime.RequestIdRelational;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

@Label("HTTP Blocking")
@Category({ "Quarkus", "REST" })
@StackTrace(false)
public class HttpBlockingEvent extends AbstractHttpBlockingEvent<RequestId> {

    @RequestIdRelational
    protected String requestId;

    public void setRequestId(RequestId requestId) {
        this.requestId = requestId.id;
    }
}
