package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class ObjectSerializer implements Serializer<Object> {

    @Override
    public boolean handles(Object payload) {
        return payload instanceof Object;
    }

    @Override
    public Buffer serialize(Object payload) {
        return JsonObject.mapFrom(payload).toBuffer();
    }

    @Override
    public int getPriority() {
        // first try other serializers, try this one if they don't match
        return DEFAULT_PRIORITY - 200;
    }
}
