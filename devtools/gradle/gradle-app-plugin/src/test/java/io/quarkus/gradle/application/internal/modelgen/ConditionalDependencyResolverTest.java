package io.quarkus.gradle.application.internal.modelgen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

class ConditionalDependencyResolverTest {

    @TempDir
    Path testDirectory;

    @Test
    void resolvesOnlyConditionSatisfiedConditionalDependencies() throws IOException {
        Path parent = extensionJar("parent.jar",
                """
                        conditional-dependencies=org.acme\\:satisfied-extension\\:\\:jar\\:1.0 org.acme\\:missing-extension\\:\\:jar\\:1.0
                        deployment-artifact=org.acme\\:parent-deployment\\:1.0
                        """);
        Path condition = jar("condition.jar");
        Path satisfied = extensionJar("satisfied.jar", """
                dependency-condition=org.condition\\:present
                deployment-artifact=org.acme\\:satisfied-extension-deployment\\:1.0
                """);
        Path missing = extensionJar("missing.jar", """
                dependency-condition=org.condition\\:missing
                deployment-artifact=org.acme\\:missing-extension-deployment\\:1.0
                """);

        List<String> runtimeComponentKeys = List.of(
                artifactKey("org.acme", "parent"),
                artifactKey("org.condition", "present"));
        List<String> conditionalArtifacts = List.of(
                artifactRecord("org.acme:satisfied-extension::jar:1.0", satisfied),
                artifactRecord("org.acme:missing-extension::jar:1.0", missing));

        assertThat(ConditionalDependencyResolver.conditionalDependencyCoordinates(List.of(parent.toFile(), condition.toFile())))
                .containsExactly("org.acme:missing-extension:1.0", "org.acme:satisfied-extension:1.0");
        assertThat(ConditionalDependencyResolver.satisfiedConditionalDependencyCoordinates(runtimeComponentKeys,
                conditionalArtifacts))
                .containsExactly("org.acme:satisfied-extension:1.0");
    }

    @Test
    void resolvesConditionalsSatisfiedByEarlierConditionalDependencies() throws IOException {
        Path parent = extensionJar("parent.jar", """
                conditional-dependencies=org.acme\\:first-extension\\:\\:jar\\:1.0 org.acme\\:second-extension\\:\\:jar\\:1.0
                deployment-artifact=org.acme\\:parent-deployment\\:1.0
                """);
        Path first = extensionJar("first.jar", """
                deployment-artifact=org.acme\\:first-extension-deployment\\:1.0
                """);
        Path second = extensionJar("second.jar", """
                dependency-condition=org.acme\\:first-extension
                deployment-artifact=org.acme\\:second-extension-deployment\\:1.0
                """);

        List<String> runtimeComponentKeys = List.of(artifactKey("org.acme", "parent"));
        List<String> conditionalArtifacts = List.of(
                artifactRecord("org.acme:first-extension::jar:1.0", first),
                artifactRecord("org.acme:second-extension::jar:1.0", second));

        assertThat(ConditionalDependencyResolver.satisfiedConditionalDependencyCoordinates(runtimeComponentKeys,
                conditionalArtifacts))
                .containsExactly("org.acme:first-extension:1.0", "org.acme:second-extension:1.0");
    }

    @Test
    void resolvesConditionalDevDependenciesSeparately() throws IOException {
        Path parent = extensionJar("parent.jar",
                """
                        conditional-dependencies=org.acme\\:normal-extension\\:\\:jar\\:1.0
                        conditional-dev-dependencies=org.acme\\:dev-extension\\:\\:jar\\:1.0
                        deployment-artifact=org.acme\\:parent-deployment\\:1.0
                        """);

        assertThat(ConditionalDependencyResolver.conditionalDependencyCoordinates(List.of(parent.toFile())))
                .containsExactly("org.acme:normal-extension:1.0");
        assertThat(ConditionalDependencyResolver.conditionalDevDependencyCoordinates(List.of(parent.toFile())))
                .containsExactly("org.acme:dev-extension:1.0");
    }

    private Path extensionJar(String fileName, String descriptor) throws IOException {
        Path file = testDirectory.resolve(fileName);
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
            jar.putNextEntry(new JarEntry(BootstrapConstants.DESCRIPTOR_PATH));
            jar.write(descriptor.getBytes());
            jar.closeEntry();
        }
        return file;
    }

    private Path jar(String fileName) throws IOException {
        Path file = testDirectory.resolve(fileName);
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(file))) {
            return file;
        }
    }

    private static String artifactRecord(String coords, Path file) {
        return ConditionalDependencyResolver.artifactRecord(coords, file.toFile());
    }

    private static String artifactKey(String groupId, String artifactId) {
        return ConditionalDependencyResolver.serializeKey(
                ArtifactKey.of(groupId, artifactId, ArtifactCoords.DEFAULT_CLASSIFIER, ArtifactCoords.TYPE_JAR));
    }
}
