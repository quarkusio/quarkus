package io.quarkus.devui.runtime.observability.metrics;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.inject.Inject;

import io.quarkus.devui.observability.store.metrics.MetricCatalogEntry;
import io.quarkus.devui.observability.store.metrics.MetricSample;
import io.quarkus.devui.observability.store.metrics.MetricSeriesSnapshot;
import io.quarkus.devui.observability.store.metrics.MetricsTimeSeriesStore;
import io.smallrye.mutiny.Multi;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * JSON-RPC backend for the Dev UI metrics page: a grouped catalog, a mutable selection, a
 * snapshot of the selected series' history, a live stream of new samples, the meter count,
 * and a clear action. Does ALL Vert.x JSON shaping here (the store lib is Vert.x-free),
 * mirroring how {@code SpanRecord.toJson}/{@code OpenTelemetryDevUIJsonRPCService} shape the
 * traces JSON in the OTel runtime.
 *
 * NOTE: NO class-level scope annotation on purpose (see {@code MetricsStoreProducer} and the
 * traces service): registered as a bean only by the dev-only build step via
 * {@code JsonRPCProvidersBuildItem}, which scopes it {@code @ApplicationScoped}. The injected
 * {@link MetricsTimeSeriesStore} is supplied by {@code MetricsStoreProducer}.
 */
public class MetricsDevUIJsonRPCService {

    @Inject
    MetricsTimeSeriesStore store;

    public JsonObject getCatalog() {
        // group -> [metric metadata], ordered for a stable UI.
        TreeMap<String, JsonArray> groups = new TreeMap<>();
        for (MetricCatalogEntry e : store.catalog().snapshot()) {
            JsonObject metric = new JsonObject()
                    .put("name", e.name())
                    .put("type", e.type())
                    .put("cumulative", e.cumulative())
                    .put("seriesCount", e.seriesCount())
                    .put("lastValue", e.lastValue());
            groups.computeIfAbsent(e.group(), g -> new JsonArray()).add(metric);
        }
        JsonArray groupsJson = new JsonArray();
        for (Map.Entry<String, JsonArray> g : groups.entrySet()) {
            groupsJson.add(new JsonObject().put("group", g.getKey()).put("metrics", g.getValue()));
        }
        // Include the server's window/interval so the client can trim its appended stream to the
        // same retention the server enforces (it can't otherwise know these runtime values).
        return new JsonObject()
                .put("groups", groupsJson)
                .put("retentionMillis", store.retentionMillis())
                .put("sampleIntervalMillis", store.sampleIntervalMillis());
    }

    public JsonObject setSelection(List<String> names) {
        List<String> safe = names == null ? List.of() : names;
        store.setSelection(safe);
        return new JsonObject().put("selection", new JsonArray(safe));
    }

    /** {@code { "sections": [ { "name", "series": [ { "tags","type","cumulative","source","points":[[ts,val]] } ] } ] }} */
    public JsonObject getSnapshot() {
        // Group selected series by metric name, both ordered for a stable UI.
        TreeMap<String, JsonArray> byName = new TreeMap<>();
        for (MetricSeriesSnapshot ser : store.snapshot()) {
            byName.computeIfAbsent(ser.name(), n -> new JsonArray()).add(seriesJson(ser));
        }
        JsonArray sections = new JsonArray();
        for (Map.Entry<String, JsonArray> e : byName.entrySet()) {
            sections.add(new JsonObject().put("name", e.getKey()).put("series", e.getValue()));
        }
        return new JsonObject().put("sections", sections);
    }

    public Multi<JsonObject> streamMetrics() {
        return store.stream().map(MetricsDevUIJsonRPCService::sampleJson);
    }

    public int meterCount() {
        // Advertises AVAILABILITY (catalog size), not the selected count — selection starts empty.
        return store.meterCount();
    }

    public boolean clear() {
        store.clear();
        return true;
    }

    private static JsonObject seriesJson(MetricSeriesSnapshot ser) {
        JsonObject tags = new JsonObject();
        for (Map.Entry<String, String> t : new TreeMap<>(ser.tags()).entrySet()) {
            tags.put(t.getKey(), t.getValue());
        }
        JsonArray points = new JsonArray();
        long[] ts = ser.timestamps();
        double[] vals = ser.values();
        for (int i = 0; i < ts.length; i++) {
            points.add(new JsonArray().add(ts[i]).add(vals[i]));
        }
        return new JsonObject()
                .put("tags", tags)
                .put("type", ser.type())
                .put("cumulative", ser.cumulative())
                .put("source", ser.source())
                .put("points", points);
    }

    private static JsonObject sampleJson(MetricSample s) {
        JsonObject tags = new JsonObject();
        for (Map.Entry<String, String> e : s.tags().entrySet()) {
            tags.put(e.getKey(), e.getValue());
        }
        return new JsonObject()
                .put("name", s.name())
                .put("tags", tags)
                .put("type", s.type())
                .put("cumulative", s.cumulative())
                .put("value", s.value())
                .put("timestamp", s.timestampMillis())
                .put("source", s.source());
    }
}
