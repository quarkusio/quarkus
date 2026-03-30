package io.quarkus.vertx.runtime.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Byte arrays are serialized as Base64 URL-encoded strings (RFC-7493 compliant, not standard Base64).
 */
public class ByteArraySerializerDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test-module");
        module.addSerializer(byte[].class, new ByteArraySerializer());
        module.addDeserializer(byte[].class, new ByteArrayDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void roundTripSimple() throws Exception {
        byte[] original = "hello".getBytes();
        String json = mapper.writeValueAsString(original);
        byte[] deserialized = mapper.readValue(json, byte[].class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    public void roundTripEmpty() throws Exception {
        byte[] original = new byte[0];
        String json = mapper.writeValueAsString(original);
        byte[] deserialized = mapper.readValue(json, byte[].class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    public void roundTripAllByteValues() throws Exception {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }
        String json = mapper.writeValueAsString(original);
        byte[] deserialized = mapper.readValue(json, byte[].class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    public void usesBase64UrlEncoding() throws Exception {
        // Bytes that would produce + and / in standard Base64 should produce - and _ in URL-safe Base64
        byte[] bytes = new byte[] { (byte) 0xFB, (byte) 0xFF, (byte) 0xFE };
        String json = mapper.writeValueAsString(bytes);
        String encoded = json.substring(1, json.length() - 1); // strip quotes
        // URL-safe Base64 should NOT contain + or /
        assertThat(encoded).doesNotContain("+").doesNotContain("/");

        byte[] decoded = JsonUtil.BASE64_DECODER.decode(encoded);
        assertThat(decoded).isEqualTo(bytes);
    }

    @Test
    public void deserializeInvalidBase64() {
        assertThatThrownBy(() -> mapper.readValue("\"not!valid!base64!!!\"", byte[].class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("Expected a base64 encoded byte array");
    }

    @Test
    public void roundTripInPojo() throws Exception {
        ByteArrayHolder holder = new ByteArrayHolder();
        holder.data = new byte[] { 1, 2, 3, 4, 5 };

        String json = mapper.writeValueAsString(holder);
        ByteArrayHolder deserialized = mapper.readValue(json, ByteArrayHolder.class);
        assertThat(deserialized.data).isEqualTo(new byte[] { 1, 2, 3, 4, 5 });
    }

    public static class ByteArrayHolder {
        public byte[] data;
    }
}
