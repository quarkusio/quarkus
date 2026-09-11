package io.quarkus.gradle.model.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.gradle.tooling.DefaultProjectDescriptor;

class CurrentProjectDescriptorBuilderTest {

    @TempDir
    Path projectDir;

    @Test
    void providerRefreshesCoordinatesAndCapturesCurrentProjectSourcesAfterEvaluation() {
        Project project = ProjectBuilder.builder()
                .withName("sample-app")
                .withProjectDir(projectDir.toFile())
                .build();
        Path effectiveProjectDir = project.getLayout().getProjectDirectory().getAsFile().toPath();
        project.getPlugins().apply(JavaPlugin.class);
        var descriptor = CurrentProjectDescriptorBuilder.buildForCurrentProject(project);

        project.setGroup("org.acme");
        project.setVersion("1.0");
        project.getTasks().named(JavaPlugin.JAR_TASK_NAME).get();
        ((ProjectInternal) project).evaluate();

        DefaultProjectDescriptor result = descriptor.get();

        assertThat(result.getWorkspaceModule().getId().toString()).isEqualTo("org.acme:sample-app:1.0");
        assertThat(result.getWorkspaceModule().getModuleDir()).isEqualTo(effectiveProjectDir.toFile());
        assertThat(result.getWorkspaceModule().getBuildDir())
                .isEqualTo(project.getLayout().getBuildDirectory().get().getAsFile());
        assertThat(result.getWorkspaceModule().hasMainSources()).isTrue();
        assertThat(result.getWorkspaceModule().getSourceClassifiers()).contains(ArtifactSources.MAIN);
        assertThat(result.getWorkspaceModule().getMainSources().getSourceDirs())
                .singleElement()
                .satisfies(sourceDir -> {
                    assertThat(sourceDir.getDir()).isEqualTo(effectiveProjectDir.resolve("src/main/java"));
                    assertThat(sourceDir.getOutputDir()).isEqualTo(effectiveProjectDir.resolve("build/classes/java/main"));
                    assertThat(sourceDir.getAptSourcesDir())
                            .isEqualTo(effectiveProjectDir.resolve("build/generated/sources/annotationProcessor/java/main"));
                });
        assertThat(result.getWorkspaceModule().getMainSources().getResourceDirs())
                .singleElement()
                .satisfies(resourceDir -> {
                    assertThat(resourceDir.getDir()).isEqualTo(effectiveProjectDir.resolve("src/main/resources"));
                    assertThat(resourceDir.getOutputDir()).isEqualTo(effectiveProjectDir.resolve("build/resources/main"));
                    assertThat(resourceDir.getAptSourcesDir()).isNull();
                });
    }

    @Test
    void providerCapturesCustomTestSuiteClassifierWithoutRequiringProjectGraphWalk() {
        Project project = ProjectBuilder.builder()
                .withName("sample-app")
                .withProjectDir(projectDir.toFile())
                .build();
        Path effectiveProjectDir = project.getLayout().getProjectDirectory().getAsFile().toPath();
        project.getPlugins().apply(JavaPlugin.class);
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        var integrationTest = sourceSets.create("integrationTest");
        project.getTasks().register("integrationTest", org.gradle.api.tasks.testing.Test.class, test -> {
            test.setTestClassesDirs(integrationTest.getOutput().getClassesDirs());
            test.setClasspath(integrationTest.getRuntimeClasspath());
        });

        var descriptor = CurrentProjectDescriptorBuilder.buildForCurrentProject(project);
        project.getTasks().named("integrationTest").get();
        ((ProjectInternal) project).evaluate();

        DefaultProjectDescriptor result = descriptor.get();

        assertThat(result.getWorkspaceModule().getSourceClassifiers()).contains("integration-test");
        assertThat(result.getWorkspaceModule().getSources("integration-test").getSourceDirs())
                .singleElement()
                .satisfies(sourceDir -> {
                    assertThat(sourceDir.getDir()).isEqualTo(effectiveProjectDir.resolve("src/integrationTest/java"));
                    assertThat(sourceDir.getOutputDir())
                            .isEqualTo(effectiveProjectDir.resolve("build/classes/java/integrationTest"));
                    assertThat(sourceDir.getAptSourcesDir())
                            .isEqualTo(effectiveProjectDir.resolve(
                                    "build/generated/sources/annotationProcessor/java/integrationTest"));
                });
    }
}
