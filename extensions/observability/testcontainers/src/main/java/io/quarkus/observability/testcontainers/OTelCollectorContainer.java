package io.quarkus.observability.testcontainers;

import static io.quarkus.runtime.configuration.ConfigUtils.getFirstOptionalValue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.AbstractContainerConfig;
import io.quarkus.observability.common.config.ConfigUtils;
import io.quarkus.observability.common.config.JaegerConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.common.config.OTelConfig;
import io.quarkus.observability.common.config.VictoriaMetricsConfig;
import io.quarkus.observability.testcontainers.support.OTelYaml;

@SuppressWarnings("resource")
public class OTelCollectorContainer extends ObservabilityContainer<OTelCollectorContainer, OTelConfig> {
    protected static final String CONFIG_PATH = "/etc/otelcol-contrib/config.yaml";

    public static final int OTEL_GRPC_EXPORTER_PORT = 4317;
    public static final int OTEL_HTTP_EXPORTER_PORT = 4318;

    private final OTelConfig config;
    private final ModulesConfiguration root;

    public OTelCollectorContainer() {
        this(new OTelConfigImpl(), null);
    }

    public OTelCollectorContainer(OTelConfig config, ModulesConfiguration root) {
        super(config);
        this.config = config;
        this.root = root;
        withExposedPorts(OTEL_GRPC_EXPORTER_PORT, OTEL_HTTP_EXPORTER_PORT);
    }

    @Override
    protected void containerIsCreated(String containerId) {
        super.containerIsCreated(containerId);
        byte[] config;
        if (this.config.configFile().isPresent()) {
            config = getResourceAsBytes(this.config.configFile().get());
        } else {
            if (root == null) {
                config = getResourceAsBytes("otel-collector-config.yaml");
            } else {
                config = generateConfig();
            }
        }
        addFileToContainer(config, CONFIG_PATH);
    }

    private byte[] generateConfig() {
        byte[] config = getResourceAsBytes("otel-collector-config-template.yaml");
        try {
            YAMLMapper yaml = new YAMLMapper();
            yaml.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            yaml.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            OTelYaml otelYaml = yaml.readValue(config, OTelYaml.class);
            // processor, extension - add explicitly
            otelYaml.processors.put("batch", new OTelYaml.Processor());
            otelYaml.extensions.put("health_check", new OTelYaml.Extension());
            JaegerConfig jaegerConfig = root.jaeger();
            if (ConfigUtils.isEnabled(jaegerConfig)) {
                // exporter
                OTelYaml.Exporter exporter = new OTelYaml.Exporter();
                exporter.endpoint = "jaeger:14250";
                OTelYaml.Tls tls = new OTelYaml.Tls();
                tls.insecure = true;
                exporter.tls = tls;
                otelYaml.exporters.put("jaeger", exporter);
                // service
                OTelYaml.Pipeline pipeline = new OTelYaml.Pipeline();
                pipeline.receivers = List.of("otlp");
                pipeline.processors = List.of("batch");
                pipeline.exporters = List.of("jaeger");
                otelYaml.service.pipelines.put("traces", pipeline);
            }
            VictoriaMetricsConfig vmConfig = root.victoriaMetrics();
            if (ConfigUtils.isEnabled(vmConfig)) {
                // exporter
                OTelYaml.Exporter exporter = new OTelYaml.Exporter();
                exporter.endpoint = ConfigUtils.vmEndpoint(vmConfig);
                exporter.namespace = "quarkus_observability";
                otelYaml.exporters.put("prometheus", exporter);
                // service
                OTelYaml.Pipeline metrics = otelYaml.service.pipelines.get("metrics");
                List<String> exs = metrics.exporters;
                List<String> newExs = new ArrayList<>(exs);
                newExs.add("prometheus");
                metrics.exporters = newExs;
            }
            return yaml.writeValueAsBytes(otelYaml);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int getOtelGrpcExporterPort() {
        return getMappedPort(OTEL_GRPC_EXPORTER_PORT);
    }

    public int getOtelHttpExporterPort() {
        return getMappedPort(OTEL_HTTP_EXPORTER_PORT);
    }

    private static class OTelConfigImpl extends AbstractContainerConfig implements OTelConfig {
        public OTelConfigImpl() {
            super(ContainerConstants.OTEL);
        }

        @Override
        public Optional<Set<String>> networkAliases() {
            return Optional.of(Set.of("otel-collector"));
        }

        @Override
        public Optional<String> configFile() {
            return getFirstOptionalValue(List.of("quarkus.observability.otel.config-file"), String.class)
                    .or(() -> Optional.of("otel-collector-config.yaml"));
        }
    }

}
