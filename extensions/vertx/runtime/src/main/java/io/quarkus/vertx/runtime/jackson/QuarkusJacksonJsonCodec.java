package io.quarkus.vertx.runtime.jackson;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.netty.buffer.ByteBufInputStream;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.json.JsonCodec;

/**
 * The functionality of this class is copied almost verbatim from {@code io.vertx.core.json.jackson.DatabindCodec}.
 * The difference is that this class obtains the ObjectMapper from Arc in order to inherit the
 * user-customized ObjectMapper.
 */
class QuarkusJacksonJsonCodec implements JsonCodec {

    private static final ObjectMapper mapper;
    // we don't want to create this unless it's absolutely necessary (and it rarely is)
    private static volatile ObjectMapper prettyMapper;

    static {
        ArcContainer container = Arc.container();
        if (container == null) {
            // this can happen in QuarkusUnitTest
            mapper = new ObjectMapper();
        } else {
            ObjectMapper managedMapper = container.instance(ObjectMapper.class).get();
            if (managedMapper == null) {
                // TODO: is this too heavy handed? It should never happen but even if it does, it's a mostly recoverable state
                throw new IllegalStateException("There was no ObjectMapper bean configured");
            }
            // We don't want to change settings the settings of the User configured ObjectMapper,
            // but we do want to inherit all of the user's custom settings, so we copy the ObjectMapper.
            // Theoretically we could have checked to see if each of the settings
            // we want to apply is already applied, but in practice it doesn't make sense
            // as at the very least InstantSerializer and InstantDeserializer will be different than those provided by the
            // (always included with quarkus-jackson) JavaTimeModule.
            mapper = managedMapper.copy();
        }

        // Non-standard JSON but we allow C style comments in our JSON
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        SimpleModule module = new SimpleModule("vertx-module");
        // custom types
        module.addSerializer(JsonObject.class, new JsonObjectSerializer());
        module.addSerializer(JsonArray.class, new JsonArraySerializer());
        // we have 2 extensions: RFC-7493
        module.addSerializer(Instant.class, new InstantSerializer());
        module.addDeserializer(Instant.class, new InstantDeserializer());
        module.addSerializer(byte[].class, new ByteArraySerializer());
        module.addDeserializer(byte[].class, new ByteArrayDeserializer());
        module.addSerializer(Buffer.class, new BufferSerializer());
        module.addDeserializer(Buffer.class, new BufferDeserializer());

        mapper.registerModule(module);
    }

    private ObjectMapper prettyMapper() {
        if (prettyMapper == null) {
            prettyMapper = mapper.copy();
            prettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        }
        return prettyMapper;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromValue(Object json, Class<T> clazz) {
        T value = QuarkusJacksonJsonCodec.mapper.convertValue(json, clazz);
        if (clazz == Object.class) {
            value = (T) adapt(value);
        }
        return value;
    }

    @Override
    public <T> T fromString(String str, Class<T> clazz) throws DecodeException {
        return fromParser(createParser(str), clazz);
    }

    @Override
    public <T> T fromBuffer(Buffer buf, Class<T> clazz) throws DecodeException {
        return fromParser(createParser(buf), clazz);
    }

    public static JsonParser createParser(Buffer buf) {
        try {
            return QuarkusJacksonJsonCodec.mapper.getFactory()
                    .createParser((InputStream) new ByteBufInputStream(buf.getByteBuf()));
        } catch (IOException e) {
            throw new DecodeException("Failed to decode:" + e.getMessage(), e);
        }
    }

    public static JsonParser createParser(String str) {
        try {
            return QuarkusJacksonJsonCodec.mapper.getFactory().createParser(str);
        } catch (IOException e) {
            throw new DecodeException("Failed to decode:" + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromParser(JsonParser parser, Class<T> type) throws DecodeException {
        T value;
        JsonToken remaining;
        try {
            value = QuarkusJacksonJsonCodec.mapper.readValue(parser, type);
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
            ObjectMapper mapper = pretty ? prettyMapper() : QuarkusJacksonJsonCodec.mapper;
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new EncodeException("Failed to encode as JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public Buffer toBuffer(Object object, boolean pretty) throws EncodeException {
        try {
            ObjectMapper mapper = pretty ? prettyMapper() : QuarkusJacksonJsonCodec.mapper;
            return Buffer.buffer(mapper.writeValueAsBytes(object));
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

    @SuppressWarnings("rawtypes")
    private static Object adapt(Object o) {
        try {
            if (o instanceof List) {
                List list = (List) o;
                return new JsonArray(list);
            } else if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) o;
                return new JsonObject(map);
            }
            return o;
        } catch (Exception e) {
            throw new DecodeException("Failed to decode: " + e.getMessage());
        }
    }
}
