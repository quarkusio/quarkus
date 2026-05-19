package io.quarkus.devtools.commands.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.project.state.ProjectState;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.platform.tools.ToolsConstants;

class UpdateProjectCommandHandlerTest {

    private static final String VERSION = "3.0.0";

    @Test
    void shouldRecognizeQuarkusBomAsPlatformBom() {
        ProjectState state = projectStateWithBom(ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID, VERSION);

        ArtifactCoords result = UpdateProjectCommandHandler.getProjectQuarkusPlatformBOM(state);

        assertRecognizedBom(result, ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID, VERSION);
    }

    @Test
    void shouldRecognizeUniverseBomAsPlatformBom() {
        ProjectState state = projectStateWithBom(ToolsConstants.UNIVERSE_PLATFORM_BOM_ARTIFACT_ID, VERSION);

        ArtifactCoords result = UpdateProjectCommandHandler.getProjectQuarkusPlatformBOM(state);

        assertRecognizedBom(result, ToolsConstants.UNIVERSE_PLATFORM_BOM_ARTIFACT_ID, VERSION);
    }

    @Test
    void shouldRecognizeCamelQuarkusBomAsPlatformBom() {
        ProjectState state = projectStateWithBom(ToolsConstants.CAMEL_QUARKUS_BOM_ARTIFACT_ID, VERSION);

        ArtifactCoords result = UpdateProjectCommandHandler.getProjectQuarkusPlatformBOM(state);

        assertRecognizedBom(result, ToolsConstants.CAMEL_QUARKUS_BOM_ARTIFACT_ID, VERSION);
    }

    @Test
    void shouldHandleMultiplePlatformBoms() {
        ProjectState state = projectStateWithBoms(
                ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID,
                ToolsConstants.CAMEL_QUARKUS_BOM_ARTIFACT_ID);

        ArtifactCoords result = UpdateProjectCommandHandler.getProjectQuarkusPlatformBOM(state);

        // Returns first recognized BOM (quarkus-bom in this case)
        assertRecognizedBom(result, ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID, VERSION);
    }

    @Test
    void shouldRecognizeCamelBomWhenNoQuarkusBomPresent() {
        // Main use case: Camel Quarkus projects that don't import quarkus-bom
        ProjectState state = projectStateWithBom(ToolsConstants.CAMEL_QUARKUS_BOM_ARTIFACT_ID, "3.2.11.Final");

        ArtifactCoords result = UpdateProjectCommandHandler.getProjectQuarkusPlatformBOM(state);

        assertRecognizedBom(result, ToolsConstants.CAMEL_QUARKUS_BOM_ARTIFACT_ID, "3.2.11.Final");
    }

    @Test
    void shouldReturnNullWhenNoRecognizedBomPresent() {
        ProjectState state = projectStateWithBom("com.example", "some-other-bom", "1.0.0");

        ArtifactCoords result = UpdateProjectCommandHandler.getProjectQuarkusPlatformBOM(state);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenNoPlatformBomsPresent() {
        ProjectState state = ProjectState.builder().build();

        ArtifactCoords result = UpdateProjectCommandHandler.getProjectQuarkusPlatformBOM(state);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnFirstRecognizedBomRegardlessOfOrder() {
        // Camel BOM first, Quarkus BOM second
        ProjectState state = projectStateWithBoms(
                ToolsConstants.CAMEL_QUARKUS_BOM_ARTIFACT_ID,
                ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID);

        ArtifactCoords result = UpdateProjectCommandHandler.getProjectQuarkusPlatformBOM(state);

        // Returns first recognized BOM (camel-quarkus-bom in this case)
        assertRecognizedBom(result, ToolsConstants.CAMEL_QUARKUS_BOM_ARTIFACT_ID, VERSION);
    }

    private static ProjectState projectStateWithBom(String artifactId, String version) {
        return projectStateWithBom(ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID, artifactId, version);
    }

    private static ProjectState projectStateWithBom(String groupId, String artifactId, String version) {
        ProjectState.Builder builder = ProjectState.builder();
        builder.addPlatformBom(ArtifactCoords.pom(groupId, artifactId, version));
        return builder.build();
    }

    private static ProjectState projectStateWithBoms(String... artifactIds) {
        ProjectState.Builder builder = ProjectState.builder();
        for (String artifactId : artifactIds) {
            builder.addPlatformBom(ArtifactCoords.pom(
                    ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID, artifactId, VERSION));
        }
        return builder.build();
    }

    private static void assertRecognizedBom(ArtifactCoords result, String expectedArtifactId, String expectedVersion) {
        assertThat(result).isNotNull();
        assertThat(result.getArtifactId()).isEqualTo(expectedArtifactId);
        assertThat(result.getVersion()).isEqualTo(expectedVersion);
    }
}
