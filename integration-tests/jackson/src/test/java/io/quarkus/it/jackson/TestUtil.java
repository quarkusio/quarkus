package io.quarkus.it.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

final class TestUtil {

    private static volatile ObjectMapper objectMapper;

    private TestUtil() {
    }

    // we need this because the ObjectMapper can't be retrieved from Arc in native tests
    public static ObjectMapper getObjectMapperForTest() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new ParameterNamesModule())
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule());
            new MyObjectMapperCustomizer().customize(objectMapper);
        }
        return objectMapper;
    }
}
