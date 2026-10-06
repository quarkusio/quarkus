package io.quarkus.extension.deployment.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.testing.BaseGradleTest;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

public class QuarkusExtensionDeploymentPluginTest extends BaseGradleTest {
    @Test
    public void pluginCanBeResolvedFromPluginClasspath() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'test'\n");
        writeFile("build.gradle",
                "plugins {\n" +
                        "    id '" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "'\n" +
                        "}\n");

        var result = buildResult("help");

        assertTaskOutcomes(result, SUCCESS, ":help");
    }

    @Test
    public void pluginAddsAnnotationProcessor() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'test'\n");
        writeFile("build.gradle",
                "plugins {\n" +
                        "    id '" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "'\n" +
                        "}\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "    mavenLocal()\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "    implementation enforcedPlatform('io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "')\n" +
                        "    implementation 'io.quarkus:quarkus-arc-deployment'\n" +
                        "}\n");

        var result = buildResult("dependencies", "--configuration", "annotationProcessor");

        assertTaskOutcomes(result, SUCCESS, ":dependencies");
        assertThat(result.getOutput()).contains("io.quarkus:quarkus-extension-processor");
    }

    @Test
    public void deploymentTestsUseGeneratedApplicationModel() throws IOException {
        writeFile("settings.gradle",
                """
                        rootProject.name = 'test'
                        include 'runtime', 'deployment'
                        """);
        var runtimeProjectDir = testProjectDir.resolve("runtime");
        var deploymentProjectDir = testProjectDir.resolve("deployment");
        writeFile(runtimeProjectDir.resolve("build.gradle"),
                "plugins {\n" +
                        "    id 'java'\n" +
                        "}\n" +
                        "group = 'org.acme'\n" +
                        "version = '1.0.0'\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "    mavenLocal()\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "    implementation enforcedPlatform('io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "')\n" +
                        "    implementation 'io.quarkus:quarkus-arc'\n" +
                        "}\n");
        var runtimeTestFile = runtimeProjectDir.resolve("src/main/java/runtime/Test.java");
        writeFile(runtimeTestFile, "package runtime; public class Test {}\n");

        writeFile(deploymentProjectDir.resolve("build.gradle"),
                "plugins {\n" +
                        "    id '" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "'\n" +
                        "}\n" +
                        "group = 'org.acme'\n" +
                        "version = '1.0.0'\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "    mavenLocal()\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "    implementation platform('io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "')\n" +
                        "    implementation 'io.quarkus:quarkus-arc-deployment'\n" +
                        "    implementation project(':runtime')\n" +
                        "    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.3'\n" +
                        "    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.3'\n" +
                        "}\n");
        var deploymentTestFile = deploymentProjectDir.resolve("src/test/java/deployment/ModelTest.java");
        writeFile(deploymentTestFile,
                "package deployment;\n" +
                        "import static org.junit.jupiter.api.Assertions.assertTrue;\n" +
                        "import java.nio.file.Files;\n" +
                        "import java.nio.file.Path;\n" +
                        "import org.junit.jupiter.api.Test;\n" +
                        "class ModelTest {\n" +
                        "    @Test\n" +
                        "    void serializedApplicationModelIsConfigured() throws Exception {\n" +
                        "        assertTrue(Files.exists(Path.of(System.getProperty(\""
                        + BootstrapConstants.SERIALIZED_TEST_APP_MODEL + "\"))));\n" +
                        "    }\n" +
                        "}\n");

        var firstResult = buildResult(":deployment:test", CONFIGURATION_CACHE, ISOLATED_PROJECTS);
        var secondResult = buildResult(":deployment:test", CONFIGURATION_CACHE, ISOLATED_PROJECTS);

        assertTaskOutcomes(firstResult, SUCCESS,
                ":deployment:quarkusGenerateTestAppModel",
                ":deployment:test");
        assertThat(firstResult.getOutput()).contains("Configuration cache entry stored");
        assertThat(secondResult.task(":deployment:quarkusGenerateTestAppModel").getOutcome())
                .isIn(UP_TO_DATE, FROM_CACHE);
        assertThat(secondResult.task(":deployment:test").getOutcome()).isIn(UP_TO_DATE, FROM_CACHE);
        assertThat(secondResult.getOutput()).contains("Reusing configuration cache.");

        Path modelPath = deploymentProjectDir.resolve("build/quarkus/application-model/quarkus-app-test-model.dat");
        assertThat(modelPath).exists();
        ApplicationModel model = ApplicationModelSerializer.deserialize(modelPath);
        assertThat(model.getPlatforms().getImportedPlatformBoms())
                .contains(ArtifactCoords.pom("io.quarkus", "quarkus-bom", getCurrentQuarkusVersion()));
        assertThat(model.getPlatforms().isAligned()).isTrue();
        assertThat(model.getPlatformProperties()).containsKey("platform.quarkus.native.builder-image");
    }

    @Test
    public void generatedApplicationModelUsesLocalOutputsWithIsolatedProjects() throws Exception {
        writeFile("settings.gradle",
                """
                        rootProject.name = 'test'
                        include 'runtime', 'deployment'
                        """);
        var runtimeProjectDir = testProjectDir.resolve("runtime");
        var deploymentProjectDir = testProjectDir.resolve("deployment");
        writeFile(runtimeProjectDir.resolve("build.gradle"),
                "plugins {\n" +
                        "    id 'java-library'\n" +
                        "    id 'java-test-fixtures'\n" +
                        "}\n" +
                        "group = 'org.acme'\n" +
                        "version = '1.0.0'\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "    mavenLocal()\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "    implementation enforcedPlatform('io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "')\n" +
                        "    implementation 'io.quarkus:quarkus-arc'\n" +
                        "}\n");
        writeFile(runtimeProjectDir.resolve("src/main/java/runtime/MainRuntime.java"),
                "package runtime; public class MainRuntime {}\n");
        writeFile(runtimeProjectDir.resolve("src/testFixtures/java/runtime/FixtureRuntime.java"),
                "package runtime; public class FixtureRuntime {}\n");

        writeFile(deploymentProjectDir.resolve("build.gradle"),
                "plugins {\n" +
                        "    id '" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "'\n" +
                        "}\n" +
                        "group = 'org.acme'\n" +
                        "version = '1.0.0'\n" +
                        "repositories {\n" +
                        "    mavenCentral()\n" +
                        "    mavenLocal()\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "    implementation enforcedPlatform('io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "')\n" +
                        "    implementation 'io.quarkus:quarkus-arc-deployment'\n" +
                        "    implementation project(':runtime')\n" +
                        "    testImplementation testFixtures(project(':runtime'))\n" +
                        "}\n");

        var result = buildResult(":deployment:quarkusGenerateTestAppModel",
                "-Dorg.gradle.unsafe.isolated-projects=true");

        assertTaskOutcomes(result, SUCCESS, ":deployment:quarkusGenerateTestAppModel");
        Path modelPath = deploymentProjectDir.resolve("build/quarkus/application-model/quarkus-app-test-model.dat");
        assertThat(modelPath).exists();
        ApplicationModel model = ApplicationModelSerializer.deserialize(modelPath);
        Map<ArtifactKey, ResolvedDependency> orgAcmeDependencies = new HashMap<>();
        for (ResolvedDependency dependency : model.getDependencies()) {
            if (dependency.getGroupId().equals("org.acme")) {
                orgAcmeDependencies.put(dependency.getKey(), dependency);
            }
        }

        ResolvedDependency runtime = orgAcmeDependencies.get(ArtifactKey.fromString("org.acme:runtime::jar"));
        ResolvedDependency testFixtures = orgAcmeDependencies.get(ArtifactKey.fromString("org.acme:runtime:test-fixtures:jar"));
        assertWorkspaceReloadable(runtime);
        assertWorkspaceReloadable(testFixtures);
        assertResolvedPathsContain(runtime, "runtime/build/classes/java/main");
        assertResolvedPathsContain(testFixtures, "runtime/build/classes/java/testFixtures");
        assertResolvedPathsDoNotContain(testFixtures, "runtime/build/classes/java/main");

        var secondResult = buildResult(":deployment:quarkusGenerateTestAppModel",
                "-Dorg.gradle.unsafe.isolated-projects=true");
        assertThat(secondResult.task(":deployment:quarkusGenerateTestAppModel").getOutcome())
                .isIn(UP_TO_DATE, FROM_CACHE);
        assertThat(secondResult.getOutput()).contains("Reusing configuration cache.");
    }

    @Test
    public void pluginPublishesSelectableDeploymentMarkerVariant() throws IOException {
        writeFile("settings.gradle",
                """
                        rootProject.name = 'test'
                        include 'runtime', 'deployment'
                        """);
        var runtimeProjectDir = testProjectDir.resolve("runtime");
        var deploymentProjectDir = testProjectDir.resolve("deployment");
        writeFile(deploymentProjectDir.resolve("build.gradle"),
                "plugins {\n" +
                        "    id 'java'\n" +
                        "    id '" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "'\n" +
                        "}\n");
        writeFile(runtimeProjectDir.resolve("build.gradle"),
                "def deploymentAttr = Attribute.of('" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "', Boolean)\n" +
                        "def markerCategory = objects.named(org.gradle.api.attributes.Category, '" +
                        QuarkusExtensionDeploymentPlugin.MARKER_CATEGORY + "')\n" +
                        "configurations {\n" +
                        "    deploymentMarker {\n" +
                        "        canBeConsumed = false\n" +
                        "        canBeResolved = true\n" +
                        "        attributes {\n" +
                        "            attribute(org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE, markerCategory)\n" +
                        "            attribute(deploymentAttr, true)\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "    deploymentMarker project(':deployment')\n" +
                        "}\n" +
                        "def deploymentMarkerFiles = configurations.deploymentMarker\n" +
                        "tasks.register('resolveDeploymentMarker') {\n" +
                        "    inputs.files(deploymentMarkerFiles)\n" +
                        "    doLast {\n" +
                        "        def files = deploymentMarkerFiles.files\n" +
                        "        assert files.size() == 1\n" +
                        "        println 'deploymentMarker=' + files.first().text.trim()\n" +
                        "    }\n" +
                        "}\n");

        var result = buildResult(":runtime:resolveDeploymentMarker");

        assertThat(result.task(":deployment:" + QuarkusExtensionDeploymentPlugin.MARKER_TASK_NAME).getOutcome())
                .isIn(SUCCESS, FROM_CACHE);
        assertTaskOutcomes(result, SUCCESS, ":runtime:resolveDeploymentMarker");
        assertThat(result.getOutput()).contains("deploymentMarker=" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID);
    }

    @Test
    public void markerTaskRestoresOutputFromBuildCache() throws IOException {
        writeFile("settings.gradle",
                """
                        rootProject.name = 'test'
                        buildCache { local { directory = file('local-build-cache') } }
                        """);
        writeFile("build.gradle",
                "plugins {\n" +
                        "    id '" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "'\n" +
                        "}\n");

        String markerTask = ":" + QuarkusExtensionDeploymentPlugin.MARKER_TASK_NAME;
        var firstRun = buildResult("clean", markerTask);

        assertTaskOutcomes(firstRun, SUCCESS, markerTask);

        var secondRun = buildResult("clean", markerTask);

        assertTaskOutcomes(secondRun, FROM_CACHE, markerTask);
        assertThat(testProjectDir
                .resolve("build/quarkus/extension-deployment-marker/" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID))
                .content().isEqualTo(QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "\n");
    }

    @Test
    public void markerResolutionFailsWhenDeploymentPluginIsMissing() throws IOException {
        writeFile("settings.gradle",
                """
                        rootProject.name = 'test'
                        include 'runtime', 'deployment'
                        """);
        var runtimeProjectDir = testProjectDir.resolve("runtime");
        var deploymentProjectDir = testProjectDir.resolve("deployment");
        writeFile(deploymentProjectDir.resolve("build.gradle"),
                """
                        plugins {
                            id 'java'
                        }
                        """);
        writeFile(runtimeProjectDir.resolve("build.gradle"),
                "def deploymentAttr = Attribute.of('" + QuarkusExtensionDeploymentPlugin.PLUGIN_ID + "', Boolean)\n" +
                        "def markerCategory = objects.named(org.gradle.api.attributes.Category, '" +
                        QuarkusExtensionDeploymentPlugin.MARKER_CATEGORY + "')\n" +
                        "configurations {\n" +
                        "    deploymentMarker {\n" +
                        "        canBeConsumed = false\n" +
                        "        canBeResolved = true\n" +
                        "        attributes {\n" +
                        "            attribute(org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE, markerCategory)\n" +
                        "            attribute(deploymentAttr, true)\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "    deploymentMarker project(':deployment')\n" +
                        "}\n" +
                        "def deploymentMarkerFiles = configurations.deploymentMarker\n" +
                        "tasks.register('resolveDeploymentMarker') {\n" +
                        "    inputs.files(deploymentMarkerFiles)\n" +
                        "    doLast {\n" +
                        "        deploymentMarkerFiles.files\n" +
                        "    }\n" +
                        "}\n");

        var result = buildAndFailResult(Map.of(), ":runtime:resolveDeploymentMarker");

        assertThat(result.getOutput()).contains("No matching variant", QuarkusExtensionDeploymentPlugin.PLUGIN_ID);
    }

    private static String getCurrentQuarkusVersion() throws IOException {
        var gradlePropsFile = Paths.get("").toAbsolutePath().normalize().getParent().resolve("gradle.properties");
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(gradlePropsFile)) {
            props.load(is);
        }
        return props.getProperty("version");
    }

    private static void assertWorkspaceReloadable(ResolvedDependency dependency) {
        assertThat(dependency).isNotNull();
        assertThat(dependency.getFlags() & DependencyFlags.WORKSPACE_MODULE).isNotZero();
        assertThat(dependency.getFlags() & DependencyFlags.RELOADABLE).isNotZero();
    }

    private static void assertResolvedPathsContain(ResolvedDependency dependency, String expectedPath) {
        assertThat(dependency.getResolvedPaths().stream()
                .map(QuarkusExtensionDeploymentPluginTest::normalizedPath)
                .toList()).anyMatch(path -> path.endsWith(expectedPath));
    }

    private static void assertResolvedPathsDoNotContain(ResolvedDependency dependency, String unexpectedPath) {
        assertThat(dependency.getResolvedPaths().stream()
                .map(QuarkusExtensionDeploymentPluginTest::normalizedPath)
                .toList()).noneMatch(path -> path.endsWith(unexpectedPath));
    }

    private static String normalizedPath(Path path) {
        return path.toString().replace('\\', '/');
    }
}
