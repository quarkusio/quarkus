package io.quarkus.test.devconsole;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Note that this test cannot be placed under the relevant {@code -deployment} module because then the DEV UI processor would
 * not be able to locate the template resources correctly.
 */
public class DevConsoleQuteSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(new StringAsset(
                    "{hello}"),
                    "templates/hello.txt"));

    @Test
    public void testTemplates() {
        RestAssured.get("q/dev/io.quarkus.quarkus-qute/templates")
                .then()
                .statusCode(200).body(Matchers.containsString("hello.txt"));
    }

}
