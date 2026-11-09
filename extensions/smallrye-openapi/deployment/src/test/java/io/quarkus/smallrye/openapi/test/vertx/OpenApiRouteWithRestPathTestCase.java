package io.quarkus.smallrye.openapi.test.vertx;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.test.jaxrs.OpenApiResourceWithNoTag;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

class OpenApiRouteWithRestPathTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiRoute.class, OpenApiResourceWithNoTag.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/base\nquarkus.rest.path=/api"),
                            "application.properties"));

    @Test
    void testRestPathNotAppliedToRoutes() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .body("paths", Matchers.hasKey("/base/resource"))
                .body("paths", Matchers.not(Matchers.hasKey("/base/api/resource")))
                // quarkus.rest.path still applies to JAX-RS resources in the same application
                .body("paths", Matchers.hasKey("/base/api/resource/auto"))
                .body("paths", Matchers.not(Matchers.hasKey("/base/resource/auto")));
    }
}
