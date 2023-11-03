package io.quarkus.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

import org.junit.jupiter.api.Test;

class JsonbUnitTest {

    @Test
    void testJsonbWorksProperly() {
        SomeClass instance = JsonbBuilder.create().fromJson("{\"time\":1570039000}", SomeClass.class);
        assertEquals(Instant.ofEpochSecond(1570039000), instance.getTime());
    }

    public static class SomeClass {

        @JsonbProperty("time")
        @JsonbTypeAdapter(EpochSecondsAdapter.class)
        private Instant time;

        public SomeClass() {
        }

        public Instant getTime() {
            return time;
        }

        public void setTime(Instant time) {
            this.time = time;
        }

        public static class EpochSecondsAdapter implements JsonbAdapter<Instant, Long> {
            @Override
            public Long adaptToJson(Instant obj) {
                return obj.getEpochSecond();
            }

            @Override
            public Instant adaptFromJson(Long obj) {
                return Instant.ofEpochSecond(obj);
            }
        }
    }
}
