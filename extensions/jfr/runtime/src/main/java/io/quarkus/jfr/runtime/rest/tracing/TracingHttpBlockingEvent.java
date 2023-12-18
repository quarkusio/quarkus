package io.quarkus.jfr.runtime.rest.tracing;

import io.quarkus.jfr.runtime.RequestIdRelational;
import io.quarkus.jfr.runtime.SpanIdRelational;
import io.quarkus.jfr.runtime.TraceIdRelational;
import io.quarkus.jfr.runtime.TracingRequestId;
import io.quarkus.jfr.runtime.rest.AbstractHttpBlockingEvent;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

@Label("Tracing HTTP Blocking")
@Category({ "Quarkus", "REST" })
@StackTrace(false)
public class TracingHttpBlockingEvent extends AbstractHttpBlockingEvent<TracingRequestId> {

    @RequestIdRelational
    protected String requestId;

    @TraceIdRelational
    protected String traceId;

    @SpanIdRelational
    protected String spanId;

    @Override
    public void setRequestId(TracingRequestId requestId) {
        this.requestId = requestId.id;
        traceId = requestId.traceId;
        spanId = requestId.spanId;
    }
}
