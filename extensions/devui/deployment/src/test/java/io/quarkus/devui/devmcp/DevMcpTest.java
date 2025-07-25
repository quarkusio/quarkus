package io.quarkus.devui.devmcp;

import java.util.function.Supplier;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.testrunner.HelloResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class DevMcpTest {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class)
                            .add(new StringAsset("quarkus.dev-mcp.enabled=true"),
                                    "application.properties");
                }
            });

    @Test
    public void testInitialize() {
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
                .statusCode(200)
                .log().all()
                .body("id", CoreMatchers.equalTo(1))
                .body("result.serverInfo.name", CoreMatchers.equalTo("Quarkus Dev MCP"));

    }

    @Test
    public void testInitializedNotification() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "method": "notifications/initialized"
                    }
                """;

        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(202);
    }

    @Test
    public void testToolsList() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 3,
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
                .log().all()
                .body("id", CoreMatchers.equalTo(3))
                .body("jsonrpc", CoreMatchers.equalTo("2.0"))
                .body("result.tools.name", CoreMatchers.hasItem("devui-logstream_getLogger"));

    }

    @Test
    public void testToolsCall() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 4,
                      "method": "tools/call",
                      "params": {
                        "name": "devui-logstream_getLogger",
                        "arguments": {
                          "loggerName": "io.quarkus"
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
                .statusCode(200)
                .log().all()
                .body("id", CoreMatchers.equalTo(4))
                .body("jsonrpc", CoreMatchers.equalTo("2.0"))
                .body("result.content.type", CoreMatchers.hasItem("text"))
                .body("result.content.text", CoreMatchers.notNullValue());

    }

    @Test
    public void testResourcesList() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 5,
                      "method": "resources/list"
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
                .log().all()
                .body("id", CoreMatchers.equalTo(5))
                .body("jsonrpc", CoreMatchers.equalTo("2.0"))
                .body("result.resources.name", CoreMatchers.hasItem("devui/extensions"))
                .body("result.resources.uri", CoreMatchers.hasItem("quarkus://resource/build-time/devui/extensions"));

    }

    @Test
    public void testResourcesRead() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 6,
                      "method": "resources/read",
                      "params": {
                           "uri": "quarkus://resource/build-time/devui/extensions"
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
                .statusCode(200)
                .log().all()
                .body("id", CoreMatchers.equalTo(6))
                .body("jsonrpc", CoreMatchers.equalTo("2.0"))
                .body("result.contents.uri", CoreMatchers.hasItem("quarkus://resource/build-time/devui/extensions"))
                .body("result.contents.text", CoreMatchers.notNullValue());

    }

}
