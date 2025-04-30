package io.quarkus.observability.devresource.lgtm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
            "io.micrometer.prometheus.PrometheusMeterRegistry");

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
            log.warnf("Multiple Micrometer registries found - OTLP and %s, no Prometheus scrapping required.", foundRegistry);
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
    protected LgtmContainer defaultContainer() {
        return new LgtmContainer(isScrapingRequired(TCCL_FN)); // best we can do?
    }

    @Override
    public Map<String, String> doStart() {
        String host = container.getHost();
        Map<String, String> containerConfigs = new HashMap<>();

        containerConfigs.putAll(config(ContainerConstants.GRAFANA_PORT, host));
        containerConfigs.putAll(config(ContainerConstants.OTEL_HTTP_EXPORTER_PORT, host));
        // Iff GRPC is the OTLP protocol, overwrite the otel-collector.url we just wrote with the correct grpc one, and set up the otlp endpoints
        if (ContainerConstants.OTEL_GRPC_PROTOCOL.equals(container.getOtlpProtocol())) {
            containerConfigs.putAll(config(ContainerConstants.OTEL_GRPC_EXPORTER_PORT, host));
        }
        return containerConfigs;
    }

    @Override
    public int order() {
        return DevResourceLifecycleManager.GRAFANA;
    }
}
