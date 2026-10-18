package io.quarkus.opentelemetry.deployment.exporter.otlp;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item to be used by Quarkiverse exporters to register themselves as an external exporter.
 */
final public class ExternalOtelExporterBuildItem extends MultiBuildItem {
    final private String exporterName;

    public ExternalOtelExporterBuildItem(String exporterName) {
        this.exporterName = exporterName;
    }

    public String getExporterName() {
        return exporterName;
    }
}
