package io.quarkus.it.extension;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BackslashResourceIT extends BackslashResourceTest {
    /**
     * Verifies that files NOT matching the glob pattern are excluded in native mode.
     */
    @Test
    @EnabledIfSystemProperty(named = "quarkus.test.native", matches = "true")
    public void testNegativeExcludedFile() {
        when()
                .get("/core/glob-resource?path=config/app/other/excluded.properties")
                .then()
                .body(is("Resource not found"));
    }
}
