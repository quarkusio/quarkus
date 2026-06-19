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
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonTypeAdapter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper.Builder;
import tools.jackson.databind.module.SimpleModule;

public class DevJsonRpcDatabindCodec implements JsonMapper {
    private final ObjectMapper mapper;
    private volatile ObjectMapper prettyMapper;
    private final Function<Map<String, Object>, ?> runtimeObjectDeserializer;
    private final Function<List<?>, ?> runtimeArrayDeserializer;

    private DevJsonRpcDatabindCodec(ObjectMapper mapper,
            Function<Map<String, Object>, ?> runtimeObjectDeserializer,
            Function<List<?>, ?> runtimeArrayDeserializer) {
        this.mapper = mapper;
        this.runtimeObjectDeserializer = runtimeObjectDeserializer;
        this.runtimeArrayDeserializer = runtimeArrayDeserializer;
    }

    private ObjectMapper prettyMapper() {
        if (prettyMapper == null) {
            prettyMapper = ((tools.jackson.databind.json.JsonMapper) mapper).rebuild()
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .build();
        }
        return prettyMapper;
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
        return mapper.createParser(str);
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
            ObjectMapper theMapper = pretty ? prettyMapper() : mapper;
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

        @Override
        public JsonMapper create(JsonTypeAdapter<?, Map<String, Object>> jsonObjectAdapter,
                JsonTypeAdapter<?, List<?>> jsonArrayAdapter, JsonTypeAdapter<?, String> bufferAdapter) {
            // We want our own mapper, separate from the user-configured one.
            Builder builder = tools.jackson.databind.json.JsonMapper.builder();

            // Non-standard JSON but we allow C style comments in our JSON
            builder.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true);
            builder.changeDefaultPropertyInclusion(new UnaryOperator<>() {
                @Override
                public JsonInclude.Value apply(JsonInclude.Value value) {
                    return value.withValueInclusion(JsonInclude.Include.NON_NULL);
                }
            });

            SimpleModule module = new SimpleModule("devjsonrpc-module-common");
            module.addSerializer(Instant.class, new ValueSerializer<Instant>() {
                @Override
                public void serialize(Instant value, JsonGenerator gen, SerializationContext provider)
                        throws JacksonException {
                    gen.writeString(ISO_INSTANT.format(value));
                }
            });
            module.addDeserializer(Instant.class, new ValueDeserializer<Instant>() {
                @Override
                public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                    String text = p.getText();
                    try {
                        return Instant.from(ISO_INSTANT.parse(text));
                    } catch (DateTimeException e) {
                        throw ctxt.weirdStringException(text, Instant.class,
                                "Expected an ISO 8601 formatted date time");
                    }
                }
            });
            module.addSerializer(byte[].class, new ValueSerializer<byte[]>() {
                @Override
                public void serialize(byte[] value, JsonGenerator gen, SerializationContext provider)
                        throws JacksonException {
                    gen.writeString(Base64.getEncoder().encodeToString(value));
                }
            });
            module.addDeserializer(byte[].class, new ValueDeserializer<byte[]>() {
                @Override
                public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                    return Base64.getDecoder().decode(p.getText());
                }
            });
            module.addSerializer(ByteArrayInputStream.class, new ValueSerializer<ByteArrayInputStream>() {
                @Override
                public void serialize(ByteArrayInputStream value, JsonGenerator gen, SerializationContext provider)
                        throws JacksonException {
                    byte[] bytes = value.readAllBytes();
                    gen.writeBinary(bytes);
                }
            });
            module.addDeserializer(ByteArrayInputStream.class, new ValueDeserializer<ByteArrayInputStream>() {
                @Override
                public ByteArrayInputStream deserialize(JsonParser p, DeserializationContext ctxt)
                        throws JacksonException {
                    byte[] bytes = p.getBinaryValue();
                    return new ByteArrayInputStream(bytes);
                }
            });
            builder.addModule(module);

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
            builder.addModule(runtimeModule);

            ObjectMapper mapper = builder.build();
            return new DevJsonRpcDatabindCodec(mapper,
                    jsonObjectAdapter != null ? jsonObjectAdapter.deserializer : null,
                    jsonArrayAdapter != null ? jsonArrayAdapter.deserializer : null);
        }

        private static <T, S> void addAdapterToObject(SimpleModule module, JsonTypeAdapter<T, S> adapter) {
            module.addSerializer(adapter.type, new ValueSerializer<>() {
                @Override
                public void serialize(T value, JsonGenerator jgen, SerializationContext provider) throws JacksonException {
                    jgen.writePOJO(adapter.serializer.apply(value));
                }
            });
        }

        private static <T> void addAdapterToString(SimpleModule module, JsonTypeAdapter<T, String> adapter) {
            module.addSerializer(adapter.type, new ValueSerializer<>() {
                @Override
                public void serialize(T value, JsonGenerator jgen, SerializationContext provider) throws JacksonException {
                    jgen.writeString(adapter.serializer.apply(value));
                }
            });
            module.addDeserializer(adapter.type, new ValueDeserializer<T>() {
                @Override
                public T deserialize(JsonParser parser, DeserializationContext ctxt) throws JacksonException {
                    return adapter.deserializer.apply(parser.getString());
                }
            });
        }
    }
}
