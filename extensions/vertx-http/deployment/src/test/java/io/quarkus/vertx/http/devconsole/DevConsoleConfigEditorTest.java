package io.quarkus.vertx.http.devconsole;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevConsoleConfigEditorTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    public DevConsoleConfigEditorTest() {
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

    @Test
    public void testSetEmptyValue() throws Exception {
        RestAssured.with()
                .get("q/arc/beans")
                .then()
                .statusCode(200);
        super.executeJsonRPCMethod("updateProperty",
                Map.of(
                        "name", "quarkus.http.root-path",
                        "value", ""));
        RestAssured.with()
                .get("q/arc/beans")
                .then()
                .statusCode(200);
    }
}
