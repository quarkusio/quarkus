package io.quarkus.devtools.project.create;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;

public class RecommendStreamsFromScenario3Test extends RecommendStreamsFromScenarioBase {

    @Override
    protected void setDownstreamRegistryExtraOptions(TestRegistryClientBuilder.TestRegistryBuilder registry) {
        registry.setOffering("offering-a")
                .setRecommendStreamsFrom(DOWNSTREAM_PLATFORM_KEY, "2.2");
    }

    @Test
    public void createOfferingA() throws Exception {
        final Path projectDir = newProjectDir(getClass().getSimpleName());
        createProject(projectDir, List.of("ext-a", "ext-b"));

        assertModel(projectDir,
                List.of(mainPlatformBom(),
                        platformMemberBomCoords("acme-a-bom"),
                        ArtifactCoords.pom(UPSTREAM_PLATFORM_KEY, "acme-b-bom", "2.2.2")),
                List.of(ArtifactCoords.jar("io.acme", "ext-a", null),
                        ArtifactCoords.jar("io.acme", "ext-b", null)),
                Map.of("quarkus.platform.group-id", DOWNSTREAM_PLATFORM_KEY,
                        "quarkus.platform.artifact-id", "quarkus-bom",
                        "quarkus.platform.version", "2.2.2.downstream"));
    }
}
