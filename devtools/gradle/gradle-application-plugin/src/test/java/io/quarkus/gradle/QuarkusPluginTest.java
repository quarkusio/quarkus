package io.quarkus.gradle;

import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_BUILD_TASK_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class QuarkusPluginTest {

    @Test
    public void shouldCreateTasks() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);

        assertTrue(project.getPluginManager().hasPlugin(QuarkusPlugin.ID));

        TaskContainer tasks = project.getTasks();
        assertNotNull(tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_DEP_TASK_NAME));
        assertNotNull(tasks.getByName(QUARKUS_BUILD_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.QUARKUS_DEV_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.BUILD_NATIVE_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.LIST_EXTENSIONS_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.ADD_EXTENSION_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.IMAGE_BUILD_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.IMAGE_PUSH_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.DEPLOY_TASK_NAME));
    }

    @Test
    public void shouldMakeAssembleDependOnQuarkusBuild() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);
        project.getPluginManager().apply("base");

        TaskContainer tasks = project.getTasks();
        Task assemble = tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME);
        assertThat(getDependantProvidedTaskName(assemble))
                .contains(QUARKUS_BUILD_TASK_NAME);
    }

    @Test
    public void shouldMakeQuarkusDevAndQuarkusBuildDependOnClassesTask() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);
        project.getPluginManager().apply("java");

        TaskContainer tasks = project.getTasks();

        Task quarkusAppPartsBuild = tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME);
        assertThat(getDependantProvidedTaskName(quarkusAppPartsBuild))
                .contains(JavaPlugin.CLASSES_TASK_NAME)
                .contains(QuarkusPlugin.QUARKUS_GENERATE_CODE_TASK_NAME);

        Task quarkusDepBuild = tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_DEP_TASK_NAME);
        assertThat(getDependantProvidedTaskName(quarkusDepBuild))
                .isEmpty();

        Task quarkusBuild = tasks.getByName(QUARKUS_BUILD_TASK_NAME);
        assertThat(getDependantProvidedTaskName(quarkusBuild))
                .contains(QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME)
                .contains(QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME);

        Task quarkusDev = tasks.getByName(QuarkusPlugin.QUARKUS_DEV_TASK_NAME);
    }

    @Test
    public void shouldNotFailOnProjectDependenciesWithoutMain(@TempDir Path testProjectDir) throws IOException {
        var kotlinVersion = System.getProperty("kotlin_version", "2.4.20");
        var settingFile = testProjectDir.resolve("settings.gradle");
        var mppProjectDir = testProjectDir.resolve("mpp");
        var quarkusProjectDir = testProjectDir.resolve("quarkus");
        var mppBuild = mppProjectDir.resolve("build.gradle");
        var quarkusBuild = quarkusProjectDir.resolve("build.gradle");
        Files.createDirectory(mppProjectDir);
        Files.createDirectory(quarkusProjectDir);
        Files.writeString(settingFile, """
                pluginManagement {
                    plugins {
                        id 'org.jetbrains.kotlin.jvm' version "%1$s"
                        id 'org.jetbrains.kotlin.plugin.allopen' version "%1$s"
                    }
                }

                rootProject.name = "quarkus-mpp-sample"

                include(
                    "mpp",
                    "quarkus"
                )
                """.formatted(kotlinVersion));

        Files.writeString(mppBuild, """
                plugins {
                    id 'org.jetbrains.kotlin.jvm'
                }

                compileKotlin {
                    kotlinOptions.javaParameters = true
                }""");

        Files.writeString(quarkusBuild, """
                plugins {
                    id("io.quarkus")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation(project(":mpp"))
                }""");

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("quarkusGenerateCode", "--stacktrace")
                .build();

        assertEquals(SUCCESS, result.task(":quarkus:quarkusGenerateCode").getOutcome());
    }

    @Test
    public void shouldResolveSatisfiedConditionalDependenciesInProductionRuntimeClasspath(@TempDir Path testProjectDir)
            throws IOException {
        Path repository = testProjectDir.resolve("repo");
        writeMavenArtifact(repository, "org.acme", "parent-extension", "1.0", """
                conditional-dependencies=org.acme\\:satisfied-extension\\:\\:jar\\:1.0
                deployment-artifact=org.acme\\:parent-extension-deployment\\:1.0
                """);
        writeMavenArtifact(repository, "org.acme", "satisfied-extension", "1.0", """
                dependency-condition=org.condition\\:present
                deployment-artifact=org.acme\\:satisfied-extension-deployment\\:1.0
                """);
        writeMavenArtifact(repository, "org.condition", "present", "1.0", null);
        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'conditional-runtime'\n");
        Files.writeString(testProjectDir.resolve("build.gradle"), """
                import org.gradle.api.DefaultTask
                import org.gradle.api.file.ConfigurableFileCollection
                import org.gradle.api.file.RegularFileProperty
                import org.gradle.api.tasks.Classpath
                import org.gradle.api.tasks.OutputFile
                import org.gradle.api.tasks.TaskAction

                plugins {
                    id 'io.quarkus'
                }

                repositories {
                    maven {
                        url = uri('repo')
                    }
                }

                dependencies {
                    implementation 'org.acme:parent-extension:1.0'
                    implementation 'org.condition:present:1.0'
                }

                abstract class WriteClasspath extends DefaultTask {
                    @Classpath
                    abstract ConfigurableFileCollection getClasspath()

                    @OutputFile
                    abstract RegularFileProperty getOutputFile()

                    @TaskAction
                    void write() {
                        outputFile.get().asFile.text = classpath.files*.name.sort().join('\\n') + '\\n'
                    }
                }

                tasks.register('writeProductionRuntimeClasspath', WriteClasspath) {
                    classpath.from(configurations.quarkusProdRuntimeClasspathConfiguration)
                    outputFile.set(layout.buildDirectory.file('resolved-runtime.txt'))
                }
                """);

        BuildResult result = buildProductionRuntimeClasspath(testProjectDir);
        assertEquals(SUCCESS, result.task(":writeProductionRuntimeClasspath").getOutcome());
        assertResolvedProductionRuntimeClasspath(testProjectDir);

        BuildResult cachedResult = buildProductionRuntimeClasspath(testProjectDir,
                "--configuration-cache", "-Dorg.gradle.unsafe.isolated-projects=true");
        assertEquals(SUCCESS, cachedResult.task(":writeProductionRuntimeClasspath").getOutcome());
        assertThat(cachedResult.getOutput()).contains("Configuration cache entry stored");
        assertResolvedProductionRuntimeClasspath(testProjectDir);

        BuildResult reusedCachedResult = buildProductionRuntimeClasspath(testProjectDir,
                "--configuration-cache", "-Dorg.gradle.unsafe.isolated-projects=true");
        assertThat(reusedCachedResult.task(":writeProductionRuntimeClasspath").getOutcome()).isIn(SUCCESS, UP_TO_DATE);
        assertThat(reusedCachedResult.getOutput()).contains("Reusing configuration cache.");
        assertResolvedProductionRuntimeClasspath(testProjectDir);
    }

    @Test
    public void analyticsAfterBuild() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);

        TaskContainer tasks = project.getTasks();
        Task quarkusBuild = tasks.getByName(QUARKUS_BUILD_TASK_NAME);
    }

    private static List<String> getDependantProvidedTaskName(Task task) {
        List<String> dependantTaskNames = new ArrayList<>();
        for (Object t : task.getDependsOn()) {
            try {
                dependantTaskNames.add(((Provider<Task>) t).get().getName());
            } catch (ClassCastException e) {
                // Nothing to do here
            }
        }
        return dependantTaskNames;
    }

    private static BuildResult buildProductionRuntimeClasspath(Path testProjectDir, String... arguments) {
        List<String> gradleArguments = new ArrayList<>();
        gradleArguments.add("writeProductionRuntimeClasspath");
        gradleArguments.add("--stacktrace");
        gradleArguments.addAll(List.of(arguments));
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(gradleArguments)
                .build();
    }

    private static void assertResolvedProductionRuntimeClasspath(Path testProjectDir) {
        assertThat(testProjectDir.resolve("build/resolved-runtime.txt"))
                .content()
                .contains("parent-extension-1.0.jar")
                .contains("present-1.0.jar")
                .contains("satisfied-extension-1.0.jar");
    }

    private static void writeMavenArtifact(Path repository, String groupId, String artifactId, String version,
            String extensionDescriptor) throws IOException {
        Path artifactDirectory = repository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
        Files.createDirectories(artifactDirectory);
        String baseName = artifactId + "-" + version;
        Files.writeString(artifactDirectory.resolve(baseName + ".pom"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </project>
                """.formatted(groupId, artifactId, version));
        try (JarOutputStream jar = new JarOutputStream(
                Files.newOutputStream(artifactDirectory.resolve(baseName + ".jar")))) {
            if (extensionDescriptor != null) {
                jar.putNextEntry(new JarEntry("META-INF/quarkus-extension.properties"));
                jar.write(extensionDescriptor.getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        }
    }
}
