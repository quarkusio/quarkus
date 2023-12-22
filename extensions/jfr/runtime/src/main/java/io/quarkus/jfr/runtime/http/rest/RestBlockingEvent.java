package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.RequestId;
import io.quarkus.jfr.runtime.RequestIdRelational;
import io.quarkus.jfr.runtime.http.AbstractHttpBlockingEvent;
import jdk.jfr.*;

@Label("REST Blocking")
@Category({ "Quarkus", "HTTP" })
@Name("io.quarkus.rest.RestBlocking")
@Description("This event records information about the REST API executed as blocking")
@StackTrace(false)
public class RestBlockingEvent extends AbstractHttpBlockingEvent {

    @RequestIdRelational
    protected String requestId;

    public void setRequestId(RequestId requestId) {
        this.requestId = requestId.id;
    }
}
