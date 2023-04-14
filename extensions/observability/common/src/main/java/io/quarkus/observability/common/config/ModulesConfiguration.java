package io.quarkus.observability.common.config;

import io.quarkus.runtime.annotations.ConfigDocSection;

public interface ModulesConfiguration {
    @ConfigDocSection
    GrafanaConfig grafana();

    @ConfigDocSection
    JaegerConfig jaeger();

    @ConfigDocSection
    OTelConfig otel();

    @ConfigDocSection
    VictoriaMetricsConfig victoriaMetrics();

    @ConfigDocSection
    VMAgentConfig vmAgent();
}
