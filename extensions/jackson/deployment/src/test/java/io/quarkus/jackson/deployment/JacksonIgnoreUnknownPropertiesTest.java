package io.quarkus.jackson.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonIgnoreUnknownPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testIgnoreUnknownProperties() throws JsonMappingException, JsonProcessingException {
        Pojo pojo = objectMapper.readValue("{\"property\": \"name\", \"unknownProperty\": \"unknown\"}", Pojo.class);
        assertEquals("name", pojo.property);
    }

    public static class Pojo {

        public String property;
    }
}
