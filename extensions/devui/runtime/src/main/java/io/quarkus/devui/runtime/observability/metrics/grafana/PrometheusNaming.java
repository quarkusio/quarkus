package io.quarkus.devui.runtime.observability.metrics.grafana;

import java.util.List;
import java.util.Map;

/**
 * Translates a meter name as the Dev UI captured it into the time series names the same meter ends up
 * with in Prometheus.
 * <p>
 * The Dev UI stores the meter's own name and unit, which is not what Prometheus stores: the names there
 * depend on how the application exports its metrics, and the two routes Quarkus offers rename differently.
 * A dashboard exported for Grafana has to use the exported names, so this is the one piece the export
 * cannot guess.
 */
public enum PrometheusNaming {

    /**
     * Micrometer's Prometheus registry, scraped from {@code /q/metrics}. Dots become underscores, the base
     * unit is appended unless the name already ends with it, counters gain {@code _total} and the
     * distribution types gain {@code _count}, {@code _sum}, {@code _max} and {@code _bucket}.
     */
    MICROMETER_PROMETHEUS,

    /**
     * OTLP, as Prometheus itself translates it when it receives OTLP (its {@code UnderscoreEscapingWithSuffixes}
     * strategy, which is the default and what the LGTM Dev Service runs with). The unit is spelled out as a
     * word ({@code ms} to {@code milliseconds}), {@code {annotations}} are dropped, a rate unit becomes
     * {@code _per_second}, and a unit that already appears as a word in the name is not repeated.
     */
    OTLP;

    /** Written by the build steps into the {@code prometheusNaming} build time data. */
    public static final String MICROMETER_PROMETHEUS_ID = "micrometer-prometheus";
    public static final String OTLP_ID = "otlp";

    // UCUM units as OpenTelemetry spells them, mapped the way Prometheus spells them out. Anything not
    // listed is used as it stands, which is what Prometheus does with a unit it does not know.
    private static final Map<String, String> UCUM_UNITS = Map.ofEntries(
            Map.entry("d", "days"),
            Map.entry("h", "hours"),
            Map.entry("min", "minutes"),
            Map.entry("s", "seconds"),
            Map.entry("ms", "milliseconds"),
            Map.entry("us", "microseconds"),
            Map.entry("ns", "nanoseconds"),
            Map.entry("By", "bytes"),
            Map.entry("KiBy", "kibibytes"),
            Map.entry("MiBy", "mebibytes"),
            Map.entry("GiBy", "gibibytes"),
            Map.entry("TiBy", "tibibytes"),
            Map.entry("KBy", "kilobytes"),
            Map.entry("MBy", "megabytes"),
            Map.entry("GBy", "gigabytes"),
            Map.entry("TBy", "terabytes"),
            Map.entry("m", "meters"),
            Map.entry("V", "volts"),
            Map.entry("A", "amperes"),
            Map.entry("J", "joules"),
            Map.entry("W", "watts"),
            Map.entry("g", "grams"),
            Map.entry("Cel", "celsius"),
            Map.entry("Hz", "hertz"),
            Map.entry("%", "percent"));

    /**
     * The Dev UI captures a Micrometer duration as {@code s} (see {@code DevUiMetricsSampler}), which is the
     * abbreviation the card headers read in, while the Prometheus registry writes the unit out in the name.
     */
    private static final Map<String, String> MICROMETER_TIME_UNITS = Map.of(
            "s", "seconds",
            "ms", "milliseconds",
            "us", "microseconds",
            "\u00b5s", "microseconds",
            "ns", "nanoseconds",
            "min", "minutes",
            "h", "hours",
            "d", "days");

    private static final Map<String, String> UCUM_PER_UNITS = Map.of(
            "s", "second",
            "m", "minute",
            "h", "hour",
            "d", "day",
            "w", "week",
            "mo", "month",
            "y", "year");

    public static PrometheusNaming fromId(String id) {
        return OTLP_ID.equals(id) ? OTLP : MICROMETER_PROMETHEUS;
    }

    /**
     * The base time series name, without the {@code _count}/{@code _sum}/{@code _bucket} suffix that the
     * distribution types add.
     *
     * @param meterName the name as captured, e.g. {@code http.server.requests}
     * @param unit the unit as captured: a Micrometer base unit, or a UCUM unit for an OTel metric
     * @param gauge whether the meter is a gauge, which is the only kind that gets {@code _ratio}
     */
    public String baseName(String meterName, String unit, boolean gauge) {
        String name = escape(meterName);
        String suffix = unitSuffix(unit, gauge);
        if (suffix.isEmpty()) {
            return name;
        }
        return switch (this) {
            // Micrometer leaves a name that already ends in its unit alone.
            case MICROMETER_PROMETHEUS -> name.endsWith("_" + suffix) ? name : name + "_" + suffix;
            // Prometheus skips a unit word that appears ANYWHERE in the name, not just at the end:
            // jvm.threads.live with unit "threads" stays jvm_threads_live.
            case OTLP -> appendUnlessPresent(name, suffix);
        };
    }

    /** The name of a counter, which carries the {@code _total} suffix on both routes. */
    public String counterName(String meterName, String unit) {
        String name = baseName(meterName, unit, false);
        return name.endsWith("_total") ? name : name + "_total";
    }

    /**
     * The series holding the maximum of a distribution, or {@code null} when there is none to draw.
     * Micrometer publishes a {@code _max} series beside the meter; over OTLP the maximum arrives as a meter
     * of its own ({@code http.server.requests.max}).
     *
     * @param maxCaptured whether a maximum was actually captured for this meter
     */
    public String maxName(String meterName, String unit, boolean maxCaptured) {
        if (!maxCaptured) {
            // A function timer tracks totals only, and a plain OTel histogram carries no maximum either.
            return null;
        }
        return switch (this) {
            case MICROMETER_PROMETHEUS -> baseName(meterName, unit, false) + "_max";
            case OTLP -> baseName(meterName + ".max", unit, true);
        };
    }

    /**
     * The series holding the number of tasks a long task timer has in flight. Micrometer publishes it as
     * {@code <name>_seconds_active_count}, whatever unit the Dev UI captured for the meter; over OTLP the
     * meter arrives split into {@code <name>.active} and {@code <name>.duration}, each an ordinary meter
     * with a card of its own, so there is nothing special to do.
     */
    public String longTaskActiveName(String meterName, String unit) {
        return switch (this) {
            case MICROMETER_PROMETHEUS -> escape(meterName) + "_seconds_active_count";
            case OTLP -> baseName(meterName, unit, true);
        };
    }

    /**
     * How Prometheus escapes a label key, which is how a Dev UI tag key reads in a query.
     */
    public static String labelName(String tagKey) {
        return escape(tagKey);
    }

    private String unitSuffix(String unit, boolean gauge) {
        String cleaned = unit == null ? "" : unit.trim();
        if (cleaned.isEmpty() || "none".equalsIgnoreCase(cleaned)) {
            return "";
        }
        if (this == MICROMETER_PROMETHEUS) {
            // A Micrometer base unit is otherwise already a word, e.g. "bytes" or "threads".
            return escape(MICROMETER_TIME_UNITS.getOrDefault(cleaned, cleaned));
        }
        // An annotation carries no unit of its own: "{requests}" is dropped, "{requests}/s" is a rate.
        String stripped = cleaned.replaceAll("\\{[^}]*}", "").trim();
        if ("1".equals(stripped)) {
            return gauge ? "ratio" : "";
        }
        int slash = stripped.indexOf('/');
        String main = slash < 0 ? stripped : stripped.substring(0, slash).trim();
        String per = slash < 0 ? "" : stripped.substring(slash + 1).trim();
        StringBuilder suffix = new StringBuilder();
        if (!main.isEmpty()) {
            suffix.append(escape(UCUM_UNITS.getOrDefault(main, main)));
        }
        if (!per.isEmpty()) {
            if (suffix.length() > 0) {
                suffix.append('_');
            }
            suffix.append("per_").append(escape(UCUM_PER_UNITS.getOrDefault(per, per)));
        }
        return suffix.toString();
    }

    private static String appendUnlessPresent(String name, String suffix) {
        String main = suffix;
        String per = "";
        int perAt = suffix.indexOf("per_");
        if (perAt >= 0) {
            main = suffix.substring(0, perAt).replaceAll("_$", "");
            per = suffix.substring(perAt);
        }
        String result = name;
        if (!main.isEmpty() && !List.of(result.split("_")).contains(main)) {
            result = result + "_" + main;
        }
        if (!per.isEmpty() && !result.endsWith(per)) {
            result = result + "_" + per;
        }
        return result;
    }

    /** Everything Prometheus does not allow in a name becomes an underscore, with runs collapsed. */
    private static String escape(String value) {
        String escaped = value.replaceAll("[^a-zA-Z0-9_:]", "_").replaceAll("_{2,}", "_");
        return escaped.replaceAll("^_+", "").replaceAll("_+$", "");
    }
}
