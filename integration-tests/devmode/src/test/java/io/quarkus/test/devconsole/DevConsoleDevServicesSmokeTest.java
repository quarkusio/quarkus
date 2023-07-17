package io.quarkus.test.devconsole;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Note that this test cannot be placed under the relevant {@code -deployment} module because then the DEV UI processor would
 * not be able to locate the template resources correctly.
 */
public class DevConsoleDevServicesSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    @Test
    public void testDevConsoleNotBroken() {
        RestAssured.with()
                .get("q/dev-v1")
                .then()
                .statusCode(200).body(Matchers.containsString("Dev Services"));

        RestAssured.with()
                .get("q/dev-v1/io.quarkus.quarkus-vertx-http/dev-services")
                .then()
                .statusCode(200).body(Matchers.containsString("Dev Services"));

    }
}
