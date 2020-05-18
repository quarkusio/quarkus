package io.quarkus.it.picocli;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class PicocliTest {

    @Test
    public void testCommands() {
        RestAssured.when().get("/picocli/test").then().body(is("OK"));
    }

}
