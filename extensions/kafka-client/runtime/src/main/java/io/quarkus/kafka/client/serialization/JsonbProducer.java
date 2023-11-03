package io.quarkus.kafka.client.serialization;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

final class JsonbProducer {

    private JsonbProducer() {
    }

    // Try to get Jsonb from Arc but fallback to regular Jsonb creation
    // The fallback could be used for example in unit tests where Arc has not been initialized
    static Jsonb get() {
        Jsonb jsonb = null;
        ArcContainer container = Arc.container();
        if (container != null) {
            jsonb = container.instance(Jsonb.class).get();
        }
        return jsonb != null ? jsonb : JsonbBuilder.create();
    }
}
