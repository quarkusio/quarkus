package io.quarkus.websockets.next.test.codec;

import java.lang.reflect.Type;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.TextMessageCodec;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.json.JsonObject;

@WebSocket(path = "/find-output-codec")
public class FindOutputCodec extends AbstractFind {

    // The codec is only used for output
    @OnTextMessage(outputCodec = MyOutputCodec.class)
    Item find(List<Item> items) {
        return super.find(items);
    }

    @Singleton
    @Priority(-10)
    public static class MyOutputCodec implements TextMessageCodec<Item> {

        @Override
        public boolean supports(Type type) {
            return type.equals(Item.class);
        }

        @Override
        public String encode(Item value) {
            JsonObject json = JsonObject.mapFrom(value);
            json.remove("count"); // intentionally remove the "count"
            return json.encode();
        }

        @Override
        public Item decode(Type type, String value) {
            throw new UnsupportedOperationException();
        }

    }

}
