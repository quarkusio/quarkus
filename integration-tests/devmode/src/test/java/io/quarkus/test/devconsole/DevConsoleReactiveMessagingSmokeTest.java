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
public class DevConsoleReactiveMessagingSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyProcessor.class, DummyConnector.class)
                    .addAsResource(
                            new StringAsset(
                                    "mp.messaging.incoming.input.connector=dummy\n"
                                            + "mp.messaging.incoming.input.values=hallo"),
                            "application.properties"));

    @Test
    public void testProcessor() {
        RestAssured.get("q/dev/io.quarkus.quarkus-smallrye-reactive-messaging/channels")
                .then()
                .statusCode(200).body(Matchers.containsString("io.quarkus.test.devconsole.MyProcessor#process"));
    }

}
