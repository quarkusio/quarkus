package io.quarkus.test.micrometer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.data.MapEntry;

/**
 * Parses Prometheus exposition text format and provides a query API
 * for finding metrics by name and labels, independent of label ordering.
 * <p>
 * Supports both Prometheus text format (trailing commas in labels)
 * and OpenMetrics format (no trailing commas, optional exemplar comments).
 *
 * <pre>{@code
 * PrometheusMetrics metrics = PrometheusMetrics.parse(inputStream);
 * List<PrometheusMetric> found = metrics.find("http_client_requests_seconds_count",
 *         entry("method", "GET"), entry("status", "200"));
 * }</pre>
 */
class PrometheusMetrics {

    private final List<PrometheusMetric> metrics;

    private PrometheusMetrics(List<PrometheusMetric> metrics) {
        this.metrics = Collections.unmodifiableList(metrics);
    }

    /**
     * Parse Prometheus exposition text format from an {@link InputStream} into a queryable collection.
     */
    static PrometheusMetrics parse(InputStream input) {
        List<PrometheusMetric> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                PrometheusMetric metric = parseLine(line);
                if (metric != null) {
                    result.add(metric);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new PrometheusMetrics(result);
    }

    /**
     * Find all metrics with the given name.
     */
    List<PrometheusMetric> find(String name) {
        List<PrometheusMetric> result = new ArrayList<>();
        for (PrometheusMetric m : metrics) {
            if (m.name().equals(name)) {
                result.add(m);
            }
        }
        return result;
    }

    /**
     * Find all metrics with the given name that contain all specified label key-value pairs.
     * Extra labels on the metric are allowed (subset matching).
     *
     * @param name the metric name
     * @param entries label entries to match
     */
    @SafeVarargs
    final List<PrometheusMetric> find(String name, MapEntry<String, String>... entries) {
        List<PrometheusMetric> result = new ArrayList<>();
        for (PrometheusMetric m : metrics) {
            if (m.name().equals(name) && m.hasLabels(entries)) {
                result.add(m);
            }
        }
        return result;
    }

    /**
     * Find all metrics with the given name that have exactly the specified labels (no extra labels allowed).
     */
    @SafeVarargs
    final List<PrometheusMetric> findExact(String name, MapEntry<String, String>... entries) {
        List<PrometheusMetric> result = new ArrayList<>();
        for (PrometheusMetric m : metrics) {
            if (m.name().equals(name) && m.hasExactLabels(entries)) {
                result.add(m);
            }
        }
        return result;
    }

    /**
     * Check if any metric exists with the given name and labels.
     */
    @SafeVarargs
    final boolean hasMetric(String name, MapEntry<String, String>... entries) {
        return !find(name, entries).isEmpty();
    }

    /**
     * Find all metrics whose name contains the given substring.
     */
    List<PrometheusMetric> findByNameContaining(String substring) {
        List<PrometheusMetric> result = new ArrayList<>();
        for (PrometheusMetric m : metrics) {
            if (m.name().contains(substring)) {
                result.add(m);
            }
        }
        return result;
    }

    /**
     * Return all parsed metrics.
     */
    List<PrometheusMetric> getAll() {
        return metrics;
    }

    static PrometheusMetric parseLine(String line) {
        String name;
        Map<String, String> labels;
        double value;

        int braceOpen = line.indexOf('{');
        if (braceOpen >= 0) {
            name = line.substring(0, braceOpen);
            int braceClose = findClosingBrace(line, braceOpen + 1);
            if (braceClose < 0) {
                return null;
            }
            labels = parseLabels(line.substring(braceOpen + 1, braceClose));
            value = parseValue(line.substring(braceClose + 1).trim());
        } else {
            int space = line.indexOf(' ');
            if (space < 0) {
                return null;
            }
            name = line.substring(0, space);
            labels = Collections.emptyMap();
            value = parseValue(line.substring(space + 1).trim());
        }

        return new PrometheusMetric(name, labels, value);
    }

    private static int findClosingBrace(String line, int start) {
        boolean inQuote = false;
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == '\\' && i + 1 < line.length()) {
                    i++;
                } else if (c == '"') {
                    inQuote = false;
                }
            } else {
                if (c == '"') {
                    inQuote = true;
                } else if (c == '}') {
                    return i;
                }
            }
        }
        return -1;
    }

    private static Map<String, String> parseLabels(String labelsStr) {
        Map<String, String> labels = new LinkedHashMap<>();
        int i = 0;
        int len = labelsStr.length();
        while (i < len) {
            while (i < len && (labelsStr.charAt(i) == ',' || labelsStr.charAt(i) == ' ')) {
                i++;
            }
            if (i >= len) {
                break;
            }

            int eqPos = labelsStr.indexOf('=', i);
            if (eqPos < 0) {
                break;
            }
            String key = labelsStr.substring(i, eqPos).trim();

            i = eqPos + 1;
            if (i >= len || labelsStr.charAt(i) != '"') {
                break;
            }
            i++; // skip opening quote
            StringBuilder valueBuilder = new StringBuilder();
            while (i < len && labelsStr.charAt(i) != '"') {
                if (labelsStr.charAt(i) == '\\' && i + 1 < len) {
                    char next = labelsStr.charAt(i + 1);
                    if (next == '"' || next == '\\' || next == 'n') {
                        valueBuilder.append(next == 'n' ? '\n' : next);
                        i += 2;
                        continue;
                    }
                }
                valueBuilder.append(labelsStr.charAt(i));
                i++;
            }
            if (i < len) {
                i++; // skip closing quote
            }
            labels.put(key, valueBuilder.toString());
        }
        return labels;
    }

    private static double parseValue(String valueStr) {
        int space = valueStr.indexOf(' ');
        if (space >= 0) {
            valueStr = valueStr.substring(0, space);
        }
        // Handle exemplar comments in OpenMetrics: "1.0 # {span_id=...}"
        int hash = valueStr.indexOf('#');
        if (hash >= 0) {
            valueStr = valueStr.substring(0, hash).trim();
        }
        if ("+Inf".equals(valueStr)) {
            return Double.POSITIVE_INFINITY;
        } else if ("-Inf".equals(valueStr)) {
            return Double.NEGATIVE_INFINITY;
        }
        try {
            return Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
