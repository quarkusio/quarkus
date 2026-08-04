package io.quarkus.it.extension;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
public class PublicMethodsReflectionInGraalITCase {

    @Test
    public void testPublicMethodsReflectionOnEntityFromServlet() {
        RestAssured.when().get("/core/reflection/public-methods").then()
                .body(is("OK"));
    }

}
