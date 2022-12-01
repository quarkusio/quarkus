package io.quarkus.smallrye.health.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class HealthOpenAPITest {

    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BasicHealthCheck.class, OpenApiRoute.class)
                    .addAsResource(new StringAsset("quarkus.health.openapi.included=true\n"
                            + "quarkus.smallrye-openapi.store-schema-directory=target"), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testOpenApiPathAccessResource() {

        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.hasKey("/q/health/ready"))
                .body("paths", Matchers.hasKey("/q/health/live"))
                .body("paths", Matchers.hasKey("/q/health/started"))
                .body("paths", Matchers.hasKey("/q/health"))
                .body("components.schemas.HealthCheckResponse.type", Matchers.equalTo("object"));

    }

}
