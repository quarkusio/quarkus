package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(directDep)
                .addComponent(transitiveDep)
                .toSbomContribution();

        ComponentDescriptor direct = findByName(contribution, "quarkus-rest");
        assertThat(direct.isTopLevel())
                .as("Direct dependency should be top-level")
                .isTrue();

        ComponentDescriptor transitive = findByName(contribution, "quarkus-vertx");
        assertThat(transitive.isTopLevel())
                .as("Transitive dependency should not be top-level")
                .isFalse();
    }

    @Test
    void mainComponentIsNotMarkedTopLevel() {
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .toSbomContribution();

        ComponentDescriptor mainComp = contribution.components().stream()
                .filter(c -> c.getBomRef().equals(contribution.mainComponentBomRef()))
                .findFirst()
                .orElseThrow();
        assertThat(mainComp.isTopLevel())
                .as("Main component itself should not be top-level")
                .isFalse();
    }

    @Test
    void explicitDependenciesMarkedTopLevel() {
        ResolvedDependency directDep = resolvedDep("io.quarkus", "quarkus-rest", "3.0.0", List.of());
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .setMainDependencies(List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.0.0")))
                .addComponent(directDep)
                .toSbomContribution();

        ComponentDescriptor direct = findByName(contribution, "quarkus-rest");
        assertThat(direct.isTopLevel())
                .as("Component matching explicit dependency should be top-level")
                .isTrue();
    }

    @Test
    void shadedJarBundledComponentsDetected(@TempDir Path tempDir) throws IOException {
        Path shadedJar = createShadedJar(tempDir,
                "com.example", "shaded-lib", "1.0.0",
                "org.bundled", "bundled-dep", "2.0.0");

        ResolvedDependency dep = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("shaded-lib")
                .setVersion("1.0.0")
                .setResolvedPaths(PathList.of(shadedJar))
                .setDependencies(List.of())
                .setRuntimeCp()
                .build();
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(dep)
                .toSbomContribution();

        ComponentDescriptor parent = findByName(contribution, "shaded-lib");
        assertThat(parent.getComponents())
                .as("Shaded JAR should have nested bundled components")
                .hasSize(1);

        ComponentDescriptor nested = parent.getComponents().get(0);
        assertThat(nested.getName()).isEqualTo("bundled-dep");
        assertThat(nested.getNamespace()).isEqualTo("org.bundled");
        assertThat(nested.getVersion()).isEqualTo("2.0.0");
        assertThat(nested.getBomRef())
                .isEqualTo(Purl.maven("org.bundled", "bundled-dep", "2.0.0", "jar", null) + "#bundled");
    }

    @Test
    void shadedJarBomRefUniqueFromStandaloneDependency(@TempDir Path tempDir) throws IOException {
        Path shadedJar = createShadedJar(tempDir,
                "com.example", "shaded-lib", "1.0.0",
                "org.bundled", "bundled-dep", "2.0.0");

        ResolvedDependency shadedDep = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("shaded-lib")
                .setVersion("1.0.0")
                .setResolvedPaths(PathList.of(shadedJar))
                .setDependencies(List.of())
                .setRuntimeCp()
                .build();
        ResolvedDependency standaloneDep = resolvedDep("org.bundled", "bundled-dep", "2.0.0", List.of());
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(standaloneDep)
                .addComponent(shadedDep)
                .toSbomContribution();

        ComponentDescriptor standalone = findByName(contribution, "bundled-dep");
        ComponentDescriptor parent = findByName(contribution, "shaded-lib");
        ComponentDescriptor nested = parent.getComponents().get(0);

        assertThat(standalone.getBomRef())
                .as("Standalone gets the plain PURL bomRef")
                .doesNotContain("#");
        assertThat(nested.getBomRef())
                .as("Nested bomRef gets #bundled suffix")
                .endsWith("#bundled");
        assertThat(nested.getBomRef())
                .as("Nested and standalone bomRefs must differ")
                .isNotEqualTo(standalone.getBomRef());
    }

    @Test
    void nonShadedJarHasNoNestedComponents(@TempDir Path tempDir) throws IOException {
        Path normalJar = createShadedJar(tempDir,
                "com.example", "normal-lib", "1.0.0");

        ResolvedDependency dep = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("normal-lib")
                .setVersion("1.0.0")
                .setResolvedPaths(PathList.of(normalJar))
                .setDependencies(List.of())
                .setRuntimeCp()
                .build();
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(dep)
                .toSbomContribution();

        ComponentDescriptor parent = findByName(contribution, "normal-lib");
        assertThat(parent.getComponents()).isEmpty();
    }

    private static Path createShadedJar(Path dir, String ownerGroupId, String ownerArtifactId, String ownerVersion,
            String... shadedGavTriples) throws IOException {
        Path jar = dir.resolve(ownerArtifactId + "-" + ownerVersion + ".jar");
        try (OutputStream fos = Files.newOutputStream(jar);
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            writePomProperties(zos, ownerGroupId, ownerArtifactId, ownerVersion);
            for (int i = 0; i < shadedGavTriples.length; i += 3) {
                writePomProperties(zos, shadedGavTriples[i], shadedGavTriples[i + 1], shadedGavTriples[i + 2]);
            }
        }
        return jar;
    }

    private static void writePomProperties(ZipOutputStream zos, String groupId, String artifactId, String version)
            throws IOException {
        String path = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        zos.putNextEntry(new ZipEntry(path));
        Properties props = new Properties();
        props.setProperty("groupId", groupId);
        props.setProperty("artifactId", artifactId);
        props.setProperty("version", version);
        props.store(zos, null);
        zos.closeEntry();
    }

    private static ComponentDescriptor findByName(SbomContribution contribution, String name) {
        return contribution.components().stream()
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
}
