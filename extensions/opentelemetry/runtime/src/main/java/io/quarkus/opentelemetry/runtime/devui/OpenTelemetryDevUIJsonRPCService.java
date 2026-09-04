package io.quarkus.opentelemetry.runtime.devui;

import jakarta.inject.Inject;

import io.quarkus.devui.observability.store.TelemetryStore;
import io.smallrye.mutiny.Multi;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * JSON-RPC backend for the Dev UI traces page. Exposes an initial snapshot, a live
 * stream of finished spans, the current span count, and a clear action.
 *
 * NOTE: NO class-level scope annotation on purpose — see the note on
 * {@link DevUiTracesStoreProducer}. Registered as a bean only by the dev-only build
 * step (which also produces the {@code JsonRPCProvidersBuildItem}); the build step
 * supplies the scope.
 */
public class OpenTelemetryDevUIJsonRPCService {

    @Inject
    TelemetryStore<SpanRecord> store;

    /**
     * @return {@code { "traces": [ {traceId, windowStart, windowEnd, spans:[...]} ] }}
     */
    public JsonObject getSnapshot() {
        JsonArray traces = SpanRecord.group(store.snapshot());
        return new JsonObject().put("traces", traces);
    }

    /**
     * Live stream of newly finished spans (each already serialized). Drops on overflow
     * so a slow Dev UI socket never slows request handling.
     */
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
