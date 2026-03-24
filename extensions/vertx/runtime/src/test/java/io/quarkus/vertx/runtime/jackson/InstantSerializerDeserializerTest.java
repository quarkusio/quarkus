package io.quarkus.vertx.runtime.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Test the Quarkus copies of Vert.x's package-private serializers using ISO_INSTANT format
 * (RFC-7493 compliant).
 */
public class InstantSerializerDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test-module");
        module.addSerializer(Instant.class, new InstantSerializer());
        module.addDeserializer(Instant.class, new InstantDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void roundTripNow() throws Exception {
        Instant now = Instant.now();
        String json = mapper.writeValueAsString(now);
        Instant deserialized = mapper.readValue(json, Instant.class);
        assertThat(deserialized).isEqualTo(now);
    }

    @Test
    public void roundTripEpoch() throws Exception {
        Instant epoch = Instant.EPOCH;
        String json = mapper.writeValueAsString(epoch);
        assertThat(json).isEqualTo("\"1970-01-01T00:00:00Z\"");
        Instant deserialized = mapper.readValue(json, Instant.class);
        assertThat(deserialized).isEqualTo(epoch);
    }

    @Test
    public void roundTripWithNanos() throws Exception {
        Instant withNanos = Instant.parse("2024-06-15T12:30:45.123456789Z");
        String json = mapper.writeValueAsString(withNanos);
        Instant deserialized = mapper.readValue(json, Instant.class);
        assertThat(deserialized).isEqualTo(withNanos);
    }

    @Test
    public void serializationFormat() throws Exception {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        String json = mapper.writeValueAsString(instant);
        assertThat(json).isEqualTo("\"2024-01-15T10:30:00Z\"");
    }

    @Test
    public void deserializeValidIsoString() throws Exception {
        Instant result = mapper.readValue("\"2024-06-15T12:30:45Z\"", Instant.class);
        assertThat(result).isEqualTo(Instant.parse("2024-06-15T12:30:45Z"));
    }

    @Test
    public void deserializeInvalidFormat() {
        assertThatThrownBy(() -> mapper.readValue("\"not-a-date\"", Instant.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("Expected an ISO 8601 formatted date time");
    }

    @Test
    public void roundTripInPojo() throws Exception {
        String json = mapper.writeValueAsString(new TimestampHolder(Instant.parse("2024-03-20T15:00:00Z")));
        assertThat(json).contains("2024-03-20T15:00:00Z");

        TimestampHolder holder = mapper.readValue(json, TimestampHolder.class);
        assertThat(holder.timestamp).isEqualTo(Instant.parse("2024-03-20T15:00:00Z"));
    }

    public static class TimestampHolder {
        public Instant timestamp;

        public TimestampHolder() {
        }

        public TimestampHolder(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
}
