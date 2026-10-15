package io.quarkus.test.micrometer;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.data.MapEntry;

/**
 * A single parsed Prometheus metric line.
 * <p>
 * Example input: {@code http_client_requests_seconds_count{method="GET",status="200"} 1.0}
 */
record PrometheusMetric(String name, Map<String, String> labels, double value) {

    PrometheusMetric(String name, Map<String, String> labels, double value) {
        this.name = name;
        this.labels = Collections.unmodifiableMap(labels);
        this.value = value;
    }

    public String getLabel(String key) {
        return labels.get(key);
    }

    @SafeVarargs
    public final boolean hasLabels(MapEntry<String, String>... entries) {
        for (MapEntry<String, String> entry : entries) {
            if (!entry.value.equals(labels.get(entry.key))) {
                return false;
            }
        }
        return true;
    }

    @SafeVarargs
    public final boolean hasExactLabels(MapEntry<String, String>... entries) {
        if (labels.size() != entries.length) {
            return false;
        }
        return hasLabels(entries);
    }

    @Override
    public String toString() {
        if (labels.isEmpty()) {
            return name + " " + value;
        }
        StringBuilder sb = new StringBuilder(name).append('{');
        boolean first = true;
        for (Map.Entry<String, String> e : labels.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append(e.getKey()).append("=\"").append(e.getValue()).append('"');
            first = false;
        }
        sb.append("} ").append(value);
        return sb.toString();
    }
}
