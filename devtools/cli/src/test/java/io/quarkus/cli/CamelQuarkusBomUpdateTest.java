package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.platform.tools.ToolsConstants;
import picocli.CommandLine;

/**
 * Integration test for Camel Quarkus BOM recognition in the update command.
 * Tests that projects using only the Camel Quarkus BOM (without quarkus-bom)
 * can be properly updated.
 */
public class CamelQuarkusBomUpdateTest extends RegistryClientBuilderTestBase {

    @BeforeAll
    static void configureRegistryAndMavenRepo() {
        // Configure a test registry with quarkus-camel-bom under io.quarkus.platform
        // This matches the real-world setup where quarkus-camel-bom is published
        // under io.quarkus.platform (same as quarkus-bom)
        TestRegistryClientBuilder.newInstance()
                .baseDir(registryConfigDir())
                .newRegistry("registry.test.quarkus.io")
                .newPlatform("io.quarkus.platform")
                .newStream("999")
                .newRelease(getCurrentQuarkusVersion())
                .quarkusVersion(getCurrentQuarkusVersion())
                .addCoreMember()
                .alignPluginsOnQuarkusVersion()
                .addDefaultCodestartExtensions()
                .release()
                .newMember(ToolsConstants.CAMEL_QUARKUS_BOM_ARTIFACT_ID)
                .addExtension("org.apache.camel.quarkus", "camel-quarkus-core", getCurrentQuarkusVersion())
                .addExtension("org.apache.camel.quarkus", "camel-quarkus-platform-http", getCurrentQuarkusVersion())
                .registry()
                .clientBuilder()
                .build();
    }

    @Test
    void testCamelQuarkusBomOnlyProjectCanBeUpdated() throws Exception {
        final CliDriver.Result createResult = run(workDir(), "create", "app", "camel-quarkus-only");
        assertThat(createResult.exitCode)
                .withFailMessage("Expected OK return code. Got: " + createResult.exitCode + "\nOutput:\n" + createResult.stdout)
                .isEqualTo(CommandLine.ExitCode.OK);
        assertThat(createResult.stdout)
                .withFailMessage("Expected confirmation that the project has been created." + createResult)
                .contains("SUCCESS");

        final Path projectDir = workDir().resolve("camel-quarkus-only");
        final Path pomFile = projectDir.resolve("pom.xml");

        // Modify pom.xml to simulate a Camel-only project (only camel-quarkus-bom, no quarkus-bom)
        // This is the actual use case: projects that use ONLY camel-quarkus-bom
        String pomContent = CliDriver.readFileAsString(pomFile);

        // Change the platform artifact-id from quarkus-bom to quarkus-camel-bom
        // Keep the group-id as io.quarkus.platform (which is where camel-quarkus-bom is actually published)
        pomContent = pomContent.replace(
                "<quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>",
                "<quarkus.platform.artifact-id>quarkus-camel-bom</quarkus.platform.artifact-id>");

        // Remove standard Quarkus dependencies that aren't in our test quarkus-camel-bom
        // Keep only dependencies that our test BOM manages
        pomContent = pomContent.replaceAll("(?s)<dependency>\\s*<groupId>io\\.quarkus</groupId>.*?</dependency>\\s*", "");
        pomContent = pomContent.replaceAll("(?s)<dependency>\\s*<groupId>io\\.rest-assured</groupId>.*?</dependency>\\s*", "");

        Files.writeString(pomFile, pomContent);

        // The critical test: update command should work with quarkus-camel-bom
        // Before fix #54315, this would fail with "The project state is missing the Quarkus platform BOM"
        final CliDriver.Result updateResult = run(projectDir, "update", "-N");

        assertThat(updateResult.getExitCode())
                .withFailMessage("Update command should succeed with quarkus-camel-bom as the platform BOM. " +
                        "Before fix #54315, this would fail with 'missing Quarkus platform BOM' error.")
                .isEqualTo(0);
    }

}
