package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;

public class JacksonAllowFinalFieldsAsMutatorsEnabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-allow-final-fields-as-mutators-enabled.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testFinalFieldsUsedAsMutators() {
        BeanWithFinalField result = objectMapper.readValue("{\"name\":\"hello\"}", BeanWithFinalField.class);
        assertThat(result.getName()).isEqualTo("hello");
    }

    public static class BeanWithFinalField {
        private final String name = null;

        public String getName() {
            return name;
        }
    }
}
