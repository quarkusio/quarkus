package io.quarkus.jackson.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonIgnoreUnknownPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testIgnoreUnknownProperties() throws DatabindException, JacksonException {
        Pojo pojo = objectMapper.readValue("{\"property\": \"name\", \"unknownProperty\": \"unknown\"}", Pojo.class);
        assertEquals("name", pojo.property);
    }

    public static class Pojo {

        public String property;
    }
}
