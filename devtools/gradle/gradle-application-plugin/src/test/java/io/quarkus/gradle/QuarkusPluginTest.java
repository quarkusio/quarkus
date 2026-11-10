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

    private static final String WRITE_RUNTIME_COMPONENTS_TASK_NAME = "writeProductionRuntimeComponents";

    /**
     * Consumes the production runtime classpath as a resolved dependency graph.
     * <p>
     * This project must not also consume the configuration as a file collection, for example with
     * {@code classpath.from(configurations.quarkusProdRuntimeClasspathConfiguration)}. Registering such a consumer
     * makes the conditional dependencies show up in the dependency graph as well, no matter which of the two tasks
     * runs first, which hides the regression this test guards against.
     */
    private static final String WRITE_RUNTIME_COMPONENTS_TASK = """
            abstract class WriteComponents extends DefaultTask {
                @Internal
                abstract Property<ResolvedComponentResult> getRootComponent()

                @OutputFile
                abstract RegularFileProperty getOutputFile()

                @TaskAction
                void write() {
                    def modules = new TreeSet<String>()
                    collectModules(rootComponent.get(), modules, new HashSet<Object>())
                    outputFile.get().asFile.text = modules.join('\\n') + '\\n'
                }

                private static void collectModules(ResolvedComponentResult component, Set<String> modules,
                        Set<Object> visited) {
                    if (!visited.add(component.id)) {
                        return
                    }
                    def module = component.moduleVersion
                    if (module != null) {
                        modules.add("${module.group}:${module.name}:${module.version}".toString())
                    }
                    for (dependency in component.dependencies) {
                        if (dependency instanceof ResolvedDependencyResult) {
                            collectModules(dependency.selected, modules, visited)
                        }
                    }
                }
            }

            tasks.register('%s', WriteComponents) {
                rootComponent.set(configurations.quarkusProdRuntimeClasspathConfiguration.incoming
                    .resolutionResult.rootComponent)
                outputFile.set(layout.buildDirectory.file('resolved-components.txt'))
            }
            """.formatted(WRITE_RUNTIME_COMPONENTS_TASK_NAME);

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

    /**
     * The production runtime classpath must include the satisfied conditional dependencies of the extensions it
     * contains, also when it is the first configuration that gets resolved in the build.
     * <p>
     * The classpath is consumed as a resolved dependency graph, which is how dependency reporting tools read it, for
     * example the {@code dependencies} report task and the GitHub dependency submission plugin. Graph resolution
     * snapshots the component metadata rules at the very beginning of the resolution, so it is the path that detects
     * variants registered too late. Consuming the configuration as a file collection does not detect it.
     *
     * @see <a href="https://github.com/quarkusio/quarkus/issues/58307">quarkusio/quarkus#58307</a>
     */
    @Test
    public void shouldResolveSatisfiedConditionalDependenciesInProductionRuntimeClasspath(@TempDir Path testProjectDir)
            throws IOException {
        writeConditionalDependencyProject(testProjectDir);

        BuildResult result = runInTestProject(testProjectDir, WRITE_RUNTIME_COMPONENTS_TASK_NAME);
        assertEquals(SUCCESS, result.task(":" + WRITE_RUNTIME_COMPONENTS_TASK_NAME).getOutcome());
        assertResolvedProductionRuntimeClasspath(testProjectDir);

        BuildResult cachedResult = runInTestProject(testProjectDir, WRITE_RUNTIME_COMPONENTS_TASK_NAME,
                "--configuration-cache", "-Dorg.gradle.unsafe.isolated-projects=true");
        assertEquals(SUCCESS, cachedResult.task(":" + WRITE_RUNTIME_COMPONENTS_TASK_NAME).getOutcome());
        assertThat(cachedResult.getOutput()).contains("Configuration cache entry stored");
        assertResolvedProductionRuntimeClasspath(testProjectDir);

        BuildResult reusedCachedResult = runInTestProject(testProjectDir, WRITE_RUNTIME_COMPONENTS_TASK_NAME,
                "--configuration-cache", "-Dorg.gradle.unsafe.isolated-projects=true");
        assertThat(reusedCachedResult.task(":" + WRITE_RUNTIME_COMPONENTS_TASK_NAME).getOutcome())
                .isIn(SUCCESS, UP_TO_DATE);
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

    private static BuildResult runInTestProject(Path testProjectDir, String taskName, String... arguments) {
        List<String> gradleArguments = new ArrayList<>();
        gradleArguments.add(taskName);
        gradleArguments.add("--stacktrace");
        gradleArguments.addAll(List.of(arguments));
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(gradleArguments)
                .build();
    }

    /**
     * Writes a project that depends on an extension with a satisfied conditional dependency and that reports the
     * resolved production runtime classpath components.
     *
     * @param testProjectDir project directory
     */
    private static void writeConditionalDependencyProject(Path testProjectDir) throws IOException {
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
                import org.gradle.api.artifacts.result.ResolvedComponentResult
                import org.gradle.api.artifacts.result.ResolvedDependencyResult
                import org.gradle.api.file.RegularFileProperty
                import org.gradle.api.provider.Property
                import org.gradle.api.tasks.Internal
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

                """ + WRITE_RUNTIME_COMPONENTS_TASK);
    }

    private static void assertResolvedProductionRuntimeClasspath(Path testProjectDir) {
        assertThat(testProjectDir.resolve("build/resolved-components.txt"))
                .content()
                .contains("org.acme:parent-extension:1.0")
                .contains("org.condition:present:1.0")
                .contains("org.acme:satisfied-extension:1.0");
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
