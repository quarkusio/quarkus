package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.RequestId;
import io.quarkus.jfr.runtime.RequestIdRelational;
import io.quarkus.jfr.runtime.http.AbstractHttpReactiveEndEvent;
import jdk.jfr.*;

@Label("REST Reactive End")
@Category({ "Quarkus", "HTTP" })
@Name("io.quarkus.rest.RestReactiveEnd")
@Description("This event records information at the end of the REST API executed as reactive")
@StackTrace(false)
public class RestReactiveEndEvent extends AbstractHttpReactiveEndEvent {

    @RequestIdRelational
    protected String requestId;

    @Override
    public void setRequestId(RequestId requestId) {
        this.requestId = requestId.id;
    }
}
