package io.quarkus.it.main;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ParameterizedHamcrestTestCase {

    @ParameterizedTest
    @MethodSource("args")
    void testParameterized(Map<String, Matcher<?>> tests) {
        tests.forEach(this::assertResponse);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testManually() {
        args()
                .flatMap(arguments -> ((Map<String, Matcher<?>>) arguments.get()[0]).entrySet().stream())
                .forEach(arguments -> assertResponse(arguments.getKey(), arguments.getValue()));
    }

    void assertResponse(String path, Matcher<?> matcher) {
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(matcher);
    }

    static Stream<Arguments> args() {
        return Stream.of(Arguments.of(Collections.singletonMap("/configuration-properties", is("Hello!"))));
    }
}
