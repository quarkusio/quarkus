package io.quarkus.it.opentelemetry.vertx.exporter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;

public class Logs {
    private final List<ExportLogsServiceRequest> logsRequests = new CopyOnWriteArrayList<>();

    public List<ExportLogsServiceRequest> getLogsRequests() {
        return logsRequests;
    }

    public void reset() {
        logsRequests.clear();
    }
}
