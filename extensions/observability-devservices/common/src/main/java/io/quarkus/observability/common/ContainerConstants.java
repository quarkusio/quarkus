package io.quarkus.observability.common;

public final class ContainerConstants {

    // Images

    public static final String LGTM = "docker.io/grafana/otel-lgtm:0.11.0";

    // Ports

    public static final int GRAFANA_PORT = 3000;

    public static final int OTEL_GRPC_EXPORTER_PORT = 4317;
    public static final int OTEL_HTTP_EXPORTER_PORT = 4318;

    public static final String OTEL_GRPC_PROTOCOL = "grpc";
    public static final String OTEL_HTTP_PROTOCOL = "http/protobuf";

    // Overrides

    public static final int SCRAPING_INTERVAL = 10;
    public static final String OTEL_METRIC_EXPORT_INTERVAL = "10s";
    public static final String OTEL_BSP_SCHEDULE_DELAY = "3s";
    public static final String OTEL_BLRP_SCHEDULE_DELAY = "1s";

}
