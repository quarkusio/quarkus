package io.quarkus.redis.datasource.codecs;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.vertx.runtime.jackson.QuarkusJacksonJsonCodec;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;

public class Codecs {

    private Codecs() {
        // Avoid direct instantiation
    }

    private static final List<Codec> CODECS = new CopyOnWriteArrayList<>(
            List.of(StringCodec.INSTANCE, DoubleCodec.INSTANCE, IntegerCodec.INSTANCE, ByteArrayCodec.INSTANCE));

    public static void register(Codec codec) {
        CODECS.add(Objects.requireNonNull(codec));
    }

    public static void register(Stream<Codec> codecs) {
        codecs.forEach(Codecs::register);
    }

    public static Codec getDefaultCodecFor(Type type) {
        for (Codec codec : CODECS) {
            if (codec.canHandle(type)) {
                return codec;
            }
        }

        // JSON by default
        return new JsonCodec(type);
    }

    public static class JsonCodec implements Codec {
        private final TypeReference<?> type;
        private final Class<?> clazz;
        private final ObjectMapper mapper;

        public JsonCodec(Type clazz) {
            if (clazz instanceof Class) {
                this.clazz = (Class<?>) clazz;
                this.type = null;
            } else {
                this.type = new TypeReference<>() {
                    @Override
                    public Type getType() {
                        return clazz;
                    }
                };
                this.clazz = null;
            }
            this.mapper = QuarkusJacksonJsonCodec.mapper();
        }

        @Override
        public boolean canHandle(Type clazz) {
            throw new UnsupportedOperationException("Should not be called, the JSON codec is the fallback");
        }

        @Override
        public byte[] encode(Object item) {
            return Json.encodeToBuffer(item).getBytes();
        }

        @Override
        public Object decode(byte[] payload) {
            try {
                if (clazz != null) {
                    return Json.decodeValue(Buffer.buffer(payload), clazz);
                } else {
                    return mapper.readValue(payload, type);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class StringCodec implements Codec {

        public static StringCodec INSTANCE = new StringCodec();

        private StringCodec() {
            // Avoid direct instantiation;
        }

        @Override
        public boolean canHandle(Type clazz) {
            return clazz.equals(String.class);
        }

        @Override
        public byte[] encode(Object item) {
            return ((String) item).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String decode(byte[] item) {
            return new String(item, StandardCharsets.UTF_8);
        }
    }

    public static class DoubleCodec implements Codec {

        public static DoubleCodec INSTANCE = new DoubleCodec();

        private DoubleCodec() {
            // Avoid direct instantiation;
        }

        @Override
        public boolean canHandle(Type clazz) {
            return clazz.equals(Double.class) || clazz.equals(Double.TYPE);
        }

        @Override
        public byte[] encode(Object item) {
            if (item == null) {
                return null;
            }
            return Double.toString((double) item).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Double decode(byte[] item) {
            if (item == null) {
                return 0.0;
            }
            return Double.parseDouble(new String(item, StandardCharsets.UTF_8));
        }
    }

    public static class IntegerCodec implements Codec {

        public static IntegerCodec INSTANCE = new IntegerCodec();

        private IntegerCodec() {
            // Avoid direct instantiation;
        }

        @Override
        public boolean canHandle(Type clazz) {
            return clazz.equals(Integer.class) || clazz.equals(Integer.TYPE);
        }

        @Override
        public byte[] encode(Object item) {
            if (item == null) {
                return null;
            }
            return Integer.toString((int) item).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Integer decode(byte[] item) {
            if (item == null) {
                return 0;
            }
            return Integer.parseInt(new String(item, StandardCharsets.UTF_8));
        }
    }

    public static class ByteArrayCodec implements Codec {

        public static ByteArrayCodec INSTANCE = new ByteArrayCodec();

        private ByteArrayCodec() {
            // Avoid direct instantiation;
        }

        @Override
        public boolean canHandle(Type clazz) {
            return clazz.equals(byte[].class);
        }

        @Override
        public byte[] encode(Object item) {
            return (byte[]) item;
        }

        @Override
        public byte[] decode(byte[] item) {
            return item;
        }
    }

}
