package io.quarkus.it.nat.annotation;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class NativeImageAnnotationsTest {
    @Test
    public void accessClasspathResource() {
        when()
                .get("/native-image-annotations/access-classpath-resource")
                .then()
                .body(is("Quarkus: Supersonic Subatomic Java."));
    }

    @Test
    public void accessResourceBundleEn() {
        when()
                .get("/native-image-annotations/access-resource-bundle/en")
                .then()
                .body(is("Hello World"));
    }

    @Test
    public void accessResourceBundleAr() {
        when()
                .get("/native-image-annotations/access-resource-bundle/ar")
                .then()
                .body(is("أهلاً بالعالم"));
    }

    @Test
    public void testProperLambdaSerialization() {
        when()
                .get("/native-image-annotations/serialize-proper")
                .then()
                .body(is("SUCCESS_PROPER"));
    }

    @Test
    public void testLegacyLambdaSerializationFails() {
        when()
                .get("/native-image-annotations/serialize-legacy")
                .then()
                .body(is("EXPECTED_FAILURE"));
    }
}
