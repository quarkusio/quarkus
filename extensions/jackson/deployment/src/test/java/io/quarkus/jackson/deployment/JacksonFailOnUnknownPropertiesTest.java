package io.quarkus.jackson.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonFailOnUnknownPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-fail-on-unknown-properties.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testFailOnUnknownProperties() throws JsonMappingException, JsonProcessingException {
        Assertions.assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue("{\"property\": \"name\", \"unknownProperty\": \"unknown\"}", Pojo.class));
    }

    public static class Pojo {

        public String property;
    }
}
