package io.quarkus.observability.devresource.jaeger;

import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.JaegerConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.devresource.ContainerResource;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.testcontainers.JaegerContainer;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class JaegerResource extends ContainerResource<JaegerContainer, JaegerConfig>
        implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = Logger.getLogger(JaegerResource.class);

    private static final String OTEL_CONFIG_ENDPOINT = "quarkus.otel.exporter.otlp.traces.endpoint";

    @Override
    public JaegerConfig config(ModulesConfiguration configuration) {
        return configuration.jaeger();
    }

    @Override
    public boolean enable() {
        if (ConfigUtils.isPropertyPresent(OTEL_CONFIG_ENDPOINT)) {
            log.debug("Not starting Dev Services for Jaeger as '" + OTEL_CONFIG_ENDPOINT + "' has been provided");
            return false;
        }
        return true;
    }

    @Override
    public GenericContainer<?> container(JaegerConfig config) {
        return set(new JaegerContainer(config));
    }

    @Override
    public Map<String, String> config(int privatePort, String host, int publicPort) {
        switch (privatePort) {
            case JaegerContainer.JAEGER_ENDPOINT_PORT:
                return Map.of("quarkus.jaeger.endpoint", String.format("%s:%s", host, publicPort));
            case JaegerContainer.JAEGER_CONSOLE_PORT:
                return Map.of("quarkus.jaeger.console", String.format("%s:%s", host, publicPort));
        }
        return Map.of();
    }

    @Override
    protected JaegerContainer defaultContainer() {
        return new JaegerContainer();
    }

    @Override
    public Map<String, String> doStart() {
        String host = container.getHost();
        return Map.of(
                "quarkus.jaeger.endpoint", String.format("%s:%s", host, container.getJaegerEndpointPort()),
                "quarkus.jaeger.console", String.format("%s:%s", host, container.getJaegerConsolePort()));
    }

    @Override
    public int order() {
        return DevResourceLifecycleManager.JAEGER;
    }
}
