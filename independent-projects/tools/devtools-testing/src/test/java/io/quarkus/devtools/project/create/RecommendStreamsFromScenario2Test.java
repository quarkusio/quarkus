package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * This test does not configure the recommend-streams-from but configures an offering, which makes the tools give preference to
 * an older stream
 */
public class RecommendStreamsFromScenario2Test extends RecommendStreamsFromScenarioBase {

    @Override
    protected void setDownstreamRegistryExtraOptions(TestRegistryClientBuilder.TestRegistryBuilder registry) {
        registry.setOffering("offering-a");
    }

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
                        "quarkus.platform.version", "1.1.1.downstream"));
    }
}
