package io.quarkus.websockets.next.test.codec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import io.quarkus.websockets.next.BinaryMessageCodec;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@WebSocket(path = "/find-binary")
public class FindBinary extends AbstractFind {

    // There's no binary codec available out of the box so the codecs below are needed
    @OnBinaryMessage
    Item find(List<Item> items) {
        return super.find(items);
    }

    @Singleton
    public static class ItemBinaryMessageCodec implements BinaryMessageCodec<Item> {

        @Override
        public boolean supports(Type type) {
            return type.equals(Item.class);
        }

        @Override
        public Buffer encode(Item value) {
            return Buffer.buffer(value.toString());
        }

        @Override
        public Item decode(Type type, Buffer value) {
            throw new UnsupportedOperationException();
        }

    }

    @Singleton
    public static class ListItemBinaryMessageCodec implements BinaryMessageCodec<List<Item>> {

        @Override
        public boolean supports(Type type) {
            return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(List.class);
        }

        @Override
        public Buffer encode(List<Item> value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Item> decode(Type type, Buffer value) {
            JsonArray json = value.toJsonArray();
            List<Item> items = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Item item = new Item();
                JsonObject jsonObject = json.getJsonObject(i);
                // Intentionally skip the name
                item.setCount(2 * jsonObject.getInteger("count"));
                items.add(item);
            }
            return items;
        }

    }

}
