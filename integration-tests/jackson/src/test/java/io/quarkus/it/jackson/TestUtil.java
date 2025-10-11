package io.quarkus.it.jackson;

import tools.jackson.databind.ObjectMapper;

final class TestUtil {

    private static volatile ObjectMapper objectMapper;

    private TestUtil() {
    }

    // we need this because the ObjectMapper can't be retrieved from Arc in native tests
    public static ObjectMapper getObjectMapperForTest() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            new MyObjectMapperCustomizer().customize(objectMapper);
        }
        return objectMapper;
    }
}
