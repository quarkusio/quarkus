package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiWithResteasyPathTestCase {
    private static final String OPEN_API_PATH = "/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(new StringAsset("quarkus.resteasy.path=/foo/bar"),
                            "application.properties"));

    @Test
    public void testOpenApiResteasyPath() {
        RestAssured.given().queryParam("format", "JSON")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.startsWith("3.0"))
                .body("info.title", Matchers.equalTo("Generated API"))
                .body("paths", Matchers.hasKey("/foo/bar/resource"));
    }
}