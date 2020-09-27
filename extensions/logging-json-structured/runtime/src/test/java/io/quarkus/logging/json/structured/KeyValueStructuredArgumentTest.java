package io.quarkus.logging.json.structured;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.quarkus.logging.json.structured.jackson.JacksonJsonFactory;
import io.quarkus.logging.json.structured.providers.KeyValueStructuredArgument;

class KeyValueStructuredArgumentTest {

    @Test
    void testStructuredArgumentWriteTo() throws Exception {
        assertEquals("{\"key\":null}", run("key", null));
        assertEquals("{\"anotherKey\":null}", run("anotherKey", null));
        assertEquals("{\"key\":324}", run("key", (short) 324));
        assertEquals("{\"key\":324}", run("key", 324));
        assertEquals("{\"key\":324}", run("key", 324L));
        assertEquals("{\"key\":324.348}", run("key", 324.348));
        assertEquals("{\"key\":324.348}", run("key", 324.348d));
        assertEquals("{\"key\":324}", run("key", BigInteger.valueOf(324)));
        assertEquals("{\"key\":324.348}", run("key", BigDecimal.valueOf(324.348d)));
        assertEquals("{\"key\":\"value\"}", run("key", "value"));
        assertEquals("{\"key\":[\"value\",\"value2\"]}", run("key", new String[] { "value", "value2" }));
        assertEquals("{\"key\":[\"value\",\"value2\"]}", run("key", Arrays.asList("value", "value2")));
        assertEquals("{\"key\":{}}", run("key", new Object()));
        assertEquals("{\"key\":{\"field1\":\"field1\",\"field2\":2389472389}}", run("key", new TestPojo()));
    }

    private JsonFactory getJacksonFactory() {
        return new JacksonJsonFactory(true);
    }

    private String run(String key, Object value) throws IOException {

        StringBuilderWriter w = new StringBuilderWriter();
        try (JsonGenerator generator = getJacksonFactory().createGenerator(w)) {
            generator.writeStartObject();

            new KeyValueStructuredArgument(key, value).writeTo(generator);
            generator.writeEndObject();
            generator.flush();
        }

        return w.toString();
    }

    private static class TestPojo {
        private final String field1 = "field1";
        private final Long field2 = 2389472389L;

        public String getField1() {
            return field1;
        }

        public Long getField2() {
            return field2;
        }
    }
}
