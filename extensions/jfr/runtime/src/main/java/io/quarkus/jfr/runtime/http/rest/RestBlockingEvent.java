package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.SpanIdRelational;
import io.quarkus.jfr.runtime.TraceIdRelational;
import io.quarkus.jfr.runtime.http.AbstractHttpEvent;
import jdk.jfr.*;

@Label("REST Blocking")
@Category({ "Quarkus", "HTTP" })
@Name("io.quarkus.rest.RestBlocking")
@Description("This event records information about the REST API executed as blocking")
@StackTrace(false)
public class RestBlockingEvent extends AbstractHttpEvent {

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
