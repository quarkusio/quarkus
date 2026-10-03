package io.quarkus.devui.runtime.observability.metrics.grafana;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GrafanaDashboardBuilderTest {

    private static final GrafanaDashboardBuilder OTLP = new GrafanaDashboardBuilder(PrometheusNaming.OTLP, "orders");
    private static final GrafanaDashboardBuilder MICROMETER = new GrafanaDashboardBuilder(
            PrometheusNaming.MICROMETER_PROMETHEUS, "orders");

    private static Map<String, Object> metric(String name, String plot, String unit) {
        return Map.of("kind", "metric", "name", name, "plot", plot, "unit", unit);
    }

    private static JsonObject panel(JsonObject dashboard, int index) {
        return dashboard.getJsonArray("panels").getJsonObject(index);
    }

    private static String expr(JsonObject panel, int target) {
        return panel.getJsonArray("targets").getJsonObject(target).getString("expr");
    }

    private static String unit(JsonObject panel) {
        return panel.getJsonObject("fieldConfig").getJsonObject("defaults").getString("unit");
    }

    private static Config config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .build();
    }

    @Test
    public void aCounterCardBecomesARatePanel() {
        JsonObject dashboard = MICROMETER.build(List.of(metric("probe.orders", "rate", "")), "Orders");

        JsonObject panel = panel(dashboard, 0);
        assertThat(dashboard.getString("title")).isEqualTo("Orders");
        assertThat(panel.getString("type")).isEqualTo("timeseries");
        assertThat(panel.getString("title")).isEqualTo("probe.orders");
        assertThat(expr(panel, 0)).isEqualTo("rate(probe_orders_total[$__rate_interval])");
    }

    @Test
    public void aGaugeCardBecomesAGaugePanelInItsOwnUnit() {
        JsonObject dashboard = OTLP.build(List.of(metric("jvm.memory.used", "gauge", "By")), null);

        JsonObject panel = panel(dashboard, 0);
        assertThat(panel.getString("type")).isEqualTo("gauge");
        assertThat(expr(panel, 0)).isEqualTo("jvm_memory_used_bytes");
        assertThat(panel.getJsonObject("fieldConfig").getJsonObject("defaults").getString("unit"))
                .isEqualTo("bytes");
    }

    @Test
    public void aSteppedCardKeepsItsStepsInGrafana() {
        JsonObject panel = panel(MICROMETER.build(List.of(metric("probe.long.task", "step", "tasks")), null), 0);

        assertThat(panel.getJsonObject("fieldConfig").getJsonObject("defaults").getJsonObject("custom")
                .getString("lineInterpolation")).isEqualTo("stepAfter");
    }

    @Test
    public void aLongTaskTimerQueriesItsActiveCount() {
        Map<String, Object> card = Map.of("kind", "metric", "name", "probe.long.task", "plot", "step",
                "unit", "tasks", "type", "LONG_TASK_TIMER");

        assertThat(expr(panel(MICROMETER.build(List.of(card), null), 0), 0))
                .isEqualTo("probe_long_task_seconds_active_count");
    }

    @Test
    public void aTimerIsQueriedBySecondsAlthoughItIsCapturedAsS() {
        Map<String, Object> card = Map.of("kind", "metric", "name", "probe.work", "plot", "distribution",
                "unit", "s", "type", "TIMER", "maxCaptured", true);

        JsonObject panel = panel(MICROMETER.build(List.of(card), null), 0);
        assertThat(expr(panel, 0))
                .isEqualTo("rate(probe_work_seconds_sum[$__rate_interval]) / rate(probe_work_seconds_count[$__rate_interval])");
        assertThat(expr(panel, 1)).isEqualTo("probe_work_seconds_max");
    }

    @Test
    public void aDistributionCardDrawsTheMeanOfEachIntervalAndTheMaximum() {
        Map<String, Object> card = Map.of("kind", "metric", "name", "probe.work", "plot", "distribution",
                "unit", "seconds", "maxCaptured", true);
        JsonObject panel = panel(MICROMETER.build(List.of(card), null), 0);

        assertThat(expr(panel, 0))
                .isEqualTo("rate(probe_work_seconds_sum[$__rate_interval]) / rate(probe_work_seconds_count[$__rate_interval])");
        assertThat(panel.getJsonArray("targets").getJsonObject(0).getString("legendFormat")).isEqualTo("mean");
        assertThat(expr(panel, 1)).isEqualTo("probe_work_seconds_max");
    }

    @Test
    public void aDistributionWithoutACapturedMaximumDrawsOnlyTheMean() {
        // A function timer tracks totals only: Micrometer publishes no _max series for it.
        JsonObject panel = panel(MICROMETER.build(List.of(metric("probe.function.timer", "distribution", "s")),
                null), 0);

        assertThat(panel.getJsonArray("targets")).hasSize(1);
    }

    @Test
    public void aDistributionOverOtlpOnlyDrawsTheMaximumWhenItsMeterIsKnown() {
        // A native OTel histogram records a maximum, so the page reports one captured, but Prometheus holds
        // no series of it: only a Micrometer timer bridged to OpenTelemetry publishes a .max meter.
        Map<String, Object> nativeHistogram = Map.of("kind", "metric", "name", "probe.native.duration",
                "plot", "distribution", "unit", "s", "type", "HISTOGRAM", "maxCaptured", true, "maxMeter", false);
        assertThat(panel(OTLP.build(List.of(nativeHistogram), null), 0).getJsonArray("targets")).hasSize(1);

        Map<String, Object> bridgedTimer = Map.of("kind", "metric", "name", "http.server.requests",
                "plot", "distribution", "unit", "ms", "type", "HISTOGRAM", "maxCaptured", true, "maxMeter", true);
        JsonObject panel = panel(OTLP.build(List.of(bridgedTimer), null), 0);
        assertThat(panel.getJsonArray("targets")).hasSize(2);
        assertThat(expr(panel, 1)).isEqualTo("http_server_requests_max_milliseconds");
    }

    @Test
    public void micrometerDrawsTheMaximumItCapturedWhateverMetersShareItsName() {
        // A meter named <name>.max says nothing about Micrometer's own _max series.
        Map<String, Object> functionTimer = Map.of("kind", "metric", "name", "probe.function.timer",
                "plot", "distribution", "unit", "s", "type", "TIMER", "maxCaptured", false, "maxMeter", true);

        assertThat(panel(MICROMETER.build(List.of(functionTimer), null), 0).getJsonArray("targets")).hasSize(1);
    }

    @Test
    public void onlyAnOtlpGaugeWithUnitOneIsARatio() {
        // jvm.cpu.limit is a non-monotonic sum: drawn as a current value, but not a gauge to Prometheus.
        Map<String, Object> nonMonotonicSum = Map.of("kind", "metric", "name", "jvm.cpu.limit", "plot", "value",
                "unit", "1", "type", "DOUBLE_SUM");
        JsonObject limit = panel(OTLP.build(List.of(nonMonotonicSum), null), 0);
        assertThat(expr(limit, 0)).isEqualTo("jvm_cpu_limit");
        // A count of processors, not a fraction.
        assertThat(unit(limit)).isNull();

        Map<String, Object> gauge = Map.of("kind", "metric", "name", "jvm.cpu.recent_utilization",
                "plot", "gauge", "unit", "1", "type", "DOUBLE_GAUGE");
        JsonObject panel = panel(OTLP.build(List.of(gauge), null), 0);
        assertThat(expr(panel, 0)).isEqualTo("jvm_cpu_recent_utilization_ratio");
        assertThat(unit(panel)).isEqualTo("percentunit");
    }

    @Test
    public void aRatioMeterIsQueriedByItsOwnNameAndShownAsAPercentage() {
        // Micrometer gives system.cpu.usage no unit; the page only calls it a ratio to format it.
        Map<String, Object> card = Map.of("kind", "metric", "name", "system.cpu.usage", "plot", "gauge",
                "unit", "", "type", "GAUGE", "ratio", true);

        JsonObject panel = panel(MICROMETER.build(List.of(card), null), 0);
        assertThat(expr(panel, 0)).isEqualTo("system_cpu_usage");
        assertThat(unit(panel)).isEqualTo("percentunit");
    }

    @Test
    public void aRateIsShownPerSecond() {
        assertThat(unit(panel(MICROMETER.build(List.of(metric("probe.payload.received", "rate", "bytes")), null), 0)))
                .isEqualTo("binBps");
        assertThat(unit(panel(OTLP.build(List.of(metric("probe.native.received", "rate", "By")), null), 0)))
                .isEqualTo("binBps");
        // Seconds counted per second is the fraction of the time spent, e.g. CPU time.
        assertThat(unit(panel(MICROMETER.build(List.of(metric("process.cpu.time", "rate", "seconds")), null), 0)))
                .isEqualTo("percentunit");
        assertThat(unit(panel(MICROMETER.build(List.of(metric("probe.orders", "rate", "")), null), 0)))
                .isEqualTo("cps");
        assertThat(unit(panel(OTLP.build(List.of(metric("probe.native.orders", "rate", "{order}")), null), 0)))
                .isEqualTo("cps");
        assertThat(unit(panel(MICROMETER.build(List.of(metric("probe.items", "rate", "items")), null), 0)))
                .isEqualTo("suffix: items/s");
    }

    @Test
    public void withoutAKnownNamingOnlyTheSignalCardsAreExported() {
        JsonArray panels = new GrafanaDashboardBuilder(PrometheusNaming.fromId(null), "orders").build(List.of(
                metric("jvm.memory.used", "gauge", "By"),
                Map.of("kind", "signal", "id", "traces", "title", "Traces")), null).getJsonArray("panels");

        assertThat(panels).hasSize(1);
        assertThat(panels.getJsonObject(0).getString("title")).isEqualTo("Traces");
    }

    @Test
    public void theTracesQueryEscapesTheServiceName() {
        JsonObject panel = panel(new GrafanaDashboardBuilder(PrometheusNaming.OTLP, "say \"hi\" \\o/")
                .build(List.of(Map.of("kind", "signal", "id", "traces")), null), 0);

        assertThat(panel.getJsonArray("targets").getJsonObject(0).getString("query"))
                .isEqualTo("{ resource.service.name = \"say \\\"hi\\\" \\\\o/\" }");
    }

    @Test
    public void theServiceNameIsResolvedAsOpenTelemetryResolvesIt() {
        assertThat(GrafanaDashboardBuilder.serviceName(config(Map.of("quarkus.application.name", "orders"))))
                .isEqualTo("orders");
        // quarkus.otel.service.name defaults to the application name, which does not override the attributes.
        assertThat(GrafanaDashboardBuilder.serviceName(config(Map.of(
                "quarkus.application.name", "orders",
                "quarkus.otel.service.name", "orders",
                "quarkus.otel.resource.attributes", "deployment.environment=dev,service.name=orders-svc"))))
                .isEqualTo("orders-svc");
        assertThat(GrafanaDashboardBuilder.serviceName(config(Map.of(
                "quarkus.application.name", "orders",
                "quarkus.otel.service.name", "orders-api",
                "quarkus.otel.resource.attributes", "service.name=orders-svc"))))
                .isEqualTo("orders-api");
    }

    @Test
    public void aHistogramCardBecomesAHeatmapOverTheBuckets() {
        JsonObject panel = panel(MICROMETER.build(List.of(metric("probe.work.histogram", "histogram", "seconds")),
                null), 0);

        assertThat(panel.getString("type")).isEqualTo("heatmap");
        assertThat(expr(panel, 0))
                .isEqualTo("sum by (le) (increase(probe_work_histogram_seconds_bucket[$__rate_interval]))");
    }

    @Test
    public void aTracesCardBecomesATempoPanelForThisApplication() {
        JsonObject dashboard = OTLP.build(List.of(Map.of("kind", "signal", "id", "traces", "title", "Traces")), null);

        JsonObject panel = panel(dashboard, 0);
        assertThat(panel.getJsonObject("datasource").getString("type")).isEqualTo("tempo");
        assertThat(panel.getJsonArray("targets").getJsonObject(0).getString("query"))
                .isEqualTo("{ resource.service.name = \"orders\" }");
        // Without a data source on the query itself the panel queries Prometheus and stays empty.
        assertThat(panel.getJsonArray("targets").getJsonObject(0).getJsonObject("datasource").getString("type"))
                .isEqualTo("tempo");
        // Grafana asks which data sources to use on import, so the dashboard is not tied to one instance.
        assertThat(dashboard.getJsonArray("__inputs").stream().map(JsonObject.class::cast)
                .map(input -> input.getString("name"))).containsExactly("DS_PROMETHEUS", "DS_TEMPO");
    }

    @Test
    public void tempoIsOnlyAskedForWhenATracesCardIsExported() {
        JsonObject dashboard = OTLP.build(List.of(metric("jvm.memory.used", "gauge", "By")), null);

        assertThat(dashboard.getJsonArray("__inputs")).hasSize(1);
    }

    @Test
    public void cardsKeepTheirOrderAndAreLaidOutTwoToARow() {
        JsonArray panels = OTLP.build(List.of(
                metric("first", "value", ""),
                metric("second", "value", ""),
                metric("third", "value", "")), null).getJsonArray("panels");

        assertThat(panels.stream().map(JsonObject.class::cast).map(p -> p.getString("title")))
                .containsExactly("first", "second", "third");
        assertThat(panels.getJsonObject(0).getJsonObject("gridPos").getInteger("x")).isZero();
        assertThat(panels.getJsonObject(1).getJsonObject("gridPos").getInteger("x")).isEqualTo(12);
        assertThat(panels.getJsonObject(2).getJsonObject("gridPos").getInteger("y")).isEqualTo(8);
    }

    @Test
    public void aSignalCardTakesAWholeRow() {
        JsonArray panels = OTLP.build(List.of(
                metric("first", "value", ""),
                Map.of("kind", "signal", "id", "traces", "title", "Traces"),
                metric("second", "value", "")), null).getJsonArray("panels");

        assertThat(panels.getJsonObject(1).getJsonObject("gridPos").getInteger("w")).isEqualTo(24);
        assertThat(panels.getJsonObject(1).getJsonObject("gridPos").getInteger("x")).isZero();
        assertThat(panels.getJsonObject(2).getJsonObject("gridPos").getInteger("x")).isZero();
        // The row a card starts below is as tall as that row was, not as tall as the card itself.
        assertThat(panels.getJsonObject(1).getJsonObject("gridPos").getInteger("y")).isEqualTo(8);
        assertThat(panels.getJsonObject(2).getJsonObject("gridPos").getInteger("y")).isEqualTo(18);
    }

    @Test
    public void aCardWithNothingToDrawLeavesNoHoleInTheLayout() {
        JsonArray panels = OTLP.build(List.of(
                Map.of("kind", "signal", "id", "something-else"),
                metric("first", "value", ""),
                metric("second", "value", "")), null).getJsonArray("panels");

        assertThat(panels).hasSize(2);
        assertThat(panels.getJsonObject(0).getJsonObject("gridPos").getInteger("y")).isZero();
        assertThat(panels.getJsonObject(0).getJsonObject("gridPos").getInteger("x")).isZero();
        assertThat(panels.getJsonObject(1).getJsonObject("gridPos").getInteger("x")).isEqualTo(12);
    }

    @Test
    public void aSignalWithNoGrafanaEquivalentIsLeftOut() {
        JsonObject dashboard = OTLP.build(List.of(Map.of("kind", "signal", "id", "something-else")), null);

        assertThat(dashboard.getJsonArray("panels")).isEmpty();
    }

    @Test
    public void anEmptyDashboardStillCarriesItsTitleAndDescription() {
        JsonObject dashboard = OTLP.build(List.of(), null);

        assertThat(dashboard.getString("title")).isEqualTo("Quarkus Dev UI observability");
        assertThat(dashboard.getString("description")).contains("orders");
        assertThat(dashboard.getJsonArray("panels")).isEmpty();
    }
}
