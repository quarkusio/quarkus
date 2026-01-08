package org.acme;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusIntegrationTest
public class GreetingResourceIT extends GreetingResourceTest {
    // Execute the same tests but in packaged mode, plus a native-image specific one.

    @DisabledIfSystemProperty(
            named = "quarkus.test.integration-test-profile",
            matches = "test-with-native-agent",
            disabledReason = "Keep org.acme.Carol unreachable to prevent the agent from detecting the access. This way we ensure that both the manual configuration and the generated one are used by GraalVM."
    )
    @Test
    public void testCarol()
    {
        given()
                .when().get("/hello/Carol")
                .then()
                .statusCode(200)
                .body(is("Hello Carol"));
    }

}
