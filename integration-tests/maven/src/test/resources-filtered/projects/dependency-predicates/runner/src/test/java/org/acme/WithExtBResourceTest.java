package org.acme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@Tag("withExtB")
@QuarkusTest
public class WithExtBResourceTest {

    @BeforeAll
    static void clean() throws IOException {
        // Ensure that there is no remaining test model
        Files.deleteIfExists(Paths.get("target/quarkus/bootstrap/test-app-model.dat"));
    }

    @Test
    void testContainsExtBEndpoint() {
        given()
            .when().get("/containsExtB")
            .then()
            .statusCode(200)
            .body(is("true"));
    }
}
