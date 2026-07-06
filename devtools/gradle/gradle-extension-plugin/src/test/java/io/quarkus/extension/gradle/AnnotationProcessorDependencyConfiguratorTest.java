package io.quarkus.extension.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipOutputStream;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.model.config.QuarkusExtensionAnnotationProcessorConfigurator;

class AnnotationProcessorDependencyConfiguratorTest {

    @Test
    void shouldUseVersionlessAnnotationProcessorWhenQuarkusPlatformIsAvailable() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(JavaPlugin.class);
        project.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
                project.getDependencies().enforcedPlatform("io.quarkus.platform:quarkus-bom:3.28.0"));
        AtomicBoolean fallbackUsed = new AtomicBoolean();

        new QuarkusExtensionAnnotationProcessorConfigurator(ignored -> {
            fallbackUsed.set(true);
            return "3.28.0";
        }).configure(project);

        DependencySet annotationProcessors = project.getConfigurations()
                .getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                .getAllDependencies();
        assertThat(annotationProcessors)
                .anySatisfy(dependency -> assertDependency(dependency, "io.quarkus.platform", "quarkus-bom"))
                .anySatisfy(dependency -> assertDependency(dependency, "io.quarkus", "quarkus-extension-processor", null));
        assertThat(fallbackUsed).isFalse();
    }

    @Test
    void shouldUseFallbackResolverWhenQuarkusPlatformIsNotAvailable() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(JavaPlugin.class);
        AtomicBoolean fallbackUsed = new AtomicBoolean();

        new QuarkusExtensionAnnotationProcessorConfigurator(ignored -> {
            fallbackUsed.set(true);
            return "3.28.0";
        }).configure(project);

        assertThat(project.getConfigurations()
                .getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                .getAllDependencies())
                .singleElement()
                .satisfies(dependency -> assertDependency(dependency, "io.quarkus", "quarkus-extension-processor", "3.28.0"));
        assertThat(fallbackUsed).isTrue();
    }

    @Test
    void shouldResolveQuarkusCoreVersionFromCompileClasspathWhenQuarkusPlatformIsNotAvailable(@TempDir Path mavenRepository)
            throws IOException {
        writeMavenArtifact(mavenRepository, "io.quarkus", "quarkus-core", "3.28.0");
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(JavaPlugin.class);
        project.getRepositories().maven(repository -> repository.setUrl(mavenRepository.toUri()));
        project.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "io.quarkus:quarkus-core:3.28.0");

        new QuarkusExtensionAnnotationProcessorConfigurator().configure(project);

        assertThat(project.getConfigurations()
                .getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                .getAllDependencies())
                .singleElement()
                .satisfies(dependency -> assertDependency(dependency, "io.quarkus", "quarkus-extension-processor", "3.28.0"));
    }

    private static void writeMavenArtifact(Path repository, String group, String name, String version) throws IOException {
        Path artifactDirectory = repository.resolve(group.replace('.', '/')).resolve(name).resolve(version);
        Files.createDirectories(artifactDirectory);
        String fileBaseName = name + '-' + version;
        Files.writeString(artifactDirectory.resolve(fileBaseName + ".pom"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </project>
                """.formatted(group, name, version));
        try (ZipOutputStream ignored = new ZipOutputStream(
                Files.newOutputStream(artifactDirectory.resolve(fileBaseName + ".jar")))) {
        }
    }

    private static void assertDependency(Dependency dependency, String group, String name, String version) {
        assertDependency(dependency, group, name);
        assertThat(dependency.getVersion()).isEqualTo(version);
    }

    private static void assertDependency(Dependency dependency, String group, String name) {
        assertThat(dependency.getGroup()).isEqualTo(group);
        assertThat(dependency.getName()).isEqualTo(name);
    }
}
