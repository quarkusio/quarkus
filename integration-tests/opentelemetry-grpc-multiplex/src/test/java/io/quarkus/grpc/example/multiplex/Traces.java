package io.quarkus.grpc.example.multiplex;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Traces {

    private final List<ExportTraceServiceRequest> traceRequests = new CopyOnWriteArrayList<>();

    public List<ExportTraceServiceRequest> getTraceRequests() {
        return traceRequests;
    }

    public void reset() {
        traceRequests.clear();
    }
}
