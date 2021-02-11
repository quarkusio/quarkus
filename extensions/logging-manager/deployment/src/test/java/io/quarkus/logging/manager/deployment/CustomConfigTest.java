package io.quarkus.logging.manager.deployment;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.logging-manager.ui.root-path=/custom"), "application.properties"));

    @Test
    public void shouldUseCustomConfig() {
        RestAssured.when().get("/q/custom").then().statusCode(200).body(containsString("Logging manager"));
        RestAssured.when().get("/q/custom/index.html").then().statusCode(200).body(containsString("Logging manager"));
    }
}
