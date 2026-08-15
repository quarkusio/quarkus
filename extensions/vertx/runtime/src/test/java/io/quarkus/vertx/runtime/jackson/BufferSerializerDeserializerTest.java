package io.quarkus.vertx.runtime.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Buffers should be serialized as Base64 URL-encoded strings.
 */
public class BufferSerializerDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        JsonMapper.Builder builder = JsonMapper.builder();
        SimpleModule module = new SimpleModule("test-module");
        module.addSerializer(Buffer.class, new BufferSerializer());
        module.addDeserializer(Buffer.class, new BufferDeserializer());
        builder.addModule(module);
        mapper = builder.build();
    }

    @Test
    public void roundTripSimpleContent() throws Exception {
        Buffer original = Buffer.buffer("hello world");
        String json = mapper.writeValueAsString(original);
        Buffer deserialized = mapper.readValue(json, Buffer.class);
        assertThat(deserialized.getBytes()).isEqualTo(original.getBytes());
    }

    @Test
    public void roundTripEmptyBuffer() throws Exception {
        Buffer original = Buffer.buffer();
        String json = mapper.writeValueAsString(original);
        Buffer deserialized = mapper.readValue(json, Buffer.class);
        assertThat(deserialized.length()).isEqualTo(0);
    }

    @Test
    public void roundTripBinaryContent() throws Exception {
        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        Buffer original = Buffer.buffer(bytes);
        String json = mapper.writeValueAsString(original);
        Buffer deserialized = mapper.readValue(json, Buffer.class);
        assertThat(deserialized.getBytes()).isEqualTo(original.getBytes());
    }

    @Test
    public void serializationIsBase64UrlEncoded() throws Exception {
        // Base64 URL encoding uses - and _ instead of + and /
        Buffer buffer = Buffer.buffer(new byte[] { (byte) 0xFB, (byte) 0xFF, (byte) 0xFE });
        String json = mapper.writeValueAsString(buffer);
        // The JSON value should be a quoted string
        assertThat(json).startsWith("\"").endsWith("\"");

        String encoded = json.substring(1, json.length() - 1);
        byte[] decoded = JsonUtil.BASE64_DECODER.decode(encoded);
        assertThat(decoded).isEqualTo(buffer.getBytes());
    }

    @Test
    public void deserializeInvalidBase64() {
        assertThatThrownBy(() -> mapper.readValue("\"not!valid!base64!!!\"", Buffer.class))
                .isInstanceOf(tools.jackson.databind.exc.InvalidFormatException.class)
                .hasMessageContaining("Expected a base64 encoded byte array");
    }

    @Test
    public void roundTripInPojo() throws Exception {
        BufferHolder holder = new BufferHolder();
        holder.data = Buffer.buffer("test data");

        String json = mapper.writeValueAsString(holder);
        BufferHolder deserialized = mapper.readValue(json, BufferHolder.class);
        assertThat(deserialized.data.toString()).isEqualTo("test data");
    }

    public static class BufferHolder {
        public Buffer data;
    }
}
