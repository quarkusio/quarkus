package io.quarkus.devui.devmcp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.testrunner.HelloResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/**
 * Tests that extension skill resources (from {@code META-INF/quarkus-skill.md} files)
 * are correctly composed and exposed via the Dev MCP server.
 */
public class SkillResourceTest {

    private static Path TEMP_HOME;
    private static String OLD_USER_HOME;

    @RegisterExtension
    static final QuarkusDevModeTest test = createDevModeTest();

    private static QuarkusDevModeTest createDevModeTest() {
        try {
            TEMP_HOME = Files.createTempDirectory("devmcp-skill-test-");
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
    public void testSkillResourcesAppearInList() {
        // ArC is always on the classpath and has a quarkus-skill.md file,
        // so we should see at least one skills- resource
        String jsonBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "resources/list"
                    }
                """;

        ValidatableResponse response = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("result.resources.name", hasItem(startsWith("skills-")));

        // Verify the skill resource has a description
        List<String> descriptions = response.extract()
                .jsonPath()
                .getList("result.resources.findAll { it.name.startsWith('skills-') }.description");
        org.junit.jupiter.api.Assertions.assertFalse(descriptions.isEmpty(),
                "Should have at least one skill resource");
        for (String desc : descriptions) {
            org.junit.jupiter.api.Assertions.assertTrue(
                    desc.contains("Coding skill and guidelines"),
                    "Skill resource description should contain 'Coding skill and guidelines', got: " + desc);
        }
    }

    @Test
    public void testSkillResourceCanBeRead() {
        // First, find a skill resource URI
        String listBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "resources/list"
                    }
                """;

        String skillUri = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(listBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("result.resources.find { it.name.startsWith('skills-') }.uri");

        org.junit.jupiter.api.Assertions.assertNotNull(skillUri,
                "Should find at least one skill resource URI");

        // Now read the skill resource
        String readBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 3,
                      "method": "resources/read",
                      "params": {
                           "uri": "%s"
                      }
                    }
                """.formatted(skillUri);

        String content = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(readBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(200)
                .body("id", equalTo(3))
                .body("result.contents", notNullValue())
                .body("result.contents", hasSize(1))
                .body("result.contents[0].uri", equalTo(skillUri))
                .extract()
                .jsonPath()
                .getString("result.contents[0].text");

        org.junit.jupiter.api.Assertions.assertNotNull(content, "Skill resource content should not be null");

        // The composed content should contain the extension name as a header
        // and the "Patterns and Guidelines" section from the skill file
        org.junit.jupiter.api.Assertions.assertTrue(
                content.contains("Patterns and Guidelines"),
                "Skill content should contain 'Patterns and Guidelines' section, got: "
                        + content.substring(0, Math.min(200, content.length())));
    }

    @Test
    public void testSkillResourceHasMarkdownMimeType() {
        String listBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 4,
                      "method": "resources/list"
                    }
                """;

        String mimeType = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(listBody)
                .when()
                .post("/q/dev-mcp")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("result.resources.find { it.name.startsWith('skills-') }.mimeType");

        org.junit.jupiter.api.Assertions.assertEquals("text/markdown", mimeType,
                "Skill resources should have text/markdown MIME type");
    }
}
