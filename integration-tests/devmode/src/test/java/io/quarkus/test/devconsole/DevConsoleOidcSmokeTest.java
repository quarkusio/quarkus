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
public class DevConsoleOidcSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    @Test
    public void testOidcProviderTemplate() {
        RestAssured.get("q/dev/io.quarkus.quarkus-oidc/provider")
                .then()
                .statusCode(200).body(Matchers.containsString("OpenId Connect Dev Console"));
    }
}
