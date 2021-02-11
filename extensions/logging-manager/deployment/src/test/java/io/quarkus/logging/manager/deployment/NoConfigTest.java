package io.quarkus.logging.manager.deployment;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class NoConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void shouldUseDefaultConfig() {
        RestAssured.when().get("/q/logging-manager-ui").then().statusCode(200).body(containsString("Logging manager"));
        RestAssured.when().get("/q/logging-manager-ui/index.html").then().statusCode(200)
                .body(containsString("Logging manager"));
    }
}
