package io.quarkus.amazon.lambda.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.runtime.FunctionError;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

public class ObjectMapperNamingStrategyTest {

    @Test
    public void testFunctionErrorAlwaysUsesCamelCase() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

        FunctionError error = new FunctionError("RuntimeError", "something went wrong");
        String json = mapper.writeValueAsString(error);

        Assertions.assertTrue(json.contains("\"errorType\""), "FunctionError should use camelCase for errorType, got: " + json);
        Assertions.assertTrue(json.contains("\"errorMessage\""),
                "FunctionError should use camelCase for errorMessage, got: " + json);
        Assertions.assertFalse(json.contains("\"error_type\""),
                "FunctionError should not use snake_case for errorType, got: " + json);
    }

    @Test
    public void testUserNamingStrategyIsPreserved() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

        String json = mapper.writeValueAsString(new UserDto("John", "Doe", 30));

        Assertions.assertTrue(json.contains("\"first_name\""),
                "User DTO should respect SNAKE_CASE strategy, got: " + json);
        Assertions.assertTrue(json.contains("\"last_name\""),
                "User DTO should respect SNAKE_CASE strategy, got: " + json);
    }

    public static class UserDto {
        private String firstName;
        private String lastName;
        private int userAge;

        public UserDto(String firstName, String lastName, int userAge) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.userAge = userAge;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public int getUserAge() {
            return userAge;
        }
    }
}
