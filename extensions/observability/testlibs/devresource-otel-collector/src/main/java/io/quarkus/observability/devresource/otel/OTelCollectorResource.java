package io.quarkus.observability.devresource.otel;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.common.config.OTelConfig;
import io.quarkus.observability.devresource.ContainerResource;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.testcontainers.OTelCollectorContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class OTelCollectorResource extends ContainerResource<OTelCollectorContainer, OTelConfig>
        implements QuarkusTestResourceLifecycleManager {
    @Override
    public OTelConfig config(ModulesConfiguration configuration) {
        return configuration.otel();
    }

    @Override
    public GenericContainer<?> container(OTelConfig config, ModulesConfiguration root) {
        return set(new OTelCollectorContainer(config, root));
    }

    @Override
    public Map<String, String> config(int privatePort, String host, int publicPort) {
        switch (privatePort) {
            case OTelCollectorContainer.OTEL_GRPC_EXPORTER_PORT:
                return Map.of("quarkus.otel-collector.grpc", String.format("%s:%s", host, publicPort));
            case OTelCollectorContainer.OTEL_HTTP_EXPORTER_PORT:
                return Map.of("quarkus.otel-collector.http", String.format("%s:%s", host, publicPort));
        }
        return Map.of();
    }

    @Override
    protected OTelCollectorContainer defaultContainer() {
        return new OTelCollectorContainer();
    }

    @Override
    protected Map<String, String> doStart() {
        String host = container.getHost();
        return Map.of(
                "quarkus.otel-collector.grpc", String.format("%s:%s", host, container.getOtelGrpcExporterPort()),
                "quarkus.otel-collector.http", String.format("%s:%s", host, container.getOtelHttpExporterPort()));
    }

    @Override
    public int order() {
        return DevResourceLifecycleManager.OTEL;
    }
}
