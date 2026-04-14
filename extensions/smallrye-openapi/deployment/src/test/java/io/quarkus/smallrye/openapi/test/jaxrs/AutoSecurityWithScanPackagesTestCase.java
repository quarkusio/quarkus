package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Verify that setting {@code mp.openapi.scan.packages} does not cause a build failure
 * when security annotations like {@code @PermissionsAllowed} are not in the filtered index.
 *
 * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/53471">#53471</a>.
 */
class AutoSecurityWithScanPackagesTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ResourceBean.class, OpenApiResourceSecuredAtMethodLevel.class)
                    .addAsResource(
                            new StringAsset("""
                                    quarkus.smallrye-openapi.security-scheme=jwt
                                    mp.openapi.scan.packages=io.quarkus.smallrye.openapi.test.jaxrs
                                    """),
                            "application.properties"));

    @Test
    void testOpenApiGeneratedWithScanPackages() {
        RestAssured.given()
                .header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths", Matchers.notNullValue());
    }
}
