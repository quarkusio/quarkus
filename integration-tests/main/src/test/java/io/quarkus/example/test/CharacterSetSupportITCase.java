package io.quarkus.example.test;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.SubstrateTest;
import io.restassured.RestAssured;

@SubstrateTest
public class CharacterSetSupportITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() throws Exception {
        RestAssured.when().get("/core/charsetsupport").then()
                .body(is("OK"));
    }

}
