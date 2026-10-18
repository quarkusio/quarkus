package io.quarkus.observability.devresource.lgtm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.LgtmConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.devresource.Container;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.devresource.ExtensionsCatalog;
import io.quarkus.observability.devresource.testcontainers.ContainerResource;
import io.quarkus.observability.testcontainers.LgtmContainer;

public class LgtmResource extends ContainerResource<LgtmContainer, LgtmConfig> {

    private static final Logger log = Logger.getLogger(LgtmResource.class.getName());

    protected static final Set<String> SCRAPING_REGISTRIES = Set.of(
            "io.micrometer.prometheusmetrics.PrometheusMeterRegistry");

    private static final String OTEL_OTLP_ENDPOINT = "quarkus.otel.exporter.otlp.endpoint";
    private static final String OTEL_OTLP_TRACES_ENDPOINT = "quarkus.otel.exporter.otlp.traces.endpoint";
    private static final String OTEL_OTLP_METRICS_ENDPOINT = "quarkus.otel.exporter.otlp.metrics.endpoint";
    private static final String OTEL_OTLP_LOGS_ENDPOINT = "quarkus.otel.exporter.otlp.logs.endpoint";
    private static final String MICROMETER_OTLP_URL = "quarkus.micrometer.export.otlp.url";

    protected static final Function<String, Boolean> TCCL_FN = s -> {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass(s);
            return true;
        } catch (Exception e) {
            // any exception
            return false;
        }
    };

    private ExtensionsCatalog catalog;
    private LgtmConfig config;

    @Override
    public LgtmConfig config(ModulesConfiguration configuration) {
        LgtmConfig config = configuration.lgtm();
        this.config = config;
        return config;
    }

    @Override
    public LgtmConfig config(ModulesConfiguration configuration, ExtensionsCatalog catalog) {
        this.catalog = catalog;
        return config(configuration);
    }

    @Override
    public Container<LgtmConfig> container(LgtmConfig config, ModulesConfiguration root) {
        return set(new LgtmContainer(config, isScrapingRequired(catalog.classChecker())));
    }

    @Override
    public boolean enable() {
        String explicitEndpoint = findExplicitlyConfiguredOtlpEndpoint();
        if (explicitEndpoint == null) {
            return true;
        }

        List<String> configuredPorts = configuredPorts();
        if (!configuredPorts.isEmpty()) {
            log.warnf(
                    "Not starting LGTM Dev Services because '%s' is explicitly configured, but %s (is/are) also set and will have no effect.",
                    explicitEndpoint, configuredPorts);
        } else {
            log.infof("Not starting LGTM Dev Services: '%s' is explicitly configured.", explicitEndpoint);
        }
        return false;
    }

    private String findExplicitlyConfiguredOtlpEndpoint() {
        var cfg = ConfigProvider.getConfig();
        if (catalog != null && catalog.hasOpenTelemetry()) {
            for (String key : List.of(
                    OTEL_OTLP_ENDPOINT,
                    OTEL_OTLP_TRACES_ENDPOINT,
                    OTEL_OTLP_METRICS_ENDPOINT,
                    OTEL_OTLP_LOGS_ENDPOINT)) {
                if (cfg.getOptionalValue(key, String.class).isPresent()) {
                    return key;
                }
            }
        }
        if (catalog != null && catalog.hasMicrometerOtlp()) {
            if (cfg.getOptionalValue(MICROMETER_OTLP_URL, String.class).isPresent()) {
                return MICROMETER_OTLP_URL;
            }
        }
        return null;
    }

    private List<String> configuredPorts() {
        List<String> configured = new ArrayList<>();
        if (config != null) {
            if (config.grafanaPort().isPresent()) {
                configured.add("quarkus.observability.lgtm.grafana-port");
            }
            if (config.otelGrpcPort().isPresent()) {
                configured.add("quarkus.observability.lgtm.otel-grpc-port");
            }
            if (config.otelHttpPort().isPresent()) {
                configured.add("quarkus.observability.lgtm.otel-http-port");
            }
        }
        return configured;
    }

    private boolean isScrapingRequired(Function<String, Boolean> checker) {
        boolean result = false;
        String foundRegistry = null;
        for (String clazz : SCRAPING_REGISTRIES) {
            if (checker.apply(clazz)) {
                foundRegistry = clazz;
                result = true;
                break;
            }
        }

        if (result && (catalog != null && catalog.hasMicrometerOtlp())) {
            log.warnf("Multiple Micrometer registries found - OTLP and %s, no Prometheus scraping required.", foundRegistry);
            return false;
        }

        return result;
    }

    private int getPrivateOtlpPort() {
        if (config != null) {
            return LgtmContainer.getPrivateOtlpPort(config.otlpProtocol());
        } else {
            return -1;
        }
    }

    private Map<String, String> config(int privatePort, String host) {
        return config(privatePort, host, container.getMappedPort(privatePort));
    }

    @Override
    public Map<String, String> config(int privatePort, String host, int publicPort) {

        Map<String, String> containerConfigs = new HashMap<>();

        switch (privatePort) {
            case ContainerConstants.GRAFANA_PORT:
                containerConfigs.put("grafana.endpoint", String.format("http://%s:%s", host, publicPort));
                break;
            case ContainerConstants.PROMETHEUS_PORT:
                containerConfigs.put("prometheus.endpoint", String.format("http://%s:%s", host, publicPort));
                break;
            case ContainerConstants.TEMPO_MCP_PORT:
                containerConfigs.put("tempo-mcp.endpoint", String.format("http://%s:%s", host, publicPort));
                break;
            case ContainerConstants.OTEL_HTTP_EXPORTER_PORT:
                if (catalog != null && catalog.hasMicrometerOtlp()) {

                    containerConfigs.put("quarkus.micrometer.export.otlp.url",
                            String.format("http://%s:%s/v1/metrics", host,
                                    publicPort));
                }
                // No break, fall through
            case ContainerConstants.OTEL_GRPC_EXPORTER_PORT:
                containerConfigs.put("otel-collector.url", String.format("%s:%s", host, publicPort));
                break;
        }

        // The OTLP port is probably one of the ports we already compared against, but at compile-time we don't know which one,
        // so instead of doing this check as a fallthrough on the switch, do a normal if-check
        if (catalog != null && catalog.hasOpenTelemetry()) {
            final int privateOtlpPort = getPrivateOtlpPort();
            if (privateOtlpPort == privatePort) {
                containerConfigs.put("quarkus.otel.exporter.otlp.endpoint",
                        String.format("http://%s:%s", host, publicPort));
                String otlpProtocol = config.otlpProtocol(); // If we got to this stage, config must be not null
                containerConfigs.put("quarkus.otel.exporter.otlp.protocol", otlpProtocol);
            }

        }
        return containerConfigs;
    }

    @Override
    public <S> Map<String, Function<S, String>> configProvider(Function<S, Map<String, String>> fn) {
        return createConfigProvider(
                fn,
                Prop.of("grafana.endpoint"),
                Prop.of("prometheus.endpoint"),
                Prop.of("tempo-mcp.endpoint"),
                Prop.of("otel-collector.url"),
                new Prop("quarkus.micrometer.export.otlp.url", () -> catalog != null && catalog.hasMicrometerOtlp()),
                new Prop("quarkus.otel.exporter.otlp.endpoint", () -> catalog != null && catalog.hasOpenTelemetry()),
                new Prop("quarkus.otel.exporter.otlp.protocol", () -> catalog != null && catalog.hasOpenTelemetry()));
    }

    @Override
    protected LgtmContainer defaultContainer() {
        return new LgtmContainer(isScrapingRequired(TCCL_FN)); // best we can do?
    }

    @Override
    public Map<String, String> doStart() {
        String host = container.getHost();
        Map<String, String> containerConfigs = new HashMap<>();

        containerConfigs.putAll(config(ContainerConstants.GRAFANA_PORT, host));
        containerConfigs.putAll(config(ContainerConstants.PROMETHEUS_PORT, host));
        containerConfigs.putAll(config(ContainerConstants.TEMPO_MCP_PORT, host));
        containerConfigs.putAll(config(ContainerConstants.OTEL_HTTP_EXPORTER_PORT, host));
        // Iff GRPC is the OTLP protocol, overwrite the otel-collector.url we just wrote with the correct grpc one, and set up the otlp endpoints
        if (ContainerConstants.OTEL_GRPC_PROTOCOL.equals(container.getOtlpProtocol())) {
            containerConfigs.putAll(config(ContainerConstants.OTEL_GRPC_EXPORTER_PORT, host));
        }

        return containerConfigs;
    }

    @Override
    public void logInfo() {
        // move here, so we see it logged
        for (String dashboard : container.getCustomDashboards()) {
            log.infof("Adding custom Grafana dashboard config: %s", dashboard);
        }
    }

    @Override
    public int order() {
        return DevResourceLifecycleManager.GRAFANA;
    }
}
