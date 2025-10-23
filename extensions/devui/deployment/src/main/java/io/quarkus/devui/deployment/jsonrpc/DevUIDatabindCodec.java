package io.quarkus.devui.deployment.jsonrpc;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.platform.engine.UniqueId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;

import io.quarkus.deployment.dev.testing.TestResult;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.runtime.jsonrpc.json.JsonTypeAdapter;
import io.quarkus.vertx.runtime.jackson.ByteArrayDeserializer;
import io.quarkus.vertx.runtime.jackson.ByteArraySerializer;
import io.quarkus.vertx.runtime.jackson.InstantDeserializer;
import io.quarkus.vertx.runtime.jackson.InstantSerializer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.EncodeException;

public class DevUIDatabindCodec implements JsonMapper {
    private final ObjectMapper mapper;
    private final ObjectMapper prettyMapper;
    private final Function<Map<String, Object>, ?> runtimeObjectDeserializer;
    private final Function<List<?>, ?> runtimeArrayDeserializer;

    private DevUIDatabindCodec(ObjectMapper mapper,
            Function<Map<String, Object>, ?> runtimeObjectDeserializer,
            Function<List<?>, ?> runtimeArrayDeserializer) {
        this.mapper = mapper;
        prettyMapper = mapper.copy();
        prettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        this.runtimeObjectDeserializer = runtimeObjectDeserializer;
        this.runtimeArrayDeserializer = runtimeArrayDeserializer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromValue(Object json, Class<T> clazz) {
        T value = mapper.convertValue(json, clazz);
        if (clazz == Object.class) {
            value = (T) adapt(value);
        }
        return value;
    }

    @Override
    public <T> T fromString(String str, Class<T> clazz) throws DecodeException {
        return fromParser(createParser(str), clazz);
    }

    private JsonParser createParser(String str) {
        try {
            return mapper.getFactory().createParser(str);
        } catch (IOException e) {
            throw new DecodeException("Failed to decode:" + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T fromParser(JsonParser parser, Class<T> type) throws DecodeException {
        T value;
        JsonToken remaining;
        try {
            value = mapper.readValue(parser, type);
            remaining = parser.nextToken();
        } catch (Exception e) {
            throw new DecodeException("Failed to decode:" + e.getMessage(), e);
        } finally {
            close(parser);
        }
        if (remaining != null) {
            throw new DecodeException("Unexpected trailing token");
        }
        if (type == Object.class) {
            value = (T) adapt(value);
        }
        return value;
    }

    @Override
    public String toString(Object object, boolean pretty) throws EncodeException {
        try {
            ObjectMapper theMapper = pretty ? prettyMapper : mapper;
            return theMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new EncodeException("Failed to encode as JSON: " + e.getMessage(), e);
        }
    }

    private static void close(Closeable parser) {
        try {
            parser.close();
        } catch (IOException ignore) {
        }
    }

    private Object adapt(Object o) {
        try {
            if (o instanceof List) {
                List<?> list = (List<?>) o;
                return runtimeArrayDeserializer.apply(list);
            } else if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) o;
                return runtimeObjectDeserializer.apply(map);
            }
            return o;
        } catch (Exception e) {
            throw new DecodeException("Failed to decode: " + e.getMessage());
        }
    }

    public static final class Factory implements JsonMapper.Factory {
        @Override
        public JsonMapper create(JsonTypeAdapter<?, Map<String, Object>> jsonObjectAdapter,
                JsonTypeAdapter<?, List<?>> jsonArrayAdapter, JsonTypeAdapter<?, String> bufferAdapter) {
            // We want our own mapper, separate from the user-configured one.
            ObjectMapper mapper = new ObjectMapper();

            // Non-standard JSON but we allow C style comments in our JSON
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            mapper.addMixIn(TestResult.class, TestResultMixIn.class);
            SimpleModule module = new SimpleModule("vertx-module-common");
            module.addSerializer(Instant.class, new InstantSerializer());
            module.addDeserializer(Instant.class, new InstantDeserializer());
            module.addSerializer(byte[].class, new ByteArraySerializer());
            module.addDeserializer(byte[].class, new ByteArrayDeserializer());
            module.addSerializer(ByteArrayInputStream.class, new ByteArrayInputStreamSerializer());
            module.addDeserializer(ByteArrayInputStream.class, new ByteArrayInputStreamDeserializer());
            mapper.registerModule(module);

            SimpleModule runtimeModule = new SimpleModule("vertx-module-runtime");
            addAdapterToObject(runtimeModule, jsonObjectAdapter);
            addAdapterToObject(runtimeModule, jsonArrayAdapter);
            addAdapterToString(runtimeModule, bufferAdapter);
            mapper.registerModule(runtimeModule);

            return new DevUIDatabindCodec(mapper, jsonObjectAdapter.deserializer, jsonArrayAdapter.deserializer);
        }

        private static <T, S> void addAdapterToObject(SimpleModule module, JsonTypeAdapter<T, S> adapter) {
            module.addSerializer(adapter.type, new ValueSerializer<>() {
                @Override
                public void serialize(T value, JsonGenerator jgen, SerializationContext provider) throws IOException {
                    jgen.writeObject(adapter.serializer.apply(value));
                }
            });
        }

        private static <T> void addAdapterToString(SimpleModule module, JsonTypeAdapter<T, String> adapter) {
            module.addSerializer(adapter.type, new ValueSerializer<>() {
                @Override
                public void serialize(T value, JsonGenerator jgen, SerializationContext provider) throws IOException {
                    jgen.writeString(adapter.serializer.apply(value));
                }
            });
            module.addDeserializer(adapter.type, new ValueDeserializer<T>() {
                @Override
                public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
                    return adapter.deserializer.apply(parser.getText());
                }
            });
        }
    }

    private interface TestResultMixIn {
        @JsonIgnore
        UniqueId getUniqueId();
    }
}
