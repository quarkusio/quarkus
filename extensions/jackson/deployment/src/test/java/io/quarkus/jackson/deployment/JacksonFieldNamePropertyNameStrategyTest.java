package io.quarkus.jackson.deployment;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;

public class JacksonFieldNamePropertyNameStrategyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClass(Pojo.class))
            .withConfigurationResource("application-field-name-property-name-strategy.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void test() {
        Assertions.assertThat(objectMapper.writeValueAsString(new Pojo("test"))).isEqualTo("{\"test-property\":\"test\"}");
    }

    public static class Pojo {

        private final String testProperty;

        public Pojo(String testProperty) {
            this.testProperty = testProperty;
        }

        public String getTestProperty() {
            return testProperty;
        }
    }
}
