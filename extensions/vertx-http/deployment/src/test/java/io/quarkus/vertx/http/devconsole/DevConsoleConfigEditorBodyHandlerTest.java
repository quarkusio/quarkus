package io.quarkus.vertx.http.devconsole;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * This tests that the dev console still works with this extension present (i.e. with a RequireBodyHandlerBuildItem being
 * produced)
 * <p>
 * The config editor is a deployment side handler, so this test that the deployment side functions of the dev console
 * handle body handlers correctly.
 */
public class DevConsoleConfigEditorBodyHandlerTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(BodyHandlerBean.class));

    public DevConsoleConfigEditorBodyHandlerTest() {
        super("devui-configuration");
    }

    @Test
    public void testChangeHttpRoute() throws Exception {
        RestAssured.with()
                .get("q/arc/beans")
                .then()
                .statusCode(200);
        super.executeJsonRPCMethod("updateProperty",
                Map.of(
                        "name", "quarkus.http.root-path",
                        "value", "/foo"));
        RestAssured.with()
                .get("q/arc/beans")
                .then()
                .statusCode(404);
        RestAssured.with()
                .get("foo/q/arc/beans")
                .then()
                .statusCode(200);

    }

}
