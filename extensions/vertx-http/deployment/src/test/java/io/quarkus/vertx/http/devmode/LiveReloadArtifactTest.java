package io.quarkus.vertx.http.devmode;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * tests the reload-dependencies option
 */
public class LiveReloadArtifactTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.class-loading.reloadable-artifacts=io.vertx:vertx-web-client\n"),
                            "application.properties")
                    .addClasses(LiveReloadEndpoint.class));

    @Test
    public void test() {
        String firstClassToString = RestAssured.get("/test").then().statusCode(200).extract().body().asString();
        test.modifySourceFile(LiveReloadEndpoint.class, s -> s.replace("\"/test\"", "\"/test2\""));

        String secondClassToString = RestAssured.get("/test2").then().statusCode(200).extract().body().asString();
        Assertions.assertNotEquals(firstClassToString, secondClassToString);
    }

}
