package io.quarkus.cyclonedx.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Dependency;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;
import io.quarkus.sbom.ComponentDependencies;
import io.quarkus.sbom.ComponentDescriptor;
import io.quarkus.sbom.CoreSbomContributionConfig;
import io.quarkus.sbom.Purl;
import io.quarkus.sbom.SbomContribution;

class CycloneDxSbomGeneratorTest {

    @Test
    void topLevelComponentsLinkedToMainComponent() {
        // Core contribution via CoreSbomContributionConfig (sets mainComponentBomRef)
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0",
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.0.0")));
        ResolvedDependency restDep = resolvedDep("io.quarkus", "quarkus-rest", "3.0.0", List.of());

        SbomContribution coreContribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(restDep)
                .toSbomContribution();

        String mainBomRef = coreContribution.mainComponentBomRef();

        // Extension contribution: 2 npm packages, 1 top-level + 1 transitive
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .setTopLevel(true)
                .build();
        ComponentDescriptor jsTokens = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "js-tokens", "4.0.0"))
                .build();
        SbomContribution extensionContribution = SbomContribution.of(
                List.of(react, jsTokens),
                List.of(ComponentDependencies.of(
                        react.getBomRef(),
                        List.of(jsTokens.getBomRef()))));

        // Generate SBOM text (JSON)
        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(coreContribution, extensionContribution))
                .generateText();

        assertThat(result).hasSize(1);

        // Parse the output back to verify dependency structure
        Bom bom = parseBom(result.get(0));

        // Find main component dependency entry
        Dependency mainDep = bom.getDependencies().stream()
                .filter(d -> d.getRef().equals(mainBomRef))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Main component not in dependency graph"));

        List<String> mainDependsOn = mainDep.getDependencies().stream()
                .map(Dependency::getRef)
                .toList();

        // Should include react (top-level extension component)
        assertThat(mainDependsOn).contains(react.getBomRef());
        // Should NOT include js-tokens (transitive, not top-level)
        assertThat(mainDependsOn).doesNotContain(jsTokens.getBomRef());
    }

    @Test
    void noMainComponentSkipsTopLevelLinking() {
        // Extension-only contribution with no main component
        ComponentDescriptor react = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "react", "18.0.0"))
                .setTopLevel(true)
                .build();
        SbomContribution contribution = SbomContribution.ofComponents(List.of(react));

        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setFormat("json")
                .setContributions(List.of(contribution))
                .generateText();

        assertThat(result).hasSize(1);
        // Should not crash — no main component to link to
        assertThat(result.get(0)).contains("react");
    }

    private static Bom parseBom(String json) {
        try {
            return new org.cyclonedx.parsers.JsonParser().parse(json.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BOM JSON", e);
        }
    }

    private static io.quarkus.maven.dependency.ResolvedDependency resolvedDep(
            String groupId, String artifactId, String version,
            List<ArtifactCoords> dependencies) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(groupId)
                .setArtifactId(artifactId)
                .setVersion(version)
                .setResolvedPaths(PathList.of())
                .setDependencies(dependencies)
                .setRuntimeCp()
                .build();
    }
}
