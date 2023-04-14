package io.quarkus.observability.devresource.grafana;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.GrafanaConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.devresource.ContainerResource;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.testcontainers.GrafanaContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class GrafanaResource extends ContainerResource<GrafanaContainer, GrafanaConfig>
        implements QuarkusTestResourceLifecycleManager {
    @Override
    public GrafanaConfig config(ModulesConfiguration configuration) {
        return configuration.grafana();
    }

    @Override
    public GenericContainer<?> container(GrafanaConfig config, ModulesConfiguration root) {
        return set(new GrafanaContainer(config, root));
    }

    @Override
    public Map<String, String> config(int privatePort, String host, int publicPort) {
        return Map.of("quarkus.grafana.url", String.format("%s:%s", host, publicPort));
    }

    @Override
    protected GrafanaContainer defaultContainer() {
        return new GrafanaContainer();
    }

    @Override
    public Map<String, String> doStart() {
        String host = container.getHost();
        Integer mappedPort = container.getMappedPort(3000);
        return Map.of("quarkus.grafana.url", String.format("%s:%s", host, mappedPort));
    }

    @Override
    public int order() {
        return DevResourceLifecycleManager.GRAFANA;
    }
}
