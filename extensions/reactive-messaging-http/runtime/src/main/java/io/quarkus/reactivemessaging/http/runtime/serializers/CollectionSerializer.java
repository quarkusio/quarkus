package io.quarkus.reactivemessaging.http.runtime.serializers;

import java.util.Collection;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * serializer of collections. Serializes collections to a json string inside a Buffer
 */
public class CollectionSerializer implements Serializer<Collection<?>> {

    @Override
    public boolean handles(Object payload) {
        return payload instanceof Collection;
    }

    @Override
    public Buffer serialize(Collection<?> payload) {
        JsonArray array = new JsonArray();
        for (Object element : payload) {
            array.add(JsonObject.mapFrom(element));
        }

        return array.toBuffer();
    }

    @Override
    public int getPriority() {
        // first try other serializers, try this one if they don't match
        return DEFAULT_PRIORITY - 100;
    }
}
