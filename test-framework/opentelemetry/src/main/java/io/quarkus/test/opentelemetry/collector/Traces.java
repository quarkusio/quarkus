package io.quarkus.test.opentelemetry.collector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

public final class Traces {

    private final List<ExportTraceServiceRequest> traceRequests = new CopyOnWriteArrayList<>();

    public List<ExportTraceServiceRequest> getTraceRequests() {
        return traceRequests;
    }

    public void reset() {
        traceRequests.clear();
    }
}
