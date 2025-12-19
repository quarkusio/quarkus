package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * This test does not configure the recommend-streams-from but tests the default setup
 */
public class RecommendStreamsFromScenario1Test extends RecommendStreamsFromScenarioBase {

    @Test
    public void createOfferingA() throws Exception {
        final Path projectDir = newProjectDir(getClass().getSimpleName());
        createProject(projectDir, List.of("ext-a", "ext-b"));

        assertModel(projectDir,
                List.of(mainPlatformBom(),
                        platformMemberBomCoords("acme-a-bom"),
                        platformMemberBomCoords("acme-b-bom")),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null),
                        ArtifactCoords.jar("io.acme", "ext-b", null)),
                Map.of("quarkus.platform.group-id", DOWNSTREAM_PLATFORM_KEY,
                        "quarkus.platform.artifact-id", "quarkus-bom",
                        "quarkus.platform.version", "2.2.2.downstream"));
    }
}
