package io.quarkus.devui.runtime.observability.metrics.grafana;

import java.util.List;
import java.util.Map;

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

    private final PrometheusNaming naming;
    private final String applicationName;

    public GrafanaDashboardBuilder(PrometheusNaming naming, String applicationName) {
        this.naming = naming;
        this.applicationName = applicationName;
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
                        + applicationName + ".")
                .put("tags", new JsonArray().add("quarkus").add("dev-ui"))
                .put("schemaVersion", 39)
                .put("editable", true)
                .put("time", new JsonObject().put("from", "now-15m").put("to", "now"))
                .put("refresh", "10s")
                .put("panels", panels);
    }

    private JsonObject metricPanel(Map<String, Object> card) {
        String meter = string(card, "name");
        if (meter == null || meter.isBlank()) {
            return null;
        }
        String plot = string(card, "plot");
        String unit = string(card, "unit");
        boolean maxCaptured = bool(card, "maxCaptured");
        String base = naming.baseName(meter, unit, isGauge(plot));
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
                targets.add(target(LONG_TASK_TIMER.equals(string(card, "type"))
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
                String max = naming.maxName(meter, unit, maxCaptured);
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
        String grafanaUnit = grafanaUnit(unit, plot);
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
                .put("query", "{ resource.service.name = \"" + applicationName + "\" }");
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

    private static boolean isGauge(String plot) {
        return "gauge".equals(plot) || "value".equals(plot) || "step".equals(plot);
    }

    /**
     * The Grafana field unit for a captured unit, so a panel reads in the same unit as its Dev UI card.
     * {@code null} leaves the panel unitless, which is right for a count.
     */
    private static String grafanaUnit(String unit, String plot) {
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
            // A fraction of one, which Grafana shows as a percentage of its own accord.
            case "1" -> isGauge(plot) ? "percentunit" : null;
            default -> null;
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
