package io.quarkus.jfr.runtime.http.rest.tracing;

import io.quarkus.jfr.runtime.*;
import io.quarkus.jfr.runtime.http.AbstractHttpBlockingEvent;
import jdk.jfr.*;

@Label("Tracing REST Blocking")
@Category({ "Quarkus", "HTTP" })
@Name("io.quarkus.rest.TracingRestBlocking")
@Description("This event records information about the REST API executed as blocking with OpenTelemetry")
@StackTrace(false)
public class TracingRestBlockingEvent extends AbstractHttpBlockingEvent {

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
