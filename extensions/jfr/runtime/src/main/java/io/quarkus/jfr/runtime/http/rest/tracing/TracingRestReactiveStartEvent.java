package io.quarkus.jfr.runtime.http.rest.tracing;

import io.quarkus.jfr.runtime.*;
import io.quarkus.jfr.runtime.http.AbstractHttpReactiveStartEvent;
import jdk.jfr.*;

@Label("Tracing REST Reactive Start")
@Category({ "Quarkus", "HTTP" })
@Name("io.quarkus.rest.TracingRestReactiveStart")
@Description("This event records information at the start of the REST API executed as reactive with OpenTelemetry")
@StackTrace(false)
public class TracingRestReactiveStartEvent extends AbstractHttpReactiveStartEvent {

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
