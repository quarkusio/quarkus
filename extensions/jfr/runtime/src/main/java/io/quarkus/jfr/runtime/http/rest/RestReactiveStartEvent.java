package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.SpanIdRelational;
import io.quarkus.jfr.runtime.TraceIdRelational;
import io.quarkus.jfr.runtime.http.AbstractHttpEvent;
import jdk.jfr.*;

@Label("REST Reactive Start")
@Category({ "Quarkus", "HTTP" })
@Name("io.quarkus.rest.RestReactiveStart")
@Description("This event records information at the start of the REST API executed as reactive")
@StackTrace(false)
public class RestReactiveStartEvent extends AbstractHttpEvent {

    @TraceIdRelational
    protected String traceId;

    @SpanIdRelational
    protected String spanId;

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }
}
