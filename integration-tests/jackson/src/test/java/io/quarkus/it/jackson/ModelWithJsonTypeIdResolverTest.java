package io.quarkus.it.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.it.jackson.model.ModelWithJsonTypeIdResolver;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ModelWithJsonTypeIdResolverTest {

    static List<ModelWithJsonTypeIdResolver> typeIds() {
        return Arrays.asList(
                new ModelWithJsonTypeIdResolver.SubclassOne(),
                new ModelWithJsonTypeIdResolver.SubclassTwo());
    }

    @ParameterizedTest
    @MethodSource("typeIds")
    public void testPost(ModelWithJsonTypeIdResolver instance) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        given()
                .contentType("application/json")
                .body(objectMapper.writeValueAsString(instance))
                .when().post("/typeIdResolver")
                .then()
                .statusCode(200)
                .body(is(instance.getType()));
    }

    static List<Arguments> types() {
        return Arrays.asList(
                Arguments.arguments("one", "ONE"),
                Arguments.arguments("two", "TWO"));
    }

    @ParameterizedTest
    @MethodSource("types")
    public void testGets(String endpoint, String expectedType) {
        given().when().get("/typeIdResolver/" + endpoint)
                .then()
                .statusCode(200)
                .body("type", equalTo(expectedType));
    }
}
