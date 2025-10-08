package io.quarkus.jackson.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonFailOnUnknownPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-fail-on-unknown-properties.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testFailOnUnknownProperties() throws DatabindException, JacksonException {
        Assertions.assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue("{\"property\": \"name\", \"unknownProperty\": \"unknown\"}", Pojo.class));
    }

    public static class Pojo {

        public String property;
    }
}
