package io.quarkus.it.nat.metadata;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test for native-image metadata parsing functionality.
 * This test verifies that META-INF/native-image files are properly parsed and included.
 */
@QuarkusTest
public class NativeImageMetadataParserTest {

    @Test
    public void testBasicFunctionality() {
        when()
                .get("/native-image-metadata-parser/test")
                .then()
                .body(is("Native image metadata parser test"));
    }

    @Test
    public void testResourceBundleAccess() {
        // Test that the -H:IncludeLocales argument from META-INF/native-image/native-image.properties works
        when()
                .get("/native-image-metadata-parser/bundle/en")
                .then()
                .body(is("Hello"));

        when()
                .get("/native-image-metadata-parser/bundle/fr")
                .then()
                .body(is("Bonjour"));
    }
}
