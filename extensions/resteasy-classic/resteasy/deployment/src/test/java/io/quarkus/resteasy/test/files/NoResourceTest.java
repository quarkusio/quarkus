package io.quarkus.resteasy.test.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test that we start even without resources or files
 */
public class NoResourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    public void test() {
        RestAssured.get("/").then()
                .statusCode(404);
    }
}
