package io.quarkus.devui.devmcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.testrunner.HelloResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class DevMcpTest {

    private static Path TEMP_HOME;
    private static String OLD_USER_HOME;

    @RegisterExtension
    static final QuarkusDevModeTest test = createDevModeTest();

    private static QuarkusDevModeTest createDevModeTest() {
        try {
            TEMP_HOME = Files.createTempDirectory("devmcp-home-");
            Path quarkusDir = TEMP_HOME.resolve(".quarkus");
            Files.createDirectories(quarkusDir);
            Path props = quarkusDir.resolve("dev-mcp.properties");
            Files.writeString(props, "enabled=true\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            OLD_USER_HOME = System.getProperty("user.home");
            System.setProperty("user.home", TEMP_HOME.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up Dev MCP test HOME", e);
        }

        return new QuarkusDevModeTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                        .addClasses(HelloResource.class));
    }

    @AfterAll
    static void cleanupHome() {
        // Restore original user.home
        if (OLD_USER_HOME != null) {
            System.setProperty("user.home", OLD_USER_HOME);
        }
        if (TEMP_HOME != null) {
            try {
                Files.walk(TEMP_HOME)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            } catch (IOException ignored) {
            }
        }
    }

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
    public void testToolsCallWithEmptyArgs() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 4,
                      "method": "tools/call",
                      "params": {
                        "name": "devui-logstream_getLoggers",
                        "arguments": {}
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
                .body("result.resources.name", CoreMatchers.hasItem("devui_extensions"))
                .body("result.resources.uri", CoreMatchers.hasItem("quarkus://resource/build-time/devui_extensions"));

    }

    @Test
    public void testResourcesRead() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 6,
                      "method": "resources/read",
                      "params": {
                           "uri": "quarkus://resource/build-time/devui_extensions"
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
                .body("result.contents.uri", CoreMatchers.hasItem("quarkus://resource/build-time/devui_extensions"))
                .body("result.contents.text", CoreMatchers.notNullValue());

    }

    @Test
    public void testResourcesReadWithInvalidUri() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 7,
                      "method": "resources/read",
                      "params": {
                           "uri": "quarkus://"
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
                .body("id", CoreMatchers.equalTo(7))
                .body("jsonrpc", CoreMatchers.equalTo("2.0"))
                .body("error.code", CoreMatchers.equalTo(-32603))
                .body("error.message", CoreMatchers.containsString("Invalid resource URI"));

    }

}
