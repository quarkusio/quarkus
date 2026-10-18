package io.quarkus.devui.devmcp;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.testrunner.HelloResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class DevMcpCorsTest {

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
            throw new RuntimeException("Failed to set up Dev MCP CORS test HOME", e);
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
    public void testPreflightNonLocalhostOriginRejected() {
        String methods = "GET,POST";
        RestAssured.given()
                .header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", methods)
                .when()
                .options("/q/dev-mcp").then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue())
                .header("Access-Control-Allow-Methods", nullValue())
                .body(emptyOrNullString());
    }

    @Test
    public void testSimpleRequestNonLocalhostOriginRejected() {
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "initialize",
                      "params": {
                        "protocolVersion": "2025-03-26",
                        "capabilities": {},
                        "clientInfo": {
                          "name": "EvilClient",
                          "version": "1.0.0"
                        }
                      }
                    }
                """;

        RestAssured.given()
                .header("Origin", "https://evil.example.com")
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/q/dev-mcp").then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue())
                .body(emptyOrNullString());
    }

    @Test
    public void testPreflightLocalhostOriginAllowed() {
        String origin = "http://localhost:8080";
        String methods = "GET,POST";
        RestAssured.given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .when()
                .options("/q/dev-mcp").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", methods)
                .body(emptyOrNullString());
    }
}
