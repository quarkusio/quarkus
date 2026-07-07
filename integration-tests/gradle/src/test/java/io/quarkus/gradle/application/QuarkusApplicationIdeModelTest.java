package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.gradle.wrapper.GradleUserHomeLookup;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;

class QuarkusApplicationIdeModelTest extends QuarkusApplicationGradleTestBase {

    private static final String GENERATED_MAIN = "build/generated/sources/quarkus-application/main";
    private static final String GENERATED_TEST = "build/generated/sources/quarkus-application/test";

    @Test
    void standardIdeModelsExposeGeneratedMainAndTestRoots() throws Exception {
        File projectDirectory = getProjectDir("application-plugin/ide-codegen");
        Path generatedMain = projectDirectory.toPath().resolve(GENERATED_MAIN).toAbsolutePath().normalize();
        Path generatedTest = projectDirectory.toPath().resolve(GENERATED_TEST).toAbsolutePath().normalize();

        try (ProjectConnection connection = connection(projectDirectory)) {
            IdeaProject ideaProject = connection.model(IdeaProject.class)
                    .withArguments(modelArguments())
                    .get();
            assertIdeaModel(ideaProject, generatedMain, generatedTest);

            EclipseProject eclipseProject = connection.model(EclipseProject.class)
                    .withArguments(modelArguments())
                    .get();
            assertThat(eclipseProject.getSourceDirectories())
                    .extracting(source -> source.getDirectory().toPath().toAbsolutePath().normalize())
                    .contains(generatedMain, generatedTest);
        }

        BuildResult compileResult = runApplicationGradleWrapper(projectDirectory, "compileJava", "compileTestJava");
        assertThat(compileResult.unsuccessfulTasks()).isEmpty();
        assertThat(compileResult.getTasks()).containsKeys(
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateTestCode",
                ":compileJava",
                ":compileTestJava");
    }

    private static void assertIdeaModel(IdeaProject ideaProject, Path generatedMain, Path generatedTest) {
        assertThat(ideaProject.getModules()).hasSize(1);
        IdeaModule module = ideaProject.getModules().iterator().next();
        assertThat(module.getContentRoots()).hasSize(1);
        IdeaContentRoot contentRoot = module.getContentRoots().iterator().next();
        assertGeneratedDirectory(contentRoot.getSourceDirectories(), generatedMain);
        assertGeneratedDirectory(contentRoot.getTestDirectories(), generatedTest);
    }

    private static void assertGeneratedDirectory(Iterable<? extends IdeaSourceDirectory> directories, Path expected) {
        assertThat(directories)
                .filteredOn(directory -> directory.getDirectory().toPath().toAbsolutePath().normalize().equals(expected))
                .singleElement()
                .extracting(IdeaSourceDirectory::isGenerated)
                .isEqualTo(true);
    }

    private static ProjectConnection connection(File projectDirectory) {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(projectDirectory)
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome());
        String requestedVersion = System.getProperty("quarkus-test-gradle-wrapper-version");
        if (requestedVersion != null) {
            connector.useGradleVersion(requestedVersion);
        }
        return connector.connect();
    }

    private static String[] modelArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add("--configuration-cache");
        arguments.add("-Dorg.gradle.unsafe.isolated-projects=true");
        arguments.add("--stacktrace");
        arguments.add("-Dquarkus.analytics.disabled=true");
        String localRepository = System.getProperty("maven.repo.local");
        if (localRepository != null) {
            arguments.add("-Dmaven.repo.local=" + localRepository);
        }
        return arguments.toArray(String[]::new);
    }
}
