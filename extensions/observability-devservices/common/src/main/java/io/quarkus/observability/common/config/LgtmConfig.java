package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface LgtmConfig extends GrafanaConfig {

    /**
     * The name of the Grafana LGTM Docker image.
     */
    @WithDefault(ContainerConstants.LGTM)
    String imageName();

    /**
     * The Docker network aliases.
     */
    @WithDefault("lgtm,lgtm.testcontainer.docker")
    Optional<Set<String>> networkAliases();

    /**
     * The label of the container.
     */
    @WithDefault("quarkus-dev-service-lgtm")
    String label();

    /**
     * The value of the {@code quarkus-dev-service} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     */
    @WithDefault("lgtm")
    String serviceName();

    // this is duplicated for a reason - not all collectors speak grpc,
    // which is the default in OTEL exporter,
    // where we want http as a default with LGTM

    /**
     * Set of components to log.
     * Comma separated set of components whose container log we want to output.
     *
     * @return set of components to log
     */
    Optional<Set<LgtmComponent>> logging();

    /**
     * The LGTM's OTLP protocol.
     */
    @WithDefault(ContainerConstants.OTEL_HTTP_PROTOCOL)
    String otlpProtocol();

    /**
     * The (Prometheus) scraping interval, in seconds.
     */
    @WithDefault(ContainerConstants.SCRAPING_INTERVAL + "")
    int scrapingInterval();

    /**
     * Do we force scraping.
     */
    Optional<Boolean> forceScraping();

    /**
     * A way to override `quarkus.otel.metric.export.interval` property's default value.
     */
    @OverrideProperty("quarkus.otel.metric.export.interval")
    @WithDefault(ContainerConstants.OTEL_METRIC_EXPORT_INTERVAL)
    @ConfigDocIgnore
    String otelMetricExportInterval();

    /**
     * A way to override `quarkus.otel.bsp.schedule.delay` property's default value.
     */
    @OverrideProperty("quarkus.otel.bsp.schedule.delay")
    @WithDefault(ContainerConstants.OTEL_BSP_SCHEDULE_DELAY)
    @ConfigDocIgnore
    String otelBspScheduleDelay();

    /**
     * A way to override `quarkus.otel.metric.export.interval` property's default value.
     */
    @OverrideProperty("quarkus.otel.blrp.schedule.delay")
    @WithDefault(ContainerConstants.OTEL_BLRP_SCHEDULE_DELAY)
    @ConfigDocIgnore
    String otelBlrpScheduleDelay();
}
