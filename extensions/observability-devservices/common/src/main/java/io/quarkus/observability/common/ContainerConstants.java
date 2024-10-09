package io.quarkus.observability.common;

public final class ContainerConstants {

    // Images

    public static final String LGTM = "docker.io/grafana/otel-lgtm:0.7.5";

    // Ports

    public static final int GRAFANA_PORT = 3000;

    public static final int OTEL_GRPC_EXPORTER_PORT = 4317;
    public static final int OTEL_HTTP_EXPORTER_PORT = 4318;

    public static final String OTEL_GRPC_PROTOCOL = "grpc";
    public static final String OTEL_HTTP_PROTOCOL = "http/protobuf";
}
