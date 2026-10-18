package io.quarkus.devui.devmcp;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.testrunner.HelloResource;
import io.quarkus.devui.testrunner.SimpleET;
import io.quarkus.devui.testrunner.UnitET;
import io.quarkus.devui.testrunner.UnitService;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class DevMcpTestToolsTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class, UnitService.class)
                    .add(new StringAsset(
                            "quarkus.test.continuous-testing=disabled\nquarkus.console.basic=true\nquarkus.console.disable-input=true\nquarkus.dev-mcp.enabled=true\n"),
                            "application.properties"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleET.class, UnitET.class));

    @Test
    public void testNewTestToolsInToolsList() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "initialize",
                      "params": {
                        "protocolVersion": "2025-03-26",
                        "capabilities": {},
                        "clientInfo": {
                          "name": "JUnitTestClient",
                          "version": "1.0.0"
                        }
                      }
                    }
                """;

        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(200);

        jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/list"
                    }
                """;

        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(200)
                .body("result.tools.name", CoreMatchers.hasItem("devui-testing_runTests"))
                .body("result.tools.name", CoreMatchers.hasItem("devui-testing_runTest"))
                .body("result.tools.name", CoreMatchers.hasItem("devui-testing_runAffectedTests"));
    }

    @Test
    public void testRunTestsViaMcp() {
        // Initialize first
        String initBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "initialize",
                      "params": {
                        "protocolVersion": "2025-03-26",
                        "capabilities": {},
                        "clientInfo": {
                          "name": "JUnitTestClient",
                          "version": "1.0.0"
                        }
                      }
                    }
                """;

        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(initBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(200);

        // Call runTests
        String callBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 3,
                      "method": "tools/call",
                      "params": {
                        "name": "devui-testing_runTests",
                        "arguments": {}
                      }
                    }
                """;

        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(callBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(200)
                .body("id", CoreMatchers.equalTo(3))
                .body("result.content[0].type", CoreMatchers.equalTo("text"))
                .body("result.content[0].text", CoreMatchers.containsString("\"passedCount\""))
                .body("result.content[0].text", CoreMatchers.containsString("\"failedCount\""))
                .body("result.content[0].text", CoreMatchers.containsString("\"passing\""));
    }
}
