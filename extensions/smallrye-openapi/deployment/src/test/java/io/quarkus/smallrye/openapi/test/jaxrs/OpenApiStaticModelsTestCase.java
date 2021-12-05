package io.quarkus.smallrye.openapi.test.jaxrs;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenApiStaticModelsTestCase {

    private static String directory = "META-INF/openapi/";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsManifestResource("test-openapi.yaml", "openapi/model1.yaml")
                    .addAsManifestResource("test-openapi2.yaml", "openapi/model2.yaml")
                    .addAsManifestResource("test-openapi3.yaml", "openapi/model3.yaml")
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.additional-docs-directory=" + directory),
                            "application.properties"));

    @Test
    public void testMultipleStaticFiles() throws IOException {

        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .then().body("paths.'/openapi'.get.operationId", Matchers.equalTo("someOperation"))
                .and().body("paths.'/openapi2'.get.operationId", Matchers.equalTo("someOperation2"))
                .and().body("paths.'/openapi3'.get.operationId", Matchers.equalTo("someOperation3"));
    }
}
