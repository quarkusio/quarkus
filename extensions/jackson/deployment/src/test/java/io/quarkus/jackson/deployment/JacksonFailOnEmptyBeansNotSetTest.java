package io.quarkus.jackson.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonFailOnEmptyBeansNotSetTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

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
