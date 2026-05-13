package io.quarkus.test.micrometer;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class PrometheusMetricsTest {

    private static final String SAMPLE_OUTPUT = """
            # HELP http_client_requests_seconds_count
            # TYPE http_client_requests_seconds_count counter
            http_client_requests_seconds_count{clientName="localhost",method="GET",outcome="SUCCESS",status="200",uri="root"} 2.0
            http_client_requests_seconds_count{clientName="example.com",method="POST",outcome="CLIENT_ERROR",status="404",uri="/api"} 1.0
            # HELP http_server_requests_seconds_count
            # TYPE http_server_requests_seconds_count counter
            http_server_requests_seconds_count{method="GET",status="200",uri="/hello"} 5.0
            simple_metric 42.0
            """;

    private static InputStream toStream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parseSkipsCommentsAndBlanks() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream(SAMPLE_OUTPUT));
        assertThat(metrics.getAll()).hasSize(4);
    }

    @Test
    void findByName() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream(SAMPLE_OUTPUT));
        List<PrometheusMetric> found = metrics.find("http_client_requests_seconds_count");
        assertThat(found).hasSize(2);
    }

    @Test
    void findByNameAndLabelsSubsetMatch() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream(SAMPLE_OUTPUT));
        List<PrometheusMetric> found = metrics.find("http_client_requests_seconds_count",
                entry("method", "GET"), entry("status", "200"));
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getLabel("clientName")).isEqualTo("localhost");
    }

    @Test
    void findByNameAndLabelsNoMatch() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream(SAMPLE_OUTPUT));
        List<PrometheusMetric> found = metrics.find("http_client_requests_seconds_count",
                entry("method", "DELETE"));
        assertThat(found).isEmpty();
    }

    @Test
    void hasMetric() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream(SAMPLE_OUTPUT));
        assertThat(metrics.hasMetric("simple_metric")).isTrue();
        assertThat(metrics.hasMetric("nonexistent_metric")).isFalse();
    }

    @Test
    void parseMetricWithoutLabels() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream("simple_metric 42.0\n"));
        List<PrometheusMetric> found = metrics.find("simple_metric");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).labels()).isEmpty();
        assertThat(found.get(0).value()).isEqualTo(42.0);
    }

    @Test
    void parseValueWithTimestamp() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("my_metric{label=\"val\"} 3.14 1625000000000\n"));
        assertThat(metrics.find("my_metric").get(0).value()).isEqualTo(3.14);
    }

    @Test
    void parseTrailingCommaInLabels() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("my_metric{a=\"1\",b=\"2\",} 1.0\n"));
        PrometheusMetric m = metrics.find("my_metric").get(0);
        assertThat(m.getLabel("a")).isEqualTo("1");
        assertThat(m.getLabel("b")).isEqualTo("2");
        assertThat(m.labels()).hasSize(2);
    }

    @Test
    void parseEscapedQuotesInLabelValue() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("my_metric{path=\"/say\\\"hello\\\"\"} 1.0\n"));
        assertThat(metrics.find("my_metric").get(0).getLabel("path"))
                .isEqualTo("/say\"hello\"");
    }

    @Test
    void parseEscapedNewlineInLabelValue() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("my_metric{msg=\"line1\\nline2\"} 1.0\n"));
        assertThat(metrics.find("my_metric").get(0).getLabel("msg"))
                .isEqualTo("line1\nline2");
    }

    @Test
    void parseLabelValueContainingBraces() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("http_server_requests_seconds_count{uri=\"/message/item/{id}\"} 1.0\n"));
        PrometheusMetric m = metrics.find("http_server_requests_seconds_count").get(0);
        assertThat(m.getLabel("uri")).isEqualTo("/message/item/{id}");
        assertThat(m.value()).isEqualTo(1.0);
    }

    @Test
    void hasLabelsMatchesSubset() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("m{a=\"1\",b=\"2\",c=\"3\"} 1.0\n"));
        PrometheusMetric m = metrics.find("m").get(0);
        assertThat(m.hasLabels(entry("a", "1"), entry("c", "3"))).isTrue();
        assertThat(m.hasLabels(entry("a", "1"), entry("c", "wrong"))).isFalse();
    }

    @Test
    void hasExactLabelsMatchesAll() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("m{a=\"1\",b=\"2\",c=\"3\"} 1.0\n"));
        PrometheusMetric m = metrics.find("m").get(0);
        assertThat(m.hasExactLabels(entry("a", "1"), entry("b", "2"), entry("c", "3"))).isTrue();
        assertThat(m.hasExactLabels(entry("a", "1"), entry("c", "3"))).isFalse();
        assertThat(m.hasExactLabels(entry("a", "1"), entry("b", "2"), entry("c", "3"), entry("d", "4"))).isFalse();
    }

    @Test
    void toStringWithLabels() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("my_metric{a=\"1\",b=\"2\"} 3.0\n"));
        String str = metrics.find("my_metric").get(0).toString();
        assertThat(str).contains("my_metric{").contains("a=\"1\"").contains("b=\"2\"").contains("3.0");
    }

    @Test
    void toStringWithoutLabels() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream("simple 7.0\n"));
        assertThat(metrics.find("simple").get(0).toString()).isEqualTo("simple 7.0");
    }

    @Test
    void parsePlusInfValue() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(
                toStream("my_bucket{le=\"+Inf\"} +Inf\n"));
        PrometheusMetric m = metrics.find("my_bucket").get(0);
        assertThat(m.value()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(m.getLabel("le")).isEqualTo("+Inf");
    }

    @Test
    void parseMinusInfValue() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream("gauge_metric -Inf\n"));
        assertThat(metrics.find("gauge_metric").get(0).value()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    void parseNaNValue() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream("stale_metric NaN\n"));
        assertThat(metrics.find("stale_metric").get(0).value()).isNaN();
    }

    // AssertJ assertion tests

    @Test
    void assertHasMetric() {
        assertMetrics(toStream(SAMPLE_OUTPUT)).hasMetric("simple_metric");
    }

    @Test
    void assertHasMetricFails() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT)).hasMetric("nonexistent"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void assertHasMetricWithLabels() {
        assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithLabels("http_client_requests_seconds_count",
                        entry("clientName", "localhost"), entry("method", "GET"));
    }

    @Test
    void assertHasMetricWithLabelsFailsNoName() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithLabels("nonexistent", entry("method", "GET")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void assertHasMetricWithLabelsFailsWrongLabels() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithLabels("http_client_requests_seconds_count",
                        entry("method", "DELETE")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("method=\"DELETE\"");
    }

    @Test
    void assertHasMetricWithExactLabels() {
        assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithExactLabels("http_client_requests_seconds_count",
                        entry("clientName", "localhost"), entry("method", "GET"),
                        entry("outcome", "SUCCESS"), entry("status", "200"), entry("uri", "root"));
    }

    @Test
    void assertHasMetricWithExactLabelsFailsSubset() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithExactLabels("http_client_requests_seconds_count",
                        entry("clientName", "localhost"), entry("method", "GET")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("exactly labels");
    }

    @Test
    void assertDoesNotHaveMetricWithLabels() {
        assertMetrics(toStream(SAMPLE_OUTPUT))
                .doesNotHaveMetricWithLabels("http_client_requests_seconds_count",
                        entry("method", "DELETE"));
    }

    @Test
    void assertDoesNotHaveMetricWithLabelsFails() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .doesNotHaveMetricWithLabels("http_client_requests_seconds_count",
                        entry("method", "GET")))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void assertDoesNotHaveMetric() {
        assertMetrics(toStream(SAMPLE_OUTPUT)).doesNotHaveMetric("nonexistent");
    }

    @Test
    void assertDoesNotHaveMetricFails() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT)).doesNotHaveMetric("simple_metric"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void assertHasMetricWithLabelsAndValue() {
        assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithLabelsAndValue("http_client_requests_seconds_count", 2.0,
                        entry("clientName", "localhost"), entry("method", "GET"));
    }

    @Test
    void assertHasMetricWithLabelsAndValueFailsWrongValue() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithLabelsAndValue("http_client_requests_seconds_count", 99.0,
                        entry("clientName", "localhost"), entry("method", "GET")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("99.0");
    }

    @Test
    void assertHasMetricWithLabelsAndValueFailsNoMetric() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithLabelsAndValue("nonexistent", 1.0, entry("k", "v")))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void assertHasMetricWithExactLabelsAndValue() {
        assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithExactLabelsAndValue("http_server_requests_seconds_count", 5.0,
                        entry("method", "GET"), entry("status", "200"), entry("uri", "/hello"));
    }

    @Test
    void assertHasMetricWithExactLabelsAndValueFailsSubset() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithExactLabelsAndValue("http_server_requests_seconds_count", 5.0,
                        entry("method", "GET")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("exactly labels");
    }

    @Test
    void assertHasMetricWithExactLabelsAndValueFailsWrongValue() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithExactLabelsAndValue("http_server_requests_seconds_count", 99.0,
                        entry("method", "GET"), entry("status", "200"), entry("uri", "/hello")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("99.0");
    }

    @Test
    void assertHasMetricWithLabelsAndValueGreaterThanOrEqualTo() {
        assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithLabelsAndValueGreaterThanOrEqualTo(
                        "http_server_requests_seconds_count", 5.0, entry("method", "GET"))
                .hasMetricWithLabelsAndValueGreaterThanOrEqualTo(
                        "http_server_requests_seconds_count", 3.0, entry("method", "GET"));
    }

    @Test
    void assertHasMetricWithLabelsAndValueGreaterThanOrEqualToFails() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetricWithLabelsAndValueGreaterThanOrEqualTo(
                        "http_server_requests_seconds_count", 100.0, entry("method", "GET")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(">= <100.0>");
    }

    @Test
    void findByNameContaining() {
        PrometheusMetrics metrics = PrometheusMetrics.parse(toStream(SAMPLE_OUTPUT));
        assertThat(metrics.findByNameContaining("http_client")).hasSize(2);
        assertThat(metrics.findByNameContaining("http_server")).hasSize(1);
        assertThat(metrics.findByNameContaining("nonexistent")).isEmpty();
    }

    @Test
    void assertHasMetricNameContaining() {
        assertMetrics(toStream(SAMPLE_OUTPUT)).hasMetricNameContaining("http_client");
    }

    @Test
    void assertHasMetricNameContainingFails() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT)).hasMetricNameContaining("nonexistent"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void assertDoesNotHaveMetricNameContaining() {
        assertMetrics(toStream(SAMPLE_OUTPUT)).doesNotHaveMetricNameContaining("nonexistent");
    }

    @Test
    void assertDoesNotHaveMetricNameContainingFails() {
        assertThatThrownBy(() -> assertMetrics(toStream(SAMPLE_OUTPUT)).doesNotHaveMetricNameContaining("http_client"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void assertChainingWorks() {
        assertMetrics(toStream(SAMPLE_OUTPUT))
                .hasMetric("simple_metric")
                .hasMetricWithLabels("http_client_requests_seconds_count",
                        entry("clientName", "localhost"))
                .hasMetricWithLabelsAndValue("simple_metric", 42.0)
                .doesNotHaveMetric("nonexistent")
                .doesNotHaveMetricWithLabels("http_client_requests_seconds_count",
                        entry("clientName", "unknown"));
    }
}
