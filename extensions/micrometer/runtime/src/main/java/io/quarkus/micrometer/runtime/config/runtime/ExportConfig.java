package io.quarkus.micrometer.runtime.config.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Runtime configuration for Micrometer meter registries.
 * <p>
 *
 * [NOTE]
 * ====
 * Not all the dialects are supported in GraalVM native executables: we currently provide driver extensions for PostgreSQL,
 * MariaDB, Microsoft SQL Server and H2.
 * ====
 *
 * @asciidoclet
 */
@ConfigRoot(name = "micrometer.export", phase = ConfigPhase.RUN_TIME)
public class ExportConfig {

    // @formatter:off
    /**
     * Datadog MeterRegistry configuration
     * <p>
     * A property source for configuration of the Datadog MeterRegistry to push
     * metrics using the Datadog API, see https://micrometer.io/docs/registry/datadog.
     *
     * Available values:
     *
     * [cols=2]
     * !===
     * h!Property=Default
     * h!Description
     *
     * !`apiKey=YOUR_KEY`
     * !Define the key used to push data using the Datadog API
     *
     * !`publish=true`
     * !By default, gathered metrics will be published to Datadog when the MeterRegistry is enabled.
     * Use this attribute to selectively disable publication of metrics in some environments.
     *
     * !`step=1m`
     * !The interval at which metrics are sent to Datadog. The default is 1 minute.
     * !===
     *
     * Other micrometer configuration attributes can also be specified.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> datadog;

    // @formatter:off
    /**
     * JMX registry configuration properties.
     * <p>
     * A property source for configuration of the JMX MeterRegistry,
     * see https://micrometer.io/docs/registry/jmx.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> jmx;

    // @formatter:off
    /**
     * Prometheus registry configuration properties.
     * <p>
     * A property source for configuration of the Prometheus MeterRegistry,
     * see https://micrometer.io/docs/registry/prometheus
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> prometheus;

    // @formatter:off
    /**
     * Stackdriver registry configuration properties.
     * <p>
     * A property source for configuration of the Stackdriver MeterRegistry,
     * see https://micrometer.io/docs/registry/stackdriver.
     *
     * Available values:
     *
     * [cols=2]
     * !===
     * h!Property=Default
     * h!Description
     *
     * !`project-id=MY_PROJECT_ID`
     * !Define the project id used to push data to Stackdriver Monitoring
     *
     * !`publish=true`
     * !By default, gathered metrics will be published to Datadog when the MeterRegistry is enabled.
     * Use this attribute to selectively disable publication of metrics in some environments.
     *
     * !`step=1m`
     * !The interval at which metrics are sent to Stackdriver Monitoring. The default is 1 minute.
     * !===
     *
     * Other micrometer configuration attributes can also be specified.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> stackdriver;
}
