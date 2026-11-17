package io.quarkus.kubernetes.deployment;

import java.util.Locale;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.smallrye.config.WithDefault;

public interface PrometheusConfig {

    // Note that using "prometheus.io/..." annotations is now discouraged by Prometheus,
    // but usage of said annotations is still common.
    // Removal of prometheus.io from example:
    // https://github.com/prometheus/prometheus/commit/03a9e7f72e072c6d29f422425d8acd91a957836b
    // Current example of relabeling using non-prometheus.io-based strings:
    // https://github.com/prometheus/prometheus/blob/main/documentation/examples/prometheus-kubernetes.yml#L139

    /**
     * When true (the default), emit a set of annotations to identify services that should be scraped by prometheus
     * for metrics.
     * <p>
     * In configurations that use the Prometheus operator with ServiceMonitor, annotations may not be necessary.
     */
    @WithDefault("true")
    boolean annotations();

    /**
     * When true (the default), generate a {@code ServiceMonitor} resource for Prometheus Operator based setups.
     */
    @WithDefault("true")
    boolean generateServiceMonitor();

    /**
     * Define the annotation prefix used for scrape values, this value will be used as the base for other annotation
     * name defaults. Altering the base for generated annotations can make it easier to define re-labeling rules and
     * avoid unexpected knock-on effects. The default value is {@code prometheus.io}
     *
     * See Prometheus
     * <a href="https://github.com/prometheus/prometheus/blob/main/documentation/examples/prometheus-kubernetes.yml">example</a>
     */
    @WithDefault("prometheus.io")
    String prefix();

    /**
     * Define the annotation used to indicate services that should be scraped. By default, {@code /scrape} will be
     * appended to the defined prefix.
     */
    Optional<String> scrape();

    /**
     * Define the annotation used to indicate the path to scrape. By default, {@code /path} will be appended to the
     * defined prefix.
     */
    Optional<String> path();

    /**
     * Define the annotation used to indicate the port to scrape. By default, {@code /port} will be appended to the
     * defined prefix.
     */
    Optional<String> port();

    /**
     * Define the annotation used to indicate the scheme to use for scraping By default, {@code /scheme} will be
     * appended to the defined prefix.
     */
    Optional<String> scheme();

    /**
     * The scheme Prometheus uses to scrape the metrics endpoint. It is the value of the scheme annotation and the
     * scheme of the generated {@code ServiceMonitor} endpoint.
     * <p>
     * When this is not set and {@code quarkus.kubernetes.prometheus.scheme} is {@code http} or {@code https}, that value
     * is the scheme, as in earlier versions, and a warning asks to move it to this property.
     */
    @ConfigDocDefault("http")
    Optional<ScrapeScheme> scrapeScheme();

    /**
     * The schemes Prometheus can use to scrape the metrics endpoint.
     */
    enum ScrapeScheme {
        /**
         * Scrape over plain HTTP.
         */
        HTTP,
        /**
         * Scrape over HTTPS.
         */
        HTTPS;

        /**
         * @return the scheme in lower case, as the Prometheus annotation and the {@code ServiceMonitor} expect it
         */
        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * @param value a scheme name, in any case
         * @return the matching scheme, or empty if the value is neither {@code http} nor {@code https}
         */
        public static Optional<ScrapeScheme> of(String value) {
            for (ScrapeScheme scheme : values()) {
                if (scheme.value().equalsIgnoreCase(value)) {
                    return Optional.of(scheme);
                }
            }
            return Optional.empty();
        }
    }
}
