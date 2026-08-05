package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.inject.Singleton;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import io.restassured.RestAssured;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public abstract class AbstractNonEmptySerializationTest {

    @Singleton
    public static class NonEmptyObjectMapperCustomizer implements JsonMapperBuilderCustomizer {

        @Override
        public void customize(JsonMapper.Builder builder) {
            builder
                    .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .changeDefaultPropertyInclusion(
                            incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY));
        }
    }

    @Test
    public void testObject() {
        RestAssured.get("/json-include/my-object")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.equalTo("name"))
                .body("description", Matchers.equalTo("description"))
                .body("map.test", Matchers.equalTo(1))
                .body("strings[0]", Matchers.equalTo("test"));
    }

    @Test
    public void testEmptyObject() {
        RestAssured.get("/json-include/my-object-empty")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(Matchers.is("{}"));
    }
}
