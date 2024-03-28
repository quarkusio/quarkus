package io.quarkus.observability.common;

public final class ContainerConstants {

    // Images

    public static final String LGTM = "docker.io/grafana/otel-lgtm:0.4.0";

    // Ports

    public static final int GRAFANA_PORT = 3000;

    public static final int OTEL_GRPC_EXPORTER_PORT = 4317;
    public static final int OTEL_HTTP_EXPORTER_PORT = 4318;
}
