package io.quarkus.extension.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.fs.util.ZipUtils;

public class QuarkusExtensionPluginTest {

    @TempDir
    File testProjectDir;
    private File buildFile;

    @BeforeEach
    public void setupProject() throws IOException {
        buildFile = new File(testProjectDir, "build.gradle");

        File settingFile = new File(testProjectDir, "settings.gradle");
        String settingsContent = "rootProject.name = 'test'";
        TestUtils.writeFile(settingFile, settingsContent);
    }

    @Test
    public void jarShouldContainsExtensionPropertiesFile() throws IOException {
        TestUtils.writeFile(buildFile, TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(), ""));

        BuildResult jarResult = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments("jar", "-S")
                .build();
        assertThat(jarResult.task(":jar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(jarResult.task(":" + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME).getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);

        File jarFile = new File(testProjectDir, "build/libs/test-1.0.0.jar");
        assertThat(jarFile).exists();
        assertThat(jarFile).satisfies(f -> {
            try (FileSystem jarFs = ZipUtils.newFileSystem(f.toPath())) {
                Path descriptorPath = jarFs.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                assertThat(descriptorPath).exists();

                Properties extensionProperty = TestUtils.readPropertyFile(descriptorPath);
                assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");

            } catch (IOException e) {
                Assertions.fail("Unable to read jar file");
            }
        });
    }

    @Test
    public void pluginShouldAddAnnotationProcessor() throws IOException {
        TestUtils.createExtensionProject(testProjectDir, false, Collections.emptyList(), Collections.emptyList());
        BuildResult dependencies = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments("build", ":runtime:dependencies", "--configuration", "annotationProcessor")
                .build();

        assertThat(dependencies.getOutput()).contains(QuarkusExtensionPlugin.QUARKUS_ANNOTATION_PROCESSOR);
    }

    @Test
    public void pluginShouldAddAnnotationProcessorToDeploymentModule() throws IOException {
        TestUtils.createExtensionProject(testProjectDir, false, Collections.emptyList(), Collections.emptyList());
        BuildResult dependencies = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments("build", ":deployment:dependencies", "--configuration", "annotationProcessor")
                .build();
        assertThat(dependencies.getOutput()).contains(QuarkusExtensionPlugin.QUARKUS_ANNOTATION_PROCESSOR);
    }

    @Test
    public void deploymentTestShouldGenerateApplicationModelWithComponentVariants() throws IOException {
        createExtensionProjectWithDeploymentTest();

        BuildResult test = runGradle(":deployment:test");

        assertThat(test.task(":deployment:test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(test.getOutput()).doesNotContain("cannot choose between the following variants");
        assertDeploymentTestApplicationModelMarker();
    }

    @Test
    public void deploymentTestShouldGenerateApplicationModelWithoutComponentVariants() throws IOException {
        createExtensionProjectWithDeploymentTest();

        BuildResult test = runGradle(":deployment:test", "-PdisableQuarkusComponentVariants=true");

        assertThat(test.task(":deployment:test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertDeploymentTestApplicationModelMarker();
    }

    @Test
    public void noArgApplicationModelBuilderShouldResolveDeploymentProjectWithComponentVariants() throws IOException {
        createExtensionProjectWithDeploymentTest();

        BuildResult model = runGradle(":runtime:resolveDeploymentTestApplicationModel");

        assertThat(model.task(":runtime:resolveDeploymentTestApplicationModel").getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
        assertThat(model.getOutput()).contains("resolved deployment test application model");
    }

    private BuildResult runGradle(String... arguments) {
        List<String> gradleArguments = new ArrayList<>(List.of(arguments));
        gradleArguments.add("-S");
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments(gradleArguments)
                .build();
    }

    private void createExtensionProjectWithDeploymentTest() throws IOException {
        File runtimeModule = new File(testProjectDir, "runtime");
        runtimeModule.mkdir();
        TestUtils.writeFile(new File(runtimeModule, "build.gradle"), runtimeBuildFile());
        File runtimeClass = new File(runtimeModule, "src/main/java/runtime/Test.java");
        runtimeClass.getParentFile().mkdirs();
        TestUtils.writeFile(runtimeClass, "package runtime; public class Test {}\n");

        File deploymentModule = new File(testProjectDir, "deployment");
        deploymentModule.mkdir();
        TestUtils.writeFile(new File(deploymentModule, "build.gradle"), deploymentBuildFile());
        File deploymentClass = new File(deploymentModule, "src/main/java/deployment/Test.java");
        deploymentClass.getParentFile().mkdirs();
        TestUtils.writeFile(deploymentClass, "package deployment; public class Test {}\n");
        File deploymentTest = new File(deploymentModule, "src/test/java/deployment/GeneratedModelTest.java");
        deploymentTest.getParentFile().mkdirs();
        TestUtils.writeFile(deploymentTest, """
                package deployment;

                import static org.junit.jupiter.api.Assertions.assertNotNull;

                import java.nio.file.Files;
                import java.nio.file.Path;

                import org.junit.jupiter.api.Test;

                public class GeneratedModelTest {

                    @Test
                    public void serializedTestApplicationModelIsAvailable() throws Exception {
                        String model = System.getProperty("quarkus-internal-test.serialized-app-model.path");
                        assertNotNull(model);
                        Files.writeString(Path.of(System.getProperty("model.marker.file")), "true");
                    }
                }
                """);

        TestUtils.writeFile(new File(testProjectDir, "settings.gradle"), "include 'runtime', 'deployment'\n");
    }

    private String runtimeBuildFile() throws IOException {
        return """
                plugins {
                    id 'java'
                    id 'io.quarkus.extension'
                }

                group = 'org.acme'
                version = '1.0.0'

                repositories {
                    mavenCentral()
                    mavenLocal()
                }

                quarkusExtension {
                    disableValidation = true
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:%1$s")
                    implementation "io.quarkus:quarkus-arc"
                }

                tasks.register("resolveDeploymentTestApplicationModel") {
                    dependsOn(":deployment:testClasses")
                    doLast {
                        def mode = io.quarkus.runtime.LaunchMode.TEST
                        io.quarkus.gradle.tooling.ToolingUtils.create(project(":deployment"), mode)
                        println "resolved deployment test application model"
                    }
                }
                """.formatted(TestUtils.getCurrentQuarkusVersion());
    }

    private String deploymentBuildFile() throws IOException {
        return """
                plugins {
                    id 'java'
                }

                group = 'org.acme'
                version = '1.0.0'

                repositories {
                    mavenCentral()
                    mavenLocal()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:%1$s")
                    implementation "io.quarkus:quarkus-arc-deployment"
                    implementation project(":runtime")
                    testImplementation "org.junit.jupiter:junit-jupiter-api"
                    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine"
                }

                test {
                    def markerFile = layout.buildDirectory.file("model-marker.txt").get().asFile
                    systemProperty "model.marker.file", markerFile.absolutePath
                }
                """.formatted(TestUtils.getCurrentQuarkusVersion());
    }

    private void assertDeploymentTestApplicationModelMarker() throws IOException {
        Path marker = testProjectDir.toPath().resolve("deployment/build/model-marker.txt");
        assertThat(marker).exists();
        assertThat(Files.readString(marker)).isEqualTo("true");
    }
}
