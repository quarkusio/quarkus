package io.quarkus.micrometer.runtime.config.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Runtime configuration for Micrometer meter registries.
 */
@SuppressWarnings("unused")
@ConfigRoot(name = "micrometer.export", phase = ConfigPhase.RUN_TIME)
public class ExportConfig {
    // @formatter:off
    /**
     * Azure Monitor registry configuration properties.
     *
     * A property source for configuration of the AzureMonitor MeterRegistry.
     *
     * Available values:
     *
     * [cols=2]
     * !===
     * h!Property=Default
     * h!Description
     *
     * !`instrumentation-key`
     * !Define the instrumentation key used to push data to Azure Insights Monitor
     *
     * !===
     *
     * Other Micrometer configuration attributes can also be specified.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> azuremonitor;

    // @formatter:off
    /**
     * Datadog MeterRegistry configuration properties.
     *
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
     * Other Micrometer configuration attributes can also be specified.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> datadog;

    // @formatter:off
    /**
     * JMX registry configuration properties.
     *
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
     *
     * A property source for configuration of the Prometheus MeterRegistry,
     * see https://micrometer.io/docs/registry/prometheus.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> prometheus;

    // @formatter:off
    /**
     * SignalFx registry configuration properties.
     *
     * A property source for configuration of the SignalFx MeterRegistry,
     * see https://micrometer.io/docs/registry/signalFx.
     *
     * Available values:
     *
     * [cols=2]
     * !===
     * h!Property=Default
     * h!Description
     *
     * !`access-token=MY_ACCESS_TOKEN`
     * !Define the access token required to push data to SignalFx
     *
     * !`source=identifier`
     * !Unique identifier for the app instance that is publishing metrics to SignalFx.
     * Defaults to the local host name.
     *
     * !`uri=https://ingest.signalfx.com`
     * !Define the the URI to ship metrics to. Use this attribute to specify
     * the location of an internal proxy, if necessary.
     *
     * !`step=1m`
     * !The interval at which metrics are sent to SignalFx Monitoring. The default is 1 minute.
     * !===
     *
     * Other Micrometer configuration attributes can also be specified.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> signalfx;

    // @formatter:off
    /**
     * Stackdriver registry configuration properties.
     *
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
     * !By default, gathered metrics will be published to Stackdriver when the MeterRegistry is enabled.
     * Use this attribute to selectively disable publication of metrics in some environments.
     *
     * !`step=1m`
     * !The interval at which metrics are sent to Stackdriver Monitoring. The default is 1 minute.
     * !===
     *
     * Other Micrometer configuration attributes can also be specified.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> stackdriver;

    // @formatter:off
    /**
     * StatsD registry configuration properties.
     * 
     * A property source for configuration of the StatsD MeterRegistry,
     * see https://micrometer.io/docs/registry/statsD.
     *
     * Available values:
     *
     * [cols=2]
     * !===
     * h!Property=Default
     * h!Description
     *
     * !`flavor=datadog`
     * !Specify the flavor of the StatsD line protocol. The original StatsD line protocol
     * specification is `etsy`. The default value is `datadog`.
     *
     * !`host=localhost`
     * !The host name of the StatsD agent.
     *
     * !`maxPacketLength=1400`
     * !Adjust the packet length to keep the payload within your network's MTU.
     *
     * !`port=8125`
     * !The port of the StatsD agent`.
     *
     * !`protocol=UDP`
     * !The protocol of the connection to the agent (UDP or TCP).
     *
     * !`publish=true`
     * !By default, gathered metrics will be published to StatsD when the MeterRegistry is enabled.
     * Use this attribute to selectively disable publication of metrics in some environments.
     *
     * !`step=1m`
     * !The interval at which metrics are sent to StatsD Monitoring. The default is 1 minute.
     * !===
     *
     * Other Micrometer configuration attributes can also be specified.
     *
     * As mentioned in the Micrometer StatsD documentation, if you want to customize the metrics
     * sink, do so by providing your own `StatsDMeterRegistry` instance using a CDI `@Produces`
     * method.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem
    Map<String, String> statsd;
}
