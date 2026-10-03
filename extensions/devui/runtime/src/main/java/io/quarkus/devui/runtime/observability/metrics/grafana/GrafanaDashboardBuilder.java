package io.quarkus.devui.runtime.observability.metrics.grafana;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Builds a Grafana dashboard out of the cards on the Dev UI observability dashboard, so the same view can
 * be opened against a real Grafana, for example the one the LGTM Dev Service starts.
 * <p>
 * The client sends the cards it is showing, each with the plot it chose for the meter, and this turns every
 * one into the equivalent panel. The plot is not worked out again here on purpose: the Dev UI decides how a
 * meter is drawn from what was actually captured for it, and the export has to agree with what the developer
 * is looking at.
 * <p>
 * The data sources are left as dashboard inputs ({@code ${DS_PROMETHEUS}}), which is what Grafana's own
 * "export for sharing externally" does: on import Grafana asks which data source to use, so the dashboard is
 * not tied to the ids of one instance.
 */
public final class GrafanaDashboardBuilder {

    private static final String PROMETHEUS_INPUT = "DS_PROMETHEUS";
    private static final String TEMPO_INPUT = "DS_TEMPO";
    private static final int PANEL_WIDTH = 12;
    private static final int PANEL_HEIGHT = 8;
    private static final int FULL_WIDTH = 24;
    private static final int SIGNAL_HEIGHT = 10;
    private static final String LONG_TASK_TIMER = "LONG_TASK_TIMER";
    // The meter types that are gauges on the way to Prometheus, as the Micrometer sampler and the OpenTelemetry
    // exporter report them. A non-monotonic OpenTelemetry sum is drawn like a gauge, but it is not one to
    // Prometheus, which is what decides whether a unit of "1" becomes "_ratio".
    private static final Set<String> GAUGE_TYPES = Set.of("GAUGE", "DOUBLE_GAUGE", "LONG_GAUGE");

    private final PrometheusNaming naming;
    private final String serviceName;

    /**
     * @param naming how the metrics are named in Prometheus, or {@code null} when that is not known, in which
     *        case no metric card is exported
     * @param serviceName the service name this application reports its telemetry under
     */
    public GrafanaDashboardBuilder(PrometheusNaming naming, String serviceName) {
        this.naming = naming;
        this.serviceName = serviceName;
    }

    /**
     * The service name this application's traces are in Tempo under, worked out the way the OpenTelemetry
     * extension sets the {@code service.name} resource attribute: {@code quarkus.otel.service.name} when it is
     * set to something other than its default (the application name), then {@code service.name} from
     * {@code quarkus.otel.resource.attributes}, then the application name.
     */
    public static String serviceName(Config config) {
        String applicationName = config.getOptionalValue("quarkus.application.name", String.class).orElse(null);
        String configured = config.getOptionalValue("quarkus.otel.service.name", String.class)
                .filter(name -> !name.equals(applicationName == null ? "unset" : applicationName))
                .orElse(null);
        if (configured != null) {
            return configured;
        }
        String fromAttributes = null;
        for (String attribute : config.getOptionalValues("quarkus.otel.resource.attributes", String.class)
                .orElse(List.of())) {
            String[] parts = attribute.split("=", 2);
            if (parts.length == 2 && "service.name".equals(parts[0].trim())) {
                // The last one wins, as it does when the extension reads the list into a map.
                fromAttributes = parts[1].trim();
            }
        }
        if (fromAttributes != null) {
            return fromAttributes;
        }
        return applicationName == null ? "quarkus-application" : applicationName;
    }

    /**
     * @param cards the cards as the Dev UI shows them, in dashboard order
     * @param title the dashboard title
     * @return a dashboard Grafana accepts on its import screen
     */
    public JsonObject build(List<Map<String, Object>> cards, String title) {
        JsonArray panels = new JsonArray();
        boolean tempoUsed = false;
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (Map<String, Object> card : cards) {
            boolean signal = "signal".equals(string(card, "kind"));
            JsonObject panel = signal ? signalPanel(card) : metricPanel(card);
            if (panel == null) {
                // Nothing to draw for this card, and nothing to move along either: a skipped card must not
                // leave a hole in the layout.
                continue;
            }
            int width = signal ? FULL_WIDTH : PANEL_WIDTH;
            int height = signal ? SIGNAL_HEIGHT : PANEL_HEIGHT;
            if (x + width > FULL_WIDTH) {
                // The row ends, so it is the height of the row just filled that the next one starts below.
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
            tempoUsed |= signal;
            panel.put("id", panels.size() + 1)
                    .put("gridPos", new JsonObject().put("h", height).put("w", width).put("x", x).put("y", y));
            panels.add(panel);
            x += width;
            rowHeight = Math.max(rowHeight, height);
            if (x >= FULL_WIDTH) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
        }

        JsonArray inputs = new JsonArray().add(input(PROMETHEUS_INPUT, "Prometheus", "prometheus"));
        if (tempoUsed) {
            inputs.add(input(TEMPO_INPUT, "Tempo", "tempo"));
        }
        return new JsonObject()
                .put("__inputs", inputs)
                .put("title", title == null || title.isBlank() ? "Quarkus Dev UI observability" : title)
                .put("description", "Exported from the Quarkus Dev UI observability dashboard of "
                        + serviceName + ".")
                .put("tags", new JsonArray().add("quarkus").add("dev-ui"))
                .put("schemaVersion", 39)
                .put("editable", true)
                .put("time", new JsonObject().put("from", "now-15m").put("to", "now"))
                .put("refresh", "10s")
                .put("panels", panels);
    }

    private JsonObject metricPanel(Map<String, Object> card) {
        String meter = string(card, "name");
        if (naming == null || meter == null || meter.isBlank()) {
            return null;
        }
        String plot = string(card, "plot");
        String unit = string(card, "unit");
        String type = string(card, "type");
        boolean gauge = type != null && GAUGE_TYPES.contains(type);
        String base = naming.baseName(meter, unit, gauge);
        JsonArray targets = new JsonArray();
        String panelType = "timeseries";
        boolean stepped = false;

        switch (plot == null ? "value" : plot) {
            case "rate" -> targets.add(target("rate(" + naming.counterName(meter, unit) + "[$__rate_interval])",
                    "A", null));
            case "gauge" -> {
                panelType = "gauge";
                targets.add(target(base, "A", null));
            }
            case "step" -> {
                stepped = true;
                targets.add(target(LONG_TASK_TIMER.equals(type)
                        ? naming.longTaskActiveName(meter, unit)
                        : base, "A", null));
            }
            case "histogram" -> {
                panelType = "heatmap";
                targets.add(target("sum by (le) (increase(" + base + "_bucket[$__rate_interval]))", "A", "{{le}}")
                        .put("format", "heatmap"));
            }
            case "distribution" -> {
                // The mean of each interval, which is what the Dev UI card draws: the amount recorded in the
                // interval over the number of recordings in it.
                targets.add(target("rate(" + base + "_sum[$__rate_interval]) / rate(" + base
                        + "_count[$__rate_interval])", "A", "mean"));
                String max = naming.maxName(meter, unit, bool(card, "maxCaptured"), bool(card, "maxMeter"));
                if (max != null) {
                    targets.add(target(max, "B", "max"));
                }
            }
            default -> targets.add(target(base, "A", null));
        }

        JsonObject custom = new JsonObject();
        if (stepped) {
            // A meter that only ever moves in whole steps: sloping between two readings would draw values
            // that never occurred, exactly as on the Dev UI card.
            custom.put("lineInterpolation", "stepAfter");
        }
        JsonObject defaults = new JsonObject().put("custom", custom);
        String grafanaUnit = bool(card, "ratio") ? "percentunit" : grafanaUnit(unit, plot, gauge);
        if (grafanaUnit != null) {
            defaults.put("unit", grafanaUnit);
        }
        return new JsonObject()
                .put("type", panelType)
                .put("title", meter)
                .put("datasource", datasource("prometheus", PROMETHEUS_INPUT))
                .put("fieldConfig", new JsonObject().put("defaults", defaults).put("overrides", new JsonArray()))
                .put("targets", targets);
    }

    /**
     * A signal card stands for a whole Dev UI page, e.g. the captured traces. Only traces have an equivalent
     * here: they are in Tempo, queried by the service name this application reports.
     */
    private JsonObject signalPanel(Map<String, Object> card) {
        String id = string(card, "id");
        if (!"traces".equals(id)) {
            return null;
        }
        // The data source goes on the query too: without it Grafana runs the query against the dashboard's
        // default data source, which is Prometheus, and the panel stays empty.
        JsonObject target = new JsonObject()
                .put("refId", "A")
                .put("datasource", datasource("tempo", TEMPO_INPUT))
                .put("queryType", "traceql")
                .put("limit", 50)
                .put("query", "{ resource.service.name = \"" + traceQlString(serviceName) + "\" }");
        return new JsonObject()
                .put("type", "table")
                .put("title", "Traces")
                .put("datasource", datasource("tempo", TEMPO_INPUT))
                .put("targets", new JsonArray().add(target));
    }

    private static JsonObject target(String expr, String refId, String legend) {
        JsonObject target = new JsonObject()
                .put("refId", refId)
                .put("expr", expr)
                .put("datasource", datasource("prometheus", PROMETHEUS_INPUT));
        if (legend != null) {
            target.put("legendFormat", legend);
        }
        return target;
    }

    private static JsonObject datasource(String type, String inputName) {
        return new JsonObject().put("type", type).put("uid", "${" + inputName + "}");
    }

    private static JsonObject input(String name, String label, String pluginId) {
        return new JsonObject()
                .put("name", name)
                .put("label", label)
                .put("description", "")
                .put("type", "datasource")
                .put("pluginId", pluginId)
                .put("pluginName", label);
    }

    /** The contents of a double-quoted TraceQL string, which escapes quotes and backslashes. */
    private static String traceQlString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * The Grafana field unit for a captured unit, so a panel reads in the same unit as its Dev UI card.
     * {@code null} leaves the panel unitless, which is right for a count.
     */
    private static String grafanaUnit(String unit, String plot, boolean gauge) {
        if ("rate".equals(plot)) {
            return rateUnit(unit);
        }
        if (unit == null) {
            return null;
        }
        return switch (unit.trim()) {
            case "s", "seconds" -> "s";
            case "ms", "milliseconds" -> "ms";
            case "us", "microseconds" -> "µs";
            case "ns", "nanoseconds" -> "ns";
            case "By", "bytes" -> "bytes";
            case "Cel", "celsius" -> "celsius";
            case "%", "percent" -> "percent";
            // A gauge of "1" is a fraction of one, which Grafana shows as a percentage of its own accord; a
            // non-monotonic sum of "1" is a plain count, e.g. jvm.cpu.limit.
            case "1" -> gauge ? "percentunit" : null;
            default -> null;
        };
    }

    /**
     * The Grafana unit for the per-second rate of a counter in the given unit: the rate of a byte counter is a
     * throughput, and a counter of seconds grows by the fraction of each second spent on whatever it counts.
     */
    private static String rateUnit(String unit) {
        String cleaned = unit == null ? "" : unit.replaceAll("\\{[^}]*}", "").trim();
        return switch (cleaned) {
            case "", "1", "none" -> "cps";
            case "By", "bytes" -> "binBps";
            case "s", "seconds" -> "percentunit";
            // A custom unit: Grafana appends the suffix to the value as it stands.
            default -> "suffix: " + cleaned + "/s";
        };
    }

    private static String string(Map<String, Object> card, String key) {
        Object value = card.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> card, String key) {
        return Boolean.TRUE.equals(card.get(key)) || "true".equals(string(card, key));
    }
}
