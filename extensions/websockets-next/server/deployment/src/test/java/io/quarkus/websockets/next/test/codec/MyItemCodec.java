package io.quarkus.websockets.next.test.codec;

import java.lang.reflect.Type;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.quarkus.websockets.next.TextMessageCodec;
import io.vertx.core.json.JsonObject;

@Singleton
@Priority(10)
public class MyItemCodec implements TextMessageCodec<Item> {

    @Override
    public boolean canHandle(Type type) {
        return type == Item.class;
    }

    @Override
    public String encode(Item value) {
        return new JsonObject().put("count", value.getCount()).encode();
    }

    @Override
    public Item decode(Type type, String value) {
        throw new UnsupportedOperationException();
    }

}
