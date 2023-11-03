package io.quarkus.vertx.http.devmode;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * tests the parent first artifacts option
 */
public class ParentFirstArtifactTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.class-loading.parent-first-artifacts=io.vertx:vertx-web-client\n"),
                            "application.properties")
                    .addClasses(ParentFirstEndpoint.class));

    @Test
    public void test() {
        String firstClassToString = RestAssured.get("/test").then().statusCode(200).extract().body().asString();
        Assertions.assertEquals("false", firstClassToString);
    }

}
