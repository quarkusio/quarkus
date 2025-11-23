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
    public void accessResourceBundleEn() {
        when()
                .get("/native-image-metadata-parser/access-resource-bundle/en")
                .then()
                .body(is("Hello World"));
    }

    @Test
    public void accessResourceBundleFr() {
        when()
                .get("/native-image-metadata-parser/access-resource-bundle/fr")
                .then()
                .body(is("Bonjour le monde"));
    }
}
