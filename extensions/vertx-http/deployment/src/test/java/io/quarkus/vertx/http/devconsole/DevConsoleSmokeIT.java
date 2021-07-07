package io.quarkus.vertx.http.devconsole;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * This needs to be an integration test so the pom.properties has already been created
 */
public class DevConsoleSmokeIT {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void testDevConsoleNotBroken() {
        RestAssured.with()
                .get("q/dev")
                .then()
                .statusCode(200).body(Matchers.containsString("Dev UI"));

    }
}
