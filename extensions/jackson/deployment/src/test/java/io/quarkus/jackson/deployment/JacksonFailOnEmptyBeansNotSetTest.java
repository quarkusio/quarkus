package io.quarkus.jackson.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidDefinitionException;

public class JacksonFailOnEmptyBeansNotSetTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testFailOnEmptyBeans() {
        Assertions.assertThrows(InvalidDefinitionException.class,
                () -> objectMapper.writeValueAsString(new Pojo("dummy")));
    }

    public static class Pojo {

        private final String property;

        public Pojo(String property) {
            this.property = property;
        }
    }
}
