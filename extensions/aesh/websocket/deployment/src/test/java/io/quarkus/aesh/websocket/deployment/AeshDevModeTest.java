package io.quarkus.aesh.websocket.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

/**
 * Tests that modifying a {@code @CliCommand} class triggers hot-reload correctly
 * and that the aesh command discovery pipeline picks up changes in dev mode.
 * <p>
 * This test lives in the WebSocket deployment module because it needs an HTTP
 * server for dev mode reload triggering, and the WebSocket module already
 * provides one via {@code quarkus-websockets-next}.
 */
public class AeshDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(DevModeHelloCommand.class, DevModeCommandInfoEndpoint.class)
                    .add(new StringAsset("quarkus.aesh.start-console=false\n"),
                            "application.properties"));

    @Test
    public void testCommandDiscoveryAfterHotReload() {
        // Verify the initial command is discovered
        get("/dev-test/commands")
                .then()
                .statusCode(200)
                .body(containsString("hello"));

        // Modify the command name from "hello" to "greet"
        test.modifySourceFile(DevModeHelloCommand.class,
                s -> s.replace("name = \"hello\"", "name = \"greet\""));

        // Verify the new command name is picked up after hot-reload
        get("/dev-test/commands")
                .then()
                .statusCode(200)
                .body(containsString("greet"));
    }
}
