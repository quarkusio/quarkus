package io.quarkus.funqy.test.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.response.ValidatableResponse;

@ApplicationScoped
public class BodyDeserializer {

    @Inject
    ObjectMapper objectMapper;

    /**
     * Allows to deserialize the response provided by RestAssured to the specified class.
     *
     * @param response RestAssured response
     * @param clazz class to deserialize to
     * @return the deserialized class
     * @param <T> type of the class to deserialize to
     */
    public <T> T getBodyAs(ValidatableResponse response, Class<T> clazz) {
        try {
            return objectMapper.readValue(response.extract().body().asString(), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
