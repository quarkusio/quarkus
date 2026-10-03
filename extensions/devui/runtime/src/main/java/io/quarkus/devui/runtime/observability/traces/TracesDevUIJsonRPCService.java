package io.quarkus.devui.runtime.observability.traces;

import jakarta.inject.Inject;

import io.quarkus.devui.observability.store.TelemetryStore;
import io.smallrye.mutiny.Multi;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * JSON-RPC backend for the traces view: the captured spans grouped by trace, a live stream of new ones, their count,
 * and a clear action.
 * <p>
 * NOTE: NO class-level scope annotation on purpose: registered only by the dev-only build step.
 */
public class TracesDevUIJsonRPCService {

    @Inject
    TelemetryStore<SpanRecord> store;

    public JsonObject getSnapshot() {
        JsonArray traces = SpanRecord.group(store.snapshot());
        return new JsonObject().put("traces", traces);
    }

    public Multi<JsonObject> streamSpans() {
        return store.stream().map(SpanRecord::toJson);
    }

    public int spanCount() {
        return store.size();
    }

    public boolean clear() {
        store.clear();
        return true;
    }
}
