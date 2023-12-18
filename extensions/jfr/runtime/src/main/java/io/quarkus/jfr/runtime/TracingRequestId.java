package io.quarkus.jfr.runtime;

public class TracingRequestId extends RequestId {

    public final String traceId;

    public final String spanId;

    public TracingRequestId(String id, String traceId, String spanId) {
        super(id);
        this.traceId = traceId;
        this.spanId = spanId;
    }
}
