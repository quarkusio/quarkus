package io.quarkus.devui.runtime.observability.telemetry;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.quarkus.dev.telemetry.TelemetryAttributes;
import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.devui.observability.store.metrics.MetricDistribution;
import io.quarkus.devui.observability.store.metrics.MetricSample;

/**
 * Reads a meter reading back out of the {@code metric} event a metrics backend fired, as the sample the metrics store
 * takes. This is where the generic envelope meets the typed metrics path: the dashboard needs a type, a unit, whether
 * the value is cumulative and the distribution to decide how to draw a meter, and they travel as well-known
 * attributes (see {@link TelemetryAttributes}).
 */
final class MetricEvents {

    static final String DEFAULT_SOURCE = "event";

    private static final double[] NONE = new double[0];

    private MetricEvents() {
    }

    /**
     * @return the sample, or {@code null} when the event carries no numeric {@link TelemetryAttributes#VALUE}, which is
     *         how a reading that was not finite arrives; such a reading is not charted
     */
    static MetricSample toSample(TelemetryEvent event) {
        Map<String, Object> attributes = event.attributes();
        if (!(attributes.get(TelemetryAttributes.VALUE) instanceof Number value)) {
            return null;
        }
        String type = attributes.get(TelemetryAttributes.TYPE) instanceof String t && !t.isBlank()
                ? t.toUpperCase(Locale.ROOT)
                : "GAUGE";
        boolean cumulative = Boolean.TRUE.equals(attributes.get(TelemetryAttributes.CUMULATIVE));
        String unit = attributes.get(TelemetryAttributes.UNIT) instanceof String u ? u : null;
        String source = attributes.get(TelemetryAttributes.SOURCE) instanceof String s ? s : DEFAULT_SOURCE;
        return new MetricSample(event.name(), StringValues.map(attributes.get(TelemetryAttributes.TAGS)), type,
                cumulative,
                value.doubleValue(), event.timestamp(), source, unit, distribution(attributes));
    }

    /**
     * The distribution of a timer, summary or histogram, or {@code null} for a meter with a single statistic. A
     * distribution is recognised by its total: the maximum, percentiles and buckets are each optional.
     */
    private static MetricDistribution distribution(Map<String, Object> attributes) {
        if (!(attributes.get(TelemetryAttributes.TOTAL) instanceof Number total)) {
            return null;
        }
        double max = attributes.get(TelemetryAttributes.MAX) instanceof Number m ? m.doubleValue() : Double.NaN;
        double[] ranks = NONE;
        double[] values = NONE;
        if (attributes.get(TelemetryAttributes.PERCENTILES) instanceof List<?> percentiles && !percentiles.isEmpty()) {
            ranks = new double[percentiles.size()];
            values = new double[percentiles.size()];
            for (int i = 0; i < ranks.length; i++) {
                Map<?, ?> percentile = percentiles.get(i) instanceof Map<?, ?> p ? p : Map.of();
                ranks[i] = doubleOf(percentile.get("rank"));
                values[i] = doubleOf(percentile.get("value"));
            }
        }
        return new MetricDistribution(total.doubleValue(), max, ranks, values,
                doubles(attributes.get(TelemetryAttributes.BUCKET_BOUNDARIES)),
                doubles(attributes.get(TelemetryAttributes.BUCKET_COUNTS)));
    }

    private static double[] doubles(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return NONE;
        }
        double[] values = new double[list.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = doubleOf(list.get(i));
        }
        return values;
    }

    private static double doubleOf(Object value) {
        return value instanceof Number n ? n.doubleValue() : Double.NaN;
    }
}
