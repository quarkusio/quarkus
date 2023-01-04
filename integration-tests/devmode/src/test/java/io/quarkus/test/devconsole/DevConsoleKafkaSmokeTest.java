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
public class DevConsoleKafkaSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(
                    (jar) -> jar.addAsResource(new StringAsset("quarkus.http.root-path=testing"), "application.properties"));

    @Test
    public void testServices() {
        RestAssured.get("testing/q/dev/io.quarkus.quarkus-kafka-client/kafka-dev-ui")
                .then()
                .statusCode(200).body(Matchers.containsString("Kafka Dev UI"));
    }

}
