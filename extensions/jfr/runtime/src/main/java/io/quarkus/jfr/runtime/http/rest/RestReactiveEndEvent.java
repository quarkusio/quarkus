package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.SpanIdRelational;
import io.quarkus.jfr.runtime.TraceIdRelational;
import jdk.jfr.*;

@Label("REST Reactive End")
@Category({"Quarkus", "HTTP"})
@Name("io.quarkus.rest.RestReactiveEnd")
@Description("This event records information at the end of the REST API executed as reactive")
@StackTrace(false)
public class RestReactiveEndEvent extends Event {

    @TraceIdRelational
    protected String traceId;

    @SpanIdRelational
    protected String spanId;

    @Timespan
    protected long processDuration;

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public void setProcessDuration(long processDuration) {
        this.processDuration = processDuration;
    }
}
