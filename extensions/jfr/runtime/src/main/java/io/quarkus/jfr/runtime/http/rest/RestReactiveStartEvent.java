package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.RequestId;
import io.quarkus.jfr.runtime.RequestIdRelational;
import io.quarkus.jfr.runtime.http.AbstractHttpReactiveStartEvent;
import jdk.jfr.*;

@Label("REST Reactive Start")
@Category({ "Quarkus", "HTTP" })
@Name("io.quarkus.rest.RestReactiveStart")
@Description("This event records information at the start of the REST API executed as reactive")
@StackTrace(false)
public class RestReactiveStartEvent extends AbstractHttpReactiveStartEvent {

    @RequestIdRelational
    protected String requestId;

    @Override
    public void setRequestId(RequestId requestId) {
        this.requestId = requestId.id;
    }
}
