package io.quarkus.observability.devresource.lgtm;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.LgtmConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.devresource.Container;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.devresource.ExtensionsCatalog;
import io.quarkus.observability.devresource.testcontainers.ContainerResource;
import io.quarkus.observability.testcontainers.LgtmContainer;

public class LgtmResource extends ContainerResource<LgtmContainer, LgtmConfig> {

    private ExtensionsCatalog catalog;

    @Override
    public LgtmConfig config(ModulesConfiguration configuration) {
        return configuration.lgtm();
    }

    @Override
    public LgtmConfig config(ModulesConfiguration configuration, ExtensionsCatalog catalog) {
        this.catalog = catalog;
        return config(configuration);
    }

    @Override
    public Container<LgtmConfig> container(LgtmConfig config, ModulesConfiguration root) {
        return set(new LgtmContainer(config));
    }

    // FIXME consolidate config methods.
    @Override
    public Map<String, String> config(int privatePort, String host, int publicPort) {
        switch (privatePort) {
            case ContainerConstants.GRAFANA_PORT:
                return Map.of("grafana.endpoint", String.format("http://%s:%s", host, publicPort));
            case ContainerConstants.OTEL_GRPC_EXPORTER_PORT:
            case ContainerConstants.OTEL_HTTP_EXPORTER_PORT:
                return Map.of("otel-collector.url", String.format("%s:%s", host, publicPort));
        }
        return Map.of();
    }

    @Override
    protected LgtmContainer defaultContainer() {
        return new LgtmContainer();
    }

    @Override
    public Map<String, String> doStart() {
        String host = container.getHost();
        int otlpPort = container.getOtlpPort();

        //Set non Quarkus properties for convenience and testing.
        Map<String, String> containerConfigs = new HashMap<>();
        containerConfigs.put("grafana.endpoint", String.format("http://%s:%s", host, container.getGrafanaPort()));
        containerConfigs.put("otel-collector.url", String.format("%s:%s", host, otlpPort));

        // set relevant properties for Quarkus extensions directly
        if (catalog != null && catalog.hasOpenTelemetry()) {
            containerConfigs.put("quarkus.otel.exporter.otlp.endpoint", String.format("http://%s:%s", host, otlpPort));
            containerConfigs.put("quarkus.otel.exporter.otlp.protocol", container.getOtlpProtocol());
        }
        if (catalog != null && catalog.hasMicrometerOtlp()) {
            // always use http -- as that's what Micrometer supports
            containerConfigs.put("quarkus.micrometer.export.otlp.url",
                    String.format("http://%s:%s/v1/metrics", host,
                            container.getMappedPort(ContainerConstants.OTEL_HTTP_EXPORTER_PORT)));
        }
        return containerConfigs;
    }

    @Override
    public int order() {
        return DevResourceLifecycleManager.GRAFANA;
    }
}
