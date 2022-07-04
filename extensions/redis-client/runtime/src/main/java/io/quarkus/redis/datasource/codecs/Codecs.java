package io.quarkus.redis.datasource.codecs;

import java.nio.charset.StandardCharsets;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;

public class Codecs {

    private Codecs() {
        // Avoid direct instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> getDefaultCodecFor(Class<T> clazz) {
        if (clazz.equals(Double.class) || clazz.equals(Double.TYPE)) {
            return (Codec<T>) DoubleCodec.INSTANCE;
        }
        if (clazz.equals(Integer.class) || clazz.equals(Integer.TYPE)) {
            return (Codec<T>) IntegerCodec.INSTANCE;
        }
        if (clazz.equals(String.class)) {
            return (Codec<T>) StringCodec.INSTANCE;
        }
        // JSON by default
        return new JsonCodec<>(clazz);
    }

    public static class JsonCodec<T> implements Codec<T> {

        private final Class<T> clazz;

        public JsonCodec(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public byte[] encode(T item) {
            return Json.encodeToBuffer(item).getBytes();
        }

        @Override
        public T decode(byte[] payload) {
            return Json.decodeValue(Buffer.buffer(payload), clazz);
        }
    }

    public static class StringCodec implements Codec<String> {

        public static StringCodec INSTANCE = new StringCodec();

        private StringCodec() {
            // Avoid direct instantiation;
        }

        @Override
        public byte[] encode(String item) {
            return item.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String decode(byte[] item) {
            return new String(item, StandardCharsets.UTF_8);
        }
    }

    public static class DoubleCodec implements Codec<Double> {

        public static DoubleCodec INSTANCE = new DoubleCodec();

        private DoubleCodec() {
            // Avoid direct instantiation;
        }

        @Override
        public byte[] encode(Double item) {
            if (item == null) {
                return null;
            }
            return Double.toString(item).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Double decode(byte[] item) {
            if (item == null) {
                return 0.0;
            }
            return Double.parseDouble(new String(item, StandardCharsets.UTF_8));
        }
    }

    public static class IntegerCodec implements Codec<Integer> {

        public static IntegerCodec INSTANCE = new IntegerCodec();

        private IntegerCodec() {
            // Avoid direct instantiation;
        }

        @Override
        public byte[] encode(Integer item) {
            if (item == null) {
                return null;
            }
            return Integer.toString(item).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Integer decode(byte[] item) {
            if (item == null) {
                return 0;
            }
            return Integer.parseInt(new String(item, StandardCharsets.UTF_8));
        }
    }

}
