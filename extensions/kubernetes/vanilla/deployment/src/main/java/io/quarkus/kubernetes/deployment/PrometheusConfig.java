package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PrometheusConfig {

    // Note that using "prometheus.io/..." annotations is now discouraged by Prometheus,
    // but usage of said annotations is still common.
    // Removal of prometheus.io from example:
    // https://github.com/prometheus/prometheus/commit/03a9e7f72e072c6d29f422425d8acd91a957836b
    // Current example of relabeling using non-prometheus.io-based strings:
    // https://github.com/prometheus/prometheus/blob/main/documentation/examples/prometheus-kubernetes.yml#L139

    /**
     * When true (the default), emit a set of annotations to identify
     * services that should be scraped by prometheus for metrics.
     *
     * In configurations that use the Prometheus operator with ServiceMonitor,
     * annotations may not be necessary.
     */
    @ConfigItem(defaultValue = "true")
    boolean annotations;

    /**
     * Define the annotation prefix used for scrape values, this value will be used
     * as the base for other annotation name defaults. Altering the base for generated
     * annotations can make it easier to define re-labeling rules and avoid unexpected
     * knock-on effects. The default value is {@code prometheus.io}
     *
     * See Prometheus example:
     * https://github.com/prometheus/prometheus/blob/main/documentation/examples/prometheus-kubernetes.yml
     */
    @ConfigItem(defaultValue = "prometheus.io")
    String prefix;

    /**
     * Define the annotation used to indicate services that should be scraped.
     * By default, {@code /scrape} will be appended to the defined prefix.
     */
    @ConfigItem
    Optional<String> scrape;

    /**
     * Define the annotation used to indicate the path to scrape.
     * By default, {@code /path} will be appended to the defined prefix.
     */
    @ConfigItem
    Optional<String> path;

    /**
     * Define the annotation used to indicate the port to scrape.
     * By default, {@code /port} will be appended to the defined prefix.
     */
    @ConfigItem
    Optional<String> port;

    /**
     * Define the annotation used to indicate the scheme to use for scraping
     * By default, {@code /scheme} will be appended to the defined prefix.
     */
    @ConfigItem
    Optional<String> scheme;
}
