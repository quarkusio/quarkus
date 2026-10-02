package io.quarkus.gradle.application;

import static io.quarkus.gradle.testing.BaseGradleTest.canonicalPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.events.task.TaskStartEvent;
import org.gradle.wrapper.GradleUserHomeLookup;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.gradle.testing.BaseGradleTest;
import io.quarkus.maven.dependency.ResolvedDependency;

class QuarkusApplicationToolingApiTest extends BaseGradleTest {

    private static final String MAIN_LIBRARY = "main-library";
    private static final String TEST_LIBRARY = "test-library";

    @Test
    void unparameterizedRequestBuildsDevelopmentModelWithoutTasks() throws Exception {
        writeFixture();

        requestUnparameterized();
        ModelRequest<ApplicationModel> result = requestUnparameterized();

        assertThat(result.model().getReloadableWorkspaceDependencies()).extracting(key -> key.getArtifactId())
                .doesNotContain(MAIN_LIBRARY);
        assertThat(result.model().getDependencies()).extracting(dependency -> dependency.getArtifactId())
                .contains(MAIN_LIBRARY)
                .doesNotContain(TEST_LIBRARY);
        assertWorkspaceDependency(result.model(), MAIN_LIBRARY, "main-library");
        assertNoTasks(result);
        assertConfigurationCacheReused(result.output());
    }

    @Test
    void parameterizedRequestsBuildNormalAndTestModelsWithoutTasks() throws Exception {
        writeFixture();

        ModelRequest<ApplicationModel> normal = request(new ToolingApplicationModelAction("NORMAL"));
        ModelRequest<ApplicationModel> test = request(new ToolingApplicationModelAction("TEST"));

        assertThat(normal.model().getDependencies()).extracting(dependency -> dependency.getArtifactId())
                .contains(MAIN_LIBRARY)
                .doesNotContain(TEST_LIBRARY);
        assertThat(normal.model().getReloadableWorkspaceDependencies()).extracting(key -> key.getArtifactId())
                .doesNotContain(MAIN_LIBRARY);
        assertThat(test.model().getDependencies()).extracting(dependency -> dependency.getArtifactId())
                .contains(MAIN_LIBRARY, TEST_LIBRARY);
        assertThat(test.model().getReloadableWorkspaceDependencies()).extracting(key -> key.getArtifactId())
                .doesNotContain(MAIN_LIBRARY, TEST_LIBRARY);
        assertWorkspaceDependency(test.model(), MAIN_LIBRARY, "main-library");
        assertWorkspaceDependency(test.model(), TEST_LIBRARY, "test-library");
        assertNoTasks(normal);
        assertNoTasks(test);
    }

    @Test
    void pairedRequestReturnsCorrelatedApplicationModelAndSidecarWithoutTasks() throws Exception {
        writeFixture();

        ModelRequest<ToolingPairedModels> result = request(new ToolingPairedModelsAction("DEVELOPMENT"));
        ApplicationModel applicationModel = result.model().applicationModel();
        GradleApplicationModelSidecar sidecar = result.model().sidecar();

        assertThat(sidecar.getCorrelation().getSchemaVersion())
                .isEqualTo(GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION);
        assertThat(sidecar.getCorrelation().getMode()).isEqualTo(GradleApplicationModelSidecar.Mode.DEVELOPMENT);
        assertThat(sidecar.getCorrelation().getTargetBuildTreePath()).isEqualTo(":");
        assertThat(sidecar.getCorrelation().getCanonicalGraphFacts()).isNotEmpty();
        assertThat(applicationModel.getDependencies()).extracting(dependency -> dependency.getArtifactId())
                .contains(MAIN_LIBRARY);
        assertThat(sidecar.getProjectComponents())
                .extracting(component -> component.getProjectIdentity().getBuildTreePath())
                .contains(":", ":main-library");
        assertNoTasks(result);
    }

    @Test
    void parameterizedRequestCanRunExplicitClassesPrerequisites() throws Exception {
        writeFixture(false);

        request(new ToolingApplicationModelAction("DEVELOPMENT"), "classes");
        ModelRequest<ApplicationModel> result = request(new ToolingApplicationModelAction("DEVELOPMENT"), "classes");

        assertThat(result.taskPaths()).contains(
                ":main-library:classes",
                ":classes");
        assertThat(testProjectDir.resolve("main-library/build/classes/java/main/org/acme/MainlibraryValue.class"))
                .isRegularFile();
        assertConfigurationCacheReused(result.output());
    }

    @Test
    void repeatedParameterizedRequestReusesConfigurationCacheWithoutTasks() throws Exception {
        writeFixture();

        ModelRequest<ApplicationModel> first = request(new ToolingApplicationModelAction("DEVELOPMENT"));
        ModelRequest<ApplicationModel> second = request(new ToolingApplicationModelAction("DEVELOPMENT"));

        assertThat(first.output()).contains("Configuration cache entry stored");
        assertThat(second.output()).containsAnyOf(
                "Reusing configuration cache.",
                "Configuration cache entry reused.");
        assertNoTasks(first);
        assertNoTasks(second);
    }

    private void writeFixture() throws Exception {
        writeFixture(true);
    }

    private void writeFixture(boolean failIfClassesRun) throws Exception {
        writeFile("settings.gradle", """
                rootProject.name = 'standalone-tooling-model'
                include 'main-library', 'test-library'
                """);
        writeFile("build.gradle", """
                buildscript {
                    dependencies {
                        classpath files(%s)
                    }
                }

                apply plugin: 'io.quarkus.application'

                group = 'org.acme'
                version = '1.0'

                dependencies {
                    implementation project(':main-library')
                    testImplementation project(':test-library')
                }

                %s
                """.formatted(pluginClasspathFiles(), failIfClassesRun ? classesGuard(":") : ""));
        writeLibrary("main-library", failIfClassesRun);
        writeLibrary("test-library", failIfClassesRun);
    }

    private void writeLibrary(String name, boolean failIfClassesRun) throws Exception {
        writeFile(name + "/build.gradle", """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'

                %s
                """.formatted(failIfClassesRun ? classesGuard(":" + name) : ""));
        writeFile(name + "/src/main/java/org/acme/" + className(name) + ".java", """
                package org.acme;

                public final class %s {
                }
                """.formatted(className(name)));
    }

    private ModelRequest<ApplicationModel> requestUnparameterized() {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        List<String> taskPaths = new CopyOnWriteArrayList<>();
        try (ProjectConnection connection = connection()) {
            ModelBuilder<ApplicationModel> request = connection.model(ApplicationModel.class)
                    .withArguments(arguments())
                    .setStandardOutput(standardOutput)
                    .setStandardError(errorOutput);
            observeTasks(request, taskPaths);
            return result(request.get(), standardOutput, errorOutput, taskPaths);
        }
    }

    private <T> ModelRequest<T> request(BuildAction<T> action, String... tasks) {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        List<String> taskPaths = new CopyOnWriteArrayList<>();
        try (ProjectConnection connection = connection()) {
            BuildActionExecuter<T> request = connection.action(action)
                    .withArguments(arguments())
                    .setStandardOutput(standardOutput)
                    .setStandardError(errorOutput);
            observeTasks(request, taskPaths);
            if (tasks.length > 0) {
                request.forTasks(tasks);
            }
            return result(request.run(), standardOutput, errorOutput, taskPaths);
        }
    }

    private ProjectConnection connection() {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(testProjectDir.toFile())
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome());
        String requestedVersion = System.getProperty("quarkus-test-gradle-wrapper-version");
        if (requestedVersion != null) {
            connector.useGradleVersion(requestedVersion);
        }
        return connector.connect();
    }

    private static void observeTasks(ModelBuilder<?> request, List<String> taskPaths) {
        request.addProgressListener(event -> {
            if (event instanceof TaskStartEvent taskStart) {
                taskPaths.add(taskStart.getDescriptor().getTaskPath());
            }
        }, OperationType.TASK);
    }

    private static void observeTasks(BuildActionExecuter<?> request, List<String> taskPaths) {
        request.addProgressListener(event -> {
            if (event instanceof TaskStartEvent taskStart) {
                taskPaths.add(taskStart.getDescriptor().getTaskPath());
            }
        }, OperationType.TASK);
    }

    private static <T> ModelRequest<T> result(T model, ByteArrayOutputStream standardOutput,
            ByteArrayOutputStream errorOutput, List<String> taskPaths) {
        String output = standardOutput.toString(UTF_8) + System.lineSeparator() + errorOutput.toString(UTF_8);
        return new ModelRequest<>(model, output, List.copyOf(taskPaths));
    }

    private static void assertNoTasks(ModelRequest<?> result) {
        assertThat(result.taskPaths()).as("executed Gradle task paths").isEmpty();
    }

    private void assertWorkspaceDependency(ApplicationModel model, String artifactId, String projectName) {
        ResolvedDependency dependency = model.getDependencies().stream()
                .filter(candidate -> candidate.getGroupId().equals("org.acme"))
                .filter(candidate -> candidate.getArtifactId().equals(artifactId))
                .findFirst()
                .orElseThrow();
        assertThat(dependency.getWorkspaceModule()).isNotNull();
        assertThat(dependency.getResolvedPaths().contains(
                canonicalPath(testProjectDir.resolve(projectName + "/build/classes/java/main")))).isTrue();
        assertThat(dependency.getResolvedPaths().contains(
                canonicalPath(testProjectDir.resolve(projectName + "/build/resources/main")))).isTrue();
    }

    private static String[] arguments() {
        return new String[] {
                CONFIGURATION_CACHE,
                ISOLATED_PROJECTS,
                STACKTRACE,
                "--info",
                "-Dorg.gradle.console=plain",
                "-Dquarkus.analytics.disabled=true"
        };
    }

    private static String pluginClasspathFiles() {
        return TestKitPluginClasspath.implementationClasspath().stream()
                .map(File::getAbsolutePath)
                .map(QuarkusApplicationToolingApiTest::singleQuotedGroovyString)
                .collect(Collectors.joining(", "));
    }

    private static String singleQuotedGroovyString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String classesGuard(String taskPath) {
        return """
                tasks.named('classes') {
                    doLast {
                        throw new GradleException('Tooling-model request unexpectedly executed %s:classes')
                    }
                }
                """.formatted(taskPath);
    }

    private static String className(String projectName) {
        return Character.toUpperCase(projectName.charAt(0))
                + projectName.substring(1).replace("-", "")
                + "Value";
    }

    private record ModelRequest<T>(T model, String output, List<String> taskPaths) {
    }

}
