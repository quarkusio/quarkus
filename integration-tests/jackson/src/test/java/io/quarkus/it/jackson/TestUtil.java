package io.quarkus.it.jackson;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

final class TestUtil {

    private static volatile ObjectMapper objectMapper;

    private TestUtil() {
    }

    // we need this because the ObjectMapper can't be retrieved from Arc in native tests
    public static ObjectMapper getObjectMapperForTest() {
        if (objectMapper == null) {
            var builder = JsonMapper.builder();
            new MyObjectMapperCustomizer().customize(builder);
            objectMapper = builder.build();
        }
        return objectMapper;
    }
}
