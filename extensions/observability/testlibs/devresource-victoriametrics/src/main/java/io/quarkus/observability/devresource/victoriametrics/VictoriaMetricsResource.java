package io.quarkus.observability.devresource.victoriametrics;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.common.config.VictoriaMetricsConfig;
import io.quarkus.observability.devresource.ContainerResource;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.testcontainers.VictoriaMetricsContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class VictoriaMetricsResource extends ContainerResource<VictoriaMetricsContainer, VictoriaMetricsConfig>
        implements QuarkusTestResourceLifecycleManager {

    public VictoriaMetricsResource() {
    }

    public VictoriaMetricsResource(int port, String dataPath) {
        set(defaultContainer())
                .withMappedPort(port)
                .withFileSystemBind(dataPath, "/victoria-metrics-data");
    }

    @Override
    public VictoriaMetricsConfig config(ModulesConfiguration configuration) {
        return configuration.victoriaMetrics();
    }

    @Override
    public GenericContainer<?> container(VictoriaMetricsConfig config) {
        return set(new VictoriaMetricsContainer(config));
    }

    @Override
    public Map<String, String> config(int privatePort, String host, int publicPort) {
        String endpoint = String.format("http://%s:%s", host, publicPort);
        return config(endpoint);
    }

    @Override
    protected VictoriaMetricsContainer defaultContainer() {
        return new VictoriaMetricsContainer();
    }

    @Override
    protected Map<String, String> doStart() {
        String endpoint = container.getEndpoint(false);
        return config(endpoint);
    }

    @NotNull
    private Map<String, String> config(String endpoint) {
        return Map.of(
                "quarkus.rest-client.victoriametrics.url", endpoint,
                "quarkus.rest-client.promql.url", endpoint);
    }

    @Override
    public int order() {
        return DevResourceLifecycleManager.METRICS;
    }
}
