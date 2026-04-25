package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

class CoreSbomContributionConfigTest {

    @Test
    void directDependenciesAreMarkedTopLevel() {
        ResolvedDependency directDep = resolvedDep("io.quarkus", "quarkus-rest", "3.0.0", List.of());
        ResolvedDependency transitiveDep = resolvedDep("io.quarkus", "quarkus-vertx", "3.0.0", List.of());
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0",
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.0.0")));

        CoreSbomContributionConfig config = CoreSbomContributionConfig.builder()
                .setMainComponent(toDescriptor(mainArtifact), mainArtifact)
                .addComponent(toDescriptor(directDep), directDep)
                .addComponent(toDescriptor(transitiveDep), transitiveDep)
                .build();

        ComponentDescriptor direct = findByName(config, "quarkus-rest");
        assertThat(direct.isTopLevel())
                .as("Direct dependency should be top-level")
                .isTrue();

        ComponentDescriptor transitive = findByName(config, "quarkus-vertx");
        assertThat(transitive.isTopLevel())
                .as("Transitive dependency should not be top-level")
                .isFalse();
    }

    @Test
    void mainComponentIsNotMarkedTopLevel() {
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        CoreSbomContributionConfig config = CoreSbomContributionConfig.builder()
                .setMainComponent(toDescriptor(mainArtifact), mainArtifact)
                .build();

        assertThat(config.getMainComponent().isTopLevel())
                .as("Main component itself should not be top-level")
                .isFalse();
    }

    @Test
    void explicitDependenciesMarkedTopLevel() {
        ResolvedDependency directDep = resolvedDep("io.quarkus", "quarkus-rest", "3.0.0", List.of());
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        CoreSbomContributionConfig config = CoreSbomContributionConfig.builder()
                .setMainComponent(toDescriptor(mainArtifact),
                        List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.0.0")))
                .addComponent(toDescriptor(directDep), directDep)
                .build();

        ComponentDescriptor direct = findByName(config, "quarkus-rest");
        assertThat(direct.isTopLevel())
                .as("Component matching explicit dependency should be top-level")
                .isTrue();
    }

    private static ComponentDescriptor findByName(CoreSbomContributionConfig config, String name) {
        return config.getComponents().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " not found"));
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

    private static ComponentDescriptor toDescriptor(
            io.quarkus.maven.dependency.ResolvedDependency d) {
        return ComponentDescriptor.builder()
                .setPurl(Purl.maven(d.getGroupId(), d.getArtifactId(), d.getVersion()))
                .setScope(ComponentDescriptor.SCOPE_RUNTIME)
                .build();
    }
}
