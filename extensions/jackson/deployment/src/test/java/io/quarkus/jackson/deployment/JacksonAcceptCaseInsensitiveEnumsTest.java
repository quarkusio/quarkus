package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonAcceptCaseInsensitiveEnumsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-accept-case-insensitive-enums.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testAcceptCaseInsensitiveEnums() throws JsonProcessingException {
        // Test upper case
        TestObject uppercase = objectMapper.readValue("{ \"testEnum\": \"ONE\" }", TestObject.class);
        assertThat(uppercase.testEnum).isEqualTo(TestEnum.ONE);

        // Test lower case
        TestObject lowercase = objectMapper.readValue("{ \"testEnum\": \"one\" }", TestObject.class);
        assertThat(lowercase.testEnum).isEqualTo(TestEnum.ONE);

        // Test mixed case
        TestObject mixedcase = objectMapper.readValue("{ \"testEnum\": \"oNe\" }", TestObject.class);
        assertThat(mixedcase.testEnum).isEqualTo(TestEnum.ONE);
    }

    private enum TestEnum {
        ONE,
        TWO
    }

    private static class TestObject {
        public TestEnum testEnum;
    }

}
