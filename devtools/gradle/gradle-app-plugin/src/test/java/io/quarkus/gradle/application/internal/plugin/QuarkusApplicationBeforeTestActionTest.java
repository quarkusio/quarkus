package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

class QuarkusApplicationBeforeTestActionTest {

    @TempDir
    Path projectDir;

    @org.junit.jupiter.api.Test
    void configuresTestSystemPropertiesAndMainClassMappingsAtExecutionTime() throws Exception {
        Project project = ProjectBuilder.builder()
                .withName("sample-app")
                .withProjectDir(projectDir.toFile())
                .build();
        Path modelPath = projectDir.resolve("build/quarkus/test-model.dat");
        ApplicationModelSerializer.serialize(new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("sample-app")
                        .setVersion("1.0")
                        .setType("jar")
                        .setResolvedPaths(PathList.empty()))
                .setPlatformImports(PlatformImports.fromMap(Map.of()))
                .build(), modelPath);
        Path generatedSources = Files.createDirectories(projectDir.resolve("build/generated/sources/quarkus"));
        Path mainClasses = Files.createDirectories(projectDir.resolve("build/classes/java/main"));
        Path testClasses = Files.createDirectories(projectDir.resolve("build/classes/java/test"));
        Path sources = Files.createDirectories(projectDir.resolve("src/main/java"));
        var quarkusBuildProperties = project.getObjects().mapProperty(String.class, String.class);
        quarkusBuildProperties.set(Map.of("quarkus.http.port", "0"));
        var gradleProperties = project.getObjects().mapProperty(String.class, String.class);
        gradleProperties.set(Map.of());
        var environmentVariables = project.getObjects().mapProperty(String.class, String.class);
        environmentVariables.set(Map.of());
        var systemProperties = project.getObjects().mapProperty(String.class, String.class);
        systemProperties.set(Map.of());
        Test test = project.getTasks().register("test", Test.class, task -> {
            task.setTestClassesDirs(project.files(testClasses));
            task.setClasspath(project.files(testClasses));
        }).get();

        new QuarkusApplicationBeforeTestAction(
                projectDir.toFile(),
                project.getLayout().file(project.provider(modelPath::toFile)),
                project.files(generatedSources),
                project.files(mainClasses),
                project.files(sources),
                quarkusBuildProperties,
                gradleProperties,
                environmentVariables,
                systemProperties)
                .execute(test);

        assertThat(test.getSystemProperties())
                .containsEntry(BootstrapConstants.SERIALIZED_TEST_APP_MODEL, modelPath.toString())
                .containsEntry(BootstrapConstants.OUTPUT_SOURCES_DIR, generatedSources.toAbsolutePath().toString())
                .containsEntry("quarkus.http.port", "0");
        assertThat(test.getEnvironment())
                .containsEntry(BootstrapConstants.TEST_TO_MAIN_MAPPINGS,
                        "%s:%s".formatted(projectDir.relativize(testClasses), projectDir.relativize(mainClasses)));
    }
}
