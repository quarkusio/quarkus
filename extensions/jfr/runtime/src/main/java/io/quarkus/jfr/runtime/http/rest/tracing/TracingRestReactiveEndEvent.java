package io.quarkus.jfr.runtime.http.rest.tracing;

import io.quarkus.jfr.runtime.*;
import io.quarkus.jfr.runtime.http.AbstractHttpReactiveEndEvent;
import jdk.jfr.*;

@Label("Tracing REST Reactive End")
@Category({ "Quarkus", "HTTP" })
@Name("io.quarkus.rest.TracingRestReactiveEnd")
@Description("This event records information at the end of the REST API executed as reactive with OpenTelemetry")
@StackTrace(false)
public class TracingRestReactiveEndEvent extends AbstractHttpReactiveEndEvent {

    @RequestIdRelational
    protected String requestId;

    @TraceIdRelational
    protected String traceId;

    @SpanIdRelational
    protected String spanId;

    @Override
    public void setRequestId(RequestId requestId) {
        TracingRequestId tracingRequestId = (TracingRequestId) requestId;
        this.requestId = tracingRequestId.id;
        traceId = tracingRequestId.traceId;
        spanId = tracingRequestId.spanId;
    }
}
