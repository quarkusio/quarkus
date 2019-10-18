package io.quarkus.smallrye.openapi.test;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * This test is a reproducer for https://github.com/quarkusio/quarkus/issues/4613.
 */
public class SwaggerAndOpenAPIWithCommonPrefixTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(OpenApiResource.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.path=/swagger"), "application.properties"));

    @Test
    public void shouldWorkEvenWithCommonPrefix() {
        RestAssured.when().get("/swagger-ui/index.html").then().statusCode(200).body(containsString("/swagger"));
        RestAssured.when().get("/swagger").then().statusCode(200)
                .body(containsString("/resource"), containsString("QUERY_PARAM_1"));
    }
}
