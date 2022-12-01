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
public class DevConsoleResteastyReactiveSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(MyResource.class));

    @Test
    public void testTemplates() {
        RestAssured.get("q/dev/io.quarkus.quarkus-resteasy-reactive/endpoints")
                .then()
                .statusCode(200).body(Matchers.containsString("GET /me/message"));
        RestAssured.get("q/dev/io.quarkus.quarkus-resteasy-reactive/scores")
                .then()
                .statusCode(200).body(Matchers.containsString("GET /me/message"));
    }

}
