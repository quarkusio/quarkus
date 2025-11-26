package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.deployment.filter.OperationFilter;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Verify that method reference extensions are not added to the OpenAPI model when the
 * {@link OperationFilter} is not in use (disabled via several configuration properties).
 */
class DisabledOperationFilterTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Endpoint.class)
                    .add(new StringAsset("""
                            quarkus.smallrye-openapi.auto-add-operation-summary=false
                            quarkus.smallrye-openapi.auto-add-tags=false
                            quarkus.smallrye-openapi.auto-add-security-requirement=false
                            """), "application.properties"));

    @Path("/api")
    public static class Endpoint {
        @Path("/op1")
        @GET
        public String op1() {
            return null;
        }

        @Path("/op2")
        @GET
        public String op2() {
            return null;
        }
    }

    @Test
    void testMethodRefExtensionsAbsent() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .body("paths.\"/api/op1\".get", not(hasKey(OperationFilter.EXT_METHOD_REF)))
                .body("paths.\"/api/op2\".get", not(hasKey(OperationFilter.EXT_METHOD_REF)));
    }
}
