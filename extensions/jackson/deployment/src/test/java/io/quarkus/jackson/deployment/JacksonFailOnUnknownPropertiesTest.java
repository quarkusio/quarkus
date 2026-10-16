package io.quarkus.jackson.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

public class JacksonFailOnUnknownPropertiesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-fail-on-unknown-properties.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testFailOnUnknownProperties() {
        Assertions.assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue("{\"property\": \"name\", \"unknownProperty\": \"unknown\"}", Pojo.class));
    }

    public static class Pojo {

        public String property;
    }
}
