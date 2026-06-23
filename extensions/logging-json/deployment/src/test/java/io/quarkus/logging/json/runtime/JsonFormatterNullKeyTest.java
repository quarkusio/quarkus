package io.quarkus.logging.json.runtime;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

class JsonFormatterNullKeyTest {

    @Test
    void jsonLogGeneratorAcceptsNullKeysWhenExclusionsAreConfigured() {
        JsonFormatter.JsonLogGenerator generator = new TestFormatter().newJsonLogGenerator(Set.of("timestamp", "sequence"));

        assertThatCode(() -> {
            generator.add(null, true);
            generator.add(null, 1);
            generator.add(null, 1L);
            generator.add(null, Map.of("key", "value"));
            generator.add(null, "value");
            generator.startObject(null).endObject();
            generator.startArray(null).endArray();
        }).doesNotThrowAnyException();
    }

    @Test
    void formatterKeepsExceptionRecordsWhenExclusionsAreConfigured() {
        JsonFormatter formatter = new JsonFormatter(null, Set.of("timestamp", "sequence"), Map.of());

        LogRecord record = new LogRecord(Level.WARNING, "Something went wrong");
        record.setThrown(new RuntimeException("boom"));

        assertThatCode(() -> formatter.format(record)).doesNotThrowAnyException();
    }

    private static final class TestFormatter extends JsonFormatter {

        JsonLogGenerator newJsonLogGenerator(Set<String> excludedKeys) {
            return new JsonLogGenerator(new TestGenerator(), excludedKeys);
        }

        private static final class TestGenerator implements Generator {

            @Override
            public Generator add(String key, Map<String, ?> value) {
                return this;
            }

            @Override
            public Generator add(String key, String value) {
                return this;
            }

            @Override
            public Generator startObject(String key) {
                return this;
            }

            @Override
            public Generator endObject() {
                return this;
            }

            @Override
            public Generator end() {
                return this;
            }
        }
    }

}
