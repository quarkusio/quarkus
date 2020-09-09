package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiHealthTestCase {
    private static final String OPEN_API_PATH = "/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.add-health=true\n"
                            + "quarkus.smallrye-openapi.store-schema-directory=target"), "application.properties"));

    @Test
    public void testOpenApiPathAccessResource() {

        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("paths", Matchers.hasKey("/health/ready"))
                .body("paths", Matchers.hasKey("/health/live"))
                .body("paths", Matchers.hasKey("/health"))
                .body("components.schemas.HealthCheckResponse.type", Matchers.equalTo("object"));

    }
}
