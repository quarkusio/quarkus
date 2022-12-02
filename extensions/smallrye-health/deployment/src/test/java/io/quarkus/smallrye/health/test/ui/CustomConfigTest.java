package io.quarkus.smallrye.health.test.ui;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.smallrye-health.ui.root-path=/custom"), "application.properties"));

    @Test
    public void shouldUseCustomConfig() {
        RestAssured.when().get("/custom").then().statusCode(200).body(containsString("SmallRye Health"));
        RestAssured.when().get("/custom/index.html").then().statusCode(200).body(containsString("SmallRye Health"));
    }
}
