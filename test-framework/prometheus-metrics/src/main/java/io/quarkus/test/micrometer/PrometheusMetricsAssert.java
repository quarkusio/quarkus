package io.quarkus.test.micrometer;

import java.io.InputStream;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.data.MapEntry;

/**
 * AssertJ assertion for Prometheus metrics text output.
 * <p>
 * Parses the Prometheus exposition format and provides label-order-independent assertions.
 *
 * <pre>{@code
 * assertMetrics(inputStream)
 *         .hasMetricWithLabels("http_client_requests_seconds_count",
 *                 entry("clientName", "localhost"), entry("method", "GET"), entry("status", "200"))
 *         .doesNotHaveMetricWithLabels("http_client_requests_seconds_count",
 *                 entry("uri", "/echo"));
 * }</pre>
 */
public class PrometheusMetricsAssert extends AbstractAssert<PrometheusMetricsAssert, PrometheusMetrics> {

    private PrometheusMetricsAssert(PrometheusMetrics actual) {
        super(actual, PrometheusMetricsAssert.class);
    }

    /**
     * Parse the Prometheus text body from an {@link InputStream} and return an assertion instance.
     */
    public static PrometheusMetricsAssert assertMetrics(InputStream input) {
        return new PrometheusMetricsAssert(PrometheusMetrics.parse(input));
    }

    /**
     * Assert that a metric with the given name exists.
     */
    public PrometheusMetricsAssert hasMetric(String name) {
        isNotNull();
        if (actual.find(name).isEmpty()) {
            failWithMessage("Expected metric <%s> to be present but it was not found", name);
        }
        return this;
    }

    /**
     * Assert that a metric with the given name and all specified labels exists.
     * Extra labels on the metric are allowed (subset matching).
     *
     * @param name the metric name
     * @param entries label entries to match
     */
    @SafeVarargs
    public final PrometheusMetricsAssert hasMetricWithLabels(String name, MapEntry<String, String>... entries) {
        isNotNull();
        List<PrometheusMetric> found = actual.find(name);
        if (found.isEmpty()) {
            failWithMessage("Expected metric <%s> to be present but no metric with that name was found", name);
        } else if (actual.find(name, entries).isEmpty()) {
            failWithMessage(
                    "Expected metric <%s> with labels %s to be present but only found:%n%s",
                    name, formatLabels(entries), formatMetrics(found));
        }
        return this;
    }

    /**
     * Assert that a metric with the given name exists and has exactly the specified labels (no extra labels allowed).
     *
     * @param name the metric name
     * @param entries label entries that must match exactly
     */
    @SafeVarargs
    public final PrometheusMetricsAssert hasMetricWithExactLabels(String name, MapEntry<String, String>... entries) {
        isNotNull();
        List<PrometheusMetric> found = actual.find(name);
        if (found.isEmpty()) {
            failWithMessage("Expected metric <%s> to be present but no metric with that name was found", name);
        } else if (actual.findExact(name, entries).isEmpty()) {
            failWithMessage(
                    "Expected metric <%s> with exactly labels %s to be present but only found:%n%s",
                    name, formatLabels(entries), formatMetrics(found));
        }
        return this;
    }

    /**
     * Assert that a metric with the given name, labels, and exact value exists.
     *
     * @param name the metric name
     * @param value the expected value
     * @param entries label entries to match
     */
    @SafeVarargs
    public final PrometheusMetricsAssert hasMetricWithLabelsAndValue(String name, double value,
            MapEntry<String, String>... entries) {
        isNotNull();
        List<PrometheusMetric> found = actual.find(name, entries);
        if (found.isEmpty()) {
            List<PrometheusMetric> byName = actual.find(name);
            if (byName.isEmpty()) {
                failWithMessage("Expected metric <%s> to be present but no metric with that name was found", name);
            } else {
                failWithMessage(
                        "Expected metric <%s> with labels %s to be present but only found:%n%s",
                        name, formatLabels(entries), formatMetrics(byName));
            }
        } else {
            boolean matched = false;
            for (PrometheusMetric m : found) {
                if (Double.compare(m.value(), value) == 0) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                failWithMessage(
                        "Expected metric <%s> with labels %s to have value <%s> but found:%n%s",
                        name, formatLabels(entries), value, formatMetrics(found));
            }
        }
        return this;
    }

    /**
     * Assert that a metric with the given name exists, has exactly the specified labels (no extra labels allowed),
     * and has the specified value.
     *
     * @param name the metric name
     * @param value the expected value
     * @param entries label entries that must match exactly
     */
    @SafeVarargs
    public final PrometheusMetricsAssert hasMetricWithExactLabelsAndValue(String name, double value,
            MapEntry<String, String>... entries) {
        isNotNull();
        List<PrometheusMetric> found = actual.findExact(name, entries);
        if (found.isEmpty()) {
            List<PrometheusMetric> byName = actual.find(name);
            if (byName.isEmpty()) {
                failWithMessage("Expected metric <%s> to be present but no metric with that name was found", name);
            } else {
                failWithMessage(
                        "Expected metric <%s> with exactly labels %s to be present but only found:%n%s",
                        name, formatLabels(entries), formatMetrics(byName));
            }
        } else {
            boolean matched = false;
            for (PrometheusMetric m : found) {
                if (Double.compare(m.value(), value) == 0) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                failWithMessage(
                        "Expected metric <%s> with exactly labels %s to have value <%s> but found:%n%s",
                        name, formatLabels(entries), value, formatMetrics(found));
            }
        }
        return this;
    }

    /**
     * Assert that a metric with the given name and labels exists with a value
     * greater than or equal to the specified minimum.
     *
     * @param name the metric name
     * @param minValue the minimum expected value (inclusive)
     * @param entries label entries to match
     */
    @SafeVarargs
    public final PrometheusMetricsAssert hasMetricWithLabelsAndValueGreaterThanOrEqualTo(String name, double minValue,
            MapEntry<String, String>... entries) {
        isNotNull();
        List<PrometheusMetric> found = actual.find(name, entries);
        if (found.isEmpty()) {
            List<PrometheusMetric> byName = actual.find(name);
            if (byName.isEmpty()) {
                failWithMessage("Expected metric <%s> to be present but no metric with that name was found", name);
            } else {
                failWithMessage(
                        "Expected metric <%s> with labels %s to be present but only found:%n%s",
                        name, formatLabels(entries), formatMetrics(byName));
            }
        } else {
            boolean matched = false;
            for (PrometheusMetric m : found) {
                if (m.value() >= minValue) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                failWithMessage(
                        "Expected metric <%s> with labels %s to have value >= <%s> but found:%n%s",
                        name, formatLabels(entries), minValue, formatMetrics(found));
            }
        }
        return this;
    }

    /**
     * Assert that at least one metric exists whose name contains the given substring.
     */
    public PrometheusMetricsAssert hasMetricNameContaining(String substring) {
        isNotNull();
        if (actual.findByNameContaining(substring).isEmpty()) {
            failWithMessage("Expected a metric whose name contains <%s> but none was found", substring);
        }
        return this;
    }

    /**
     * Assert that no metric exists whose name contains the given substring.
     */
    public PrometheusMetricsAssert doesNotHaveMetricNameContaining(String substring) {
        isNotNull();
        List<PrometheusMetric> found = actual.findByNameContaining(substring);
        if (!found.isEmpty()) {
            failWithMessage(
                    "Expected no metric whose name contains <%s> but found:%n%s",
                    substring, formatMetrics(found));
        }
        return this;
    }

    /**
     * Assert that no metric with the given name exists.
     */
    public PrometheusMetricsAssert doesNotHaveMetric(String name) {
        isNotNull();
        List<PrometheusMetric> found = actual.find(name);
        if (!found.isEmpty()) {
            failWithMessage(
                    "Expected no metric <%s> but found:%n%s",
                    name, formatMetrics(found));
        }
        return this;
    }

    /**
     * Assert that no metric with the given name and labels exists.
     *
     * @param name the metric name
     * @param entries label entries to match
     */
    @SafeVarargs
    public final PrometheusMetricsAssert doesNotHaveMetricWithLabels(String name, MapEntry<String, String>... entries) {
        isNotNull();
        List<PrometheusMetric> found = actual.find(name, entries);
        if (!found.isEmpty()) {
            failWithMessage(
                    "Expected no metric <%s> with labels %s but found:%n%s",
                    name, formatLabels(entries), formatMetrics(found));
        }
        return this;
    }

    private static String formatLabels(MapEntry<String, String>[] entries) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < entries.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entries[i].key).append("=\"").append(entries[i].value).append('"');
        }
        return sb.append('}').toString();
    }

    private static String formatMetrics(List<PrometheusMetric> metrics) {
        StringBuilder sb = new StringBuilder();
        for (PrometheusMetric m : metrics) {
            sb.append("  ").append(m).append('\n');
        }
        return sb.toString();
    }
}
