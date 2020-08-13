package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ContextPropagationTestCase {

    @ParameterizedTest
    @MethodSource("endpoints")
    public void testContextPropagation(String endpoint) throws Exception {
        RestAssured.when().get(endpoint).then().body(is("OK"));
    }

    private static List<String> endpoints() {
        return Arrays.asList("/context-propagation/managed-executor/created",
                "/context-propagation/managed-executor/obtained",
                "/context-propagation/thread-context",
                "/context-propagation-mutiny");
    }
}
