package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;

public class JacksonAllowFinalFieldsAsMutatorsDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testFinalFieldsNotUsedAsMutatorsByDefault() {
        BeanWithFinalField result = objectMapper.readValue("{\"name\":\"hello\"}", BeanWithFinalField.class);
        assertThat(result.getName()).isNull();
    }

    public static class BeanWithFinalField {
        private final String name = null;

        public String getName() {
            return name;
        }
    }
}
