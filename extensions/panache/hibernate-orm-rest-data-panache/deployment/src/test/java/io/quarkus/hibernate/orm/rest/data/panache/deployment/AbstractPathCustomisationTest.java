package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.restassured.response.Response;

public abstract class AbstractPathCustomisationTest {

    @ParameterizedTest
    @ArgumentsSource(TestArgumentsProvider.class)
    void testGet(String path, String accept) {
        given().accept(accept)
                .when().get(path)
                .then().statusCode(200);
    }

    @ParameterizedTest
    @ArgumentsSource(TestArgumentsProvider.class)
    void testCreateAndDelete(String path, String accept) {
        String id = "test-" + Objects.hash(path, accept);
        Response response = given().body("{\"id\": \"" + id + "\", \"name\": \"test collection\"}")
                .and().contentType("application/json")
                .and().accept(accept)
                .when().post(path)
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(201);
        when().delete(path + "/" + id)
                .then().statusCode(204);
    }

    @ParameterizedTest
    @ArgumentsSource(TestArgumentsProvider.class)
    void testUpdate(String path, String accept) {
        given().body("{\"id\": \"empty\", \"name\": \"updated collection\"}")
                .and().contentType("application/json")
                .and().accept(accept)
                .when().put(path + "/empty")
                .then().statusCode(204);
    }

    static class TestArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("/collections", "application/json"),
                    Arguments.of("/custom-collections/api", "application/json"),
                    Arguments.of("/collections", "application/hal+json"),
                    Arguments.of("/custom-collections/api", "application/hal+json"));
        }
    }
}
