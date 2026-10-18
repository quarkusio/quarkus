package io.quarkus.it.extension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClasspathTestCase {

    private static final String CLASS_FILE = "io/quarkus/it/extension/ClasspathTestEndpoint.class";
    private static final String RESOURCE_FILE = "some-resource-for-classpath-test.txt";

    @Test
    public void testAugmentationMainClassNoDuplicate() {
        given().param("resourceName", CLASS_FILE)
                .param("phase", "augmentation")
                .when().get("/core/classpath").then()
                .body(is("OK"));
    }

    @Test
    public void testAugmentationMainResourceNoDuplicate() {
        given().param("resourceName", RESOURCE_FILE)
                .param("phase", "augmentation")
                .when().get("/core/classpath").then()
                .body(is("OK"));
    }

    @Test
    // Static init may happen in a container when testing a native image,
    // in which case we don't have any classpath record to check.
    @DisabledOnIntegrationTest
    public void testStaticInitMainClassNoDuplicate() {
        given().param("resourceName", CLASS_FILE)
                .param("phase", "static_init")
                .when().get("/core/classpath").then()
                .body(is("OK"));
    }

    @Test
    // Static init may happen in a container when testing a native image,
    // in which case we don't have any classpath record to check.
    @DisabledOnIntegrationTest
    public void testStaticInitMainResourceNoDuplicate() {
        given().param("resourceName", RESOURCE_FILE)
                .param("phase", "static_init")
                .when().get("/core/classpath").then()
                .body(is("OK"));
    }

    // For some reason, class files are not accessible as resources through the runtime init classloader;"
    // that's beside the point of this PR though, so we'll ignore that."
    @Test
    @DisabledOnIntegrationTest()
    public void testRuntimeInitMainClassNoDuplicate() {
        given().param("resourceName", CLASS_FILE)
                .param("phase", "runtime_init")
                .when().get("/core/classpath").then()
                .body(is("OK"));
    }

    @Test
    public void testRuntimeInitMainResourceNoDuplicate() {
        // Runtime classloader classes are stored in memory, as "quarkus:" resources, and we do not have a quarkus filesystem provider
        // at the moment, the path helper works around that by hacking/reverse engineering a file-based location
        given().param("resourceName", RESOURCE_FILE)
                .param("phase", "runtime_init")
                .when().get("/core/classpath").then()
                .body(is("OK"));
    }

}