package io.quarkus.jackson.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonFailOnEmptyBeansSetTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-fail-on-empty-beans.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testFailOnEmptyBeans() throws JsonProcessingException {
        Assertions.assertEquals("{}", objectMapper.writeValueAsString(new Pojo("dummy")));
    }

    public static class Pojo {

        private final String property;

        public Pojo(String property) {
            this.property = property;
        }
    }
}
