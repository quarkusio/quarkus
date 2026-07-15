package io.quarkus.devjsonrpc.deployment;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonTypeAdapter;

public class DevJsonRpcDatabindCodec implements JsonMapper {
    private final ObjectMapper mapper;
    private final ObjectMapper prettyMapper;
    private final Function<Map<String, Object>, ?> runtimeObjectDeserializer;
    private final Function<List<?>, ?> runtimeArrayDeserializer;

    private DevJsonRpcDatabindCodec(ObjectMapper mapper,
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
    public <T> T fromString(String str, Class<T> clazz) {
        return fromParser(createParser(str), clazz);
    }

    private JsonParser createParser(String str) {
        try {
            return mapper.getFactory().createParser(str);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode:" + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T fromParser(JsonParser parser, Class<T> type) {
        T value;
        JsonToken remaining;
        try {
            value = mapper.readValue(parser, type);
            remaining = parser.nextToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode:" + e.getMessage(), e);
        } finally {
            close(parser);
        }
        if (remaining != null) {
            throw new RuntimeException("Unexpected trailing token");
        }
        if (type == Object.class) {
            value = (T) adapt(value);
        }
        return value;
    }

    @Override
    public String toString(Object object, boolean pretty) {
        try {
            ObjectMapper theMapper = pretty ? prettyMapper : mapper;
            return theMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode as JSON: " + e.getMessage(), e);
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
            if (o instanceof List && runtimeArrayDeserializer != null) {
                List<?> list = (List<?>) o;
                return runtimeArrayDeserializer.apply(list);
            } else if (o instanceof Map && runtimeObjectDeserializer != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) o;
                return runtimeObjectDeserializer.apply(map);
            }
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode: " + e.getMessage());
        }
    }

    public static final class Factory implements JsonMapper.Factory {
        private final List<Consumer<ObjectMapper>> customizers = new ArrayList<>();

        public Factory addCustomizer(Consumer<ObjectMapper> customizer) {
            customizers.add(customizer);
            return this;
        }

        @Override
        public JsonMapper create(JsonTypeAdapter<?, Map<String, Object>> jsonObjectAdapter,
                JsonTypeAdapter<?, List<?>> jsonArrayAdapter, JsonTypeAdapter<?, String> bufferAdapter) {
            // We want our own mapper, separate from the user-configured one.
            ObjectMapper mapper = new ObjectMapper();

            // Non-standard JSON but we allow C style comments in our JSON
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            SimpleModule module = new SimpleModule("devjsonrpc-module-common");
            module.addSerializer(Instant.class, new JsonSerializer<Instant>() {
                @Override
                public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    gen.writeString(ISO_INSTANT.format(value));
                }
            });
            module.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
                @Override
                public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String text = p.getText();
                    try {
                        return Instant.from(ISO_INSTANT.parse(text));
                    } catch (DateTimeException e) {
                        throw new InvalidFormatException(p, "Expected an ISO 8601 formatted date time", text, Instant.class);
                    }
                }
            });
            module.addSerializer(byte[].class, new JsonSerializer<byte[]>() {
                @Override
                public void serialize(byte[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    gen.writeString(Base64.getEncoder().encodeToString(value));
                }
            });
            module.addDeserializer(byte[].class, new JsonDeserializer<byte[]>() {
                @Override
                public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    return Base64.getDecoder().decode(p.getText());
                }
            });
            module.addSerializer(ByteArrayInputStream.class, new ByteArrayInputStreamSerializer());
            module.addDeserializer(ByteArrayInputStream.class, new ByteArrayInputStreamDeserializer());
            mapper.registerModule(module);
            mapper.registerModule(new Jdk8Module());

            SimpleModule runtimeModule = new SimpleModule("devjsonrpc-module-runtime");
            if (jsonObjectAdapter != null) {
                addAdapterToObject(runtimeModule, jsonObjectAdapter);
            }
            if (jsonArrayAdapter != null) {
                addAdapterToObject(runtimeModule, jsonArrayAdapter);
            }
            if (bufferAdapter != null) {
                addAdapterToString(runtimeModule, bufferAdapter);
            }
            mapper.registerModule(runtimeModule);

            for (Consumer<ObjectMapper> customizer : customizers) {
                customizer.accept(mapper);
            }

            return new DevJsonRpcDatabindCodec(mapper,
                    jsonObjectAdapter != null ? jsonObjectAdapter.deserializer : null,
                    jsonArrayAdapter != null ? jsonArrayAdapter.deserializer : null);
        }

        private static <T, S> void addAdapterToObject(SimpleModule module, JsonTypeAdapter<T, S> adapter) {
            module.addSerializer(adapter.type, new JsonSerializer<>() {
                @Override
                public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeObject(adapter.serializer.apply(value));
                }
            });
        }

        private static <T> void addAdapterToString(SimpleModule module, JsonTypeAdapter<T, String> adapter) {
            module.addSerializer(adapter.type, new JsonSerializer<>() {
                @Override
                public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeString(adapter.serializer.apply(value));
                }
            });
            module.addDeserializer(adapter.type, new JsonDeserializer<T>() {
                @Override
                public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
                    return adapter.deserializer.apply(parser.getText());
                }
            });
        }
    }

    // Inner serializers for ByteArrayInputStream (copied from DevUI, these don't depend on Vert.x)
    private static class ByteArrayInputStreamSerializer extends JsonSerializer<ByteArrayInputStream> {
        @Override
        public void serialize(ByteArrayInputStream value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            byte[] bytes = value.readAllBytes();
            gen.writeBinary(bytes);
        }
    }

    private static class ByteArrayInputStreamDeserializer extends JsonDeserializer<ByteArrayInputStream> {
        @Override
        public ByteArrayInputStream deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            byte[] bytes = p.getBinaryValue();
            return new ByteArrayInputStream(bytes);
        }
    }
}
