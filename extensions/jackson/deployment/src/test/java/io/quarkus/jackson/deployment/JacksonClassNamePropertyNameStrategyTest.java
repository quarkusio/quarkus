package io.quarkus.jackson.deployment;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonClassNamePropertyNameStrategyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(Pojo.class))
            .withConfigurationResource("application-class-name-property-name-strategy.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void test() throws JacksonException {
        Assertions.assertThat(objectMapper.writeValueAsString(new Pojo("test"))).isEqualTo("{\"test.property\":\"test\"}");
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
