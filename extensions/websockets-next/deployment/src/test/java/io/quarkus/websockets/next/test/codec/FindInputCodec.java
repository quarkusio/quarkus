package io.quarkus.websockets.next.test.codec;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.TextMessageCodec;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@WebSocket(path = "/find-input-codec")
public class FindInputCodec extends AbstractFind {

    // The codec is used for both input/output
    @OnTextMessage(codec = MyInputCodec.class)
    Item find(List<Item> items) {
        return super.find(items);
    }

    @Singleton
    @Priority(-10)
    public static class MyInputCodec implements TextMessageCodec<Object> {

        @Override
        public boolean supports(Type type) {
            return true;
        }

        @Override
        public String encode(Object value) {
            return value.toString();
        }

        @Override
        public Object decode(Type type, String value) {
            JsonArray json = new JsonArray(value);
            List<Item> items = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Item item = new Item();
                JsonObject jsonObject = json.getJsonObject(i);
                item.setCount(2 * jsonObject.getInteger("count"));
                items.add(item);
            }
            return items;
        }

    }

}
