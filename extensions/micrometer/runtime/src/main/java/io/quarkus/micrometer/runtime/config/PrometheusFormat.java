package io.quarkus.micrometer.runtime.config;

import io.prometheus.client.exporter.common.TextFormat;

public enum PrometheusFormat {
    OPENMETRICS(TextFormat.CONTENT_TYPE_OPENMETRICS_100),
    PLAIN(TextFormat.CONTENT_TYPE_004);

    private final String contentType;

    PrometheusFormat(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
