package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomHttpRootTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/foo"), "application.properties"));

    @Test
    public void shouldUseCustomConfig() {
        RestAssured.when().get("/swagger-ui").then().statusCode(200).body(containsString("/foo/openapi"));
        RestAssured.when().get("/swagger-ui/index.html").then().statusCode(200).body(containsString("/foo/openapi"));
    }
}
