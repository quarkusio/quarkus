package io.quarkus.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.testing.BaseGradleTest;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.Dependency;

/**
 * Builds a multi-module project whose Quarkus application depends on a sibling module, in some tests as two copies at
 * different locations sharing one build cache, the way two CI pipelines that each check out into their own directory
 * do.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class RelocatedBuildCacheTest extends BaseGradleTest {

    /** The extension of the digest of a model, which the tasks consuming the model are keyed on. */
    private static final String KEY = ".digest";
    private static final String MODEL_DIR = "app/build/quarkus/application-model";
    private static final List<String> MODELS = List.of("quarkus-app-model", "quarkus-app-test-model",
            "quarkus-app-model-build");

    @InjectSoftAssertions
    SoftAssertions soft;

    @ParameterizedTest(name = "configuration cache: {0}")
    @ValueSource(booleans = { true, false })
    void outputsAreReusedFromAnotherCheckout(boolean configurationCache) throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path first = prepareCheckout("first");
        Path second = prepareCheckout("second");

        BuildResult firstBuild = build(first, cache, configurationCache);
        soft.assertThat(taskOutcomes(firstBuild))
                .describedAs("first checkout: %s", firstBuild.getOutput())
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.SUCCESS)
                .containsEntry(":app:test", TaskOutcome.SUCCESS);

        BuildResult secondBuild = build(second, cache, configurationCache);
        soft.assertThat(taskOutcomes(secondBuild))
                .describedAs("second checkout: %s", secondBuild.getOutput())
                .containsEntry(":app:quarkusGenerateCode", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusGenerateCodeTests", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:compileJava", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusBuild", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:test", TaskOutcome.FROM_CACHE);

        for (String model : MODELS) {
            // The models themselves differ, as they hold the checkout directory, so this test exercises relocation
            soft.assertThat(Files.readString(modelFile(second, model + ".dat")))
                    .describedAs("%s.dat", model)
                    .isNotEqualTo(Files.readString(modelFile(first, model + ".dat")));
            soft.assertThat(modelFile(second, model + KEY))
                    .describedAs("%s" + KEY, model)
                    .hasSameTextualContentAs(modelFile(first, model + KEY));
        }
    }

    @Test
    void newDependencyInvalidatesTheReusedOutputs() throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path first = prepareCheckout("first");
        Path second = prepareCheckout("second");
        build(first, cache, true);
        addDependency(second, "io.quarkus:quarkus-jackson");

        BuildResult result = build(second, cache, true);

        soft.assertThat(taskOutcomes(result))
                .describedAs("second checkout, with a new dependency: %s", result.getOutput())
                .containsEntry(":app:quarkusGenerateCode", TaskOutcome.SUCCESS)
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.SUCCESS)
                .containsEntry(":app:quarkusBuild", TaskOutcome.SUCCESS)
                .containsEntry(":app:test", TaskOutcome.SUCCESS);
        soft.assertThat(Files.readString(modelFile(second, "quarkus-app-model-build" + KEY)))
                .contains("quarkus-jackson")
                .isNotEqualTo(Files.readString(modelFile(first, "quarkus-app-model-build" + KEY)));
    }

    @Test
    void siblingModuleIsRecordedOnceWithoutTheConfigurationCache() throws Exception {
        Path checkout = prepareCheckout("checkout");

        // All the model tasks in one build, which without the configuration cache share one workspace module
        BuildResult result = runner(checkout, testProjectDir.resolve("cache"), false,
                ":app:quarkusGenerateTestAppModel", ":app:quarkusGenerateAppModel", ":app:quarkusBuildAppModel")
                .build();

        for (String model : MODELS) {
            ApplicationModel appModel = ToolingUtils.deserializeAppModel(modelFile(checkout, model + ".dat"));
            List<String> directDependencies = appModel.getAppArtifact().getWorkspaceModule().getDirectDependencies()
                    .stream().map(Dependency::getArtifactId).toList();
            soft.assertThat(directDependencies)
                    .describedAs("direct dependencies in %s.dat: %s", model, result.getOutput())
                    .containsOnlyOnce("common");
        }
    }

    @Test
    void testModelRecordsTheMainOutputDirectories() throws Exception {
        Path checkout = prepareCheckout("checkout");

        // The test model on its own, which used to run before the main classes and resources existed
        BuildResult result = runner(checkout, testProjectDir.resolve("cache"), true, ":app:quarkusGenerateTestAppModel")
                .build();

        ApplicationModel appModel = ToolingUtils.deserializeAppModel(modelFile(checkout, "quarkus-app-test-model.dat"));
        // Gradle records the canonical location, which on macOS differs from the temporary directory's
        Path appBuildDir = checkout.toRealPath().resolve("app/build");
        soft.assertThat(appModel.getAppArtifact().getResolvedPaths())
                .describedAs("resolved paths of the application in the test model: %s", result.getOutput())
                .contains(appBuildDir.resolve("classes/java/main"), appBuildDir.resolve("resources/main"));
    }

    @Test
    void mainModelRecordsTheMainResources() throws Exception {
        Path checkout = prepareCheckout("checkout");

        BuildResult result = runner(checkout, testProjectDir.resolve("cache"), true, ":app:quarkusGenerateAppModel")
                .build();

        ApplicationModel appModel = ToolingUtils.deserializeAppModel(modelFile(checkout, "quarkus-app-model.dat"));
        soft.assertThat(appModel.getAppArtifact().getResolvedPaths())
                .describedAs("resolved paths of the application in the main model: %s", result.getOutput())
                .contains(checkout.toRealPath().resolve("app/build/resources/main"));
    }

    @Test
    void quarkusBuildIsReusedWhenTheApplicationPartsComeFromTheCache() throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path checkout = prepareCheckout("checkout");
        runner(checkout, cache, true, ":app:quarkusBuild").build();
        runner(checkout, cache, true, "clean").build();

        // quarkusAppPartsBuild restored from the cache leaves its scratch directory empty, which used to be an input
        BuildResult result = runner(checkout, cache, true, ":app:quarkusBuild").build();

        soft.assertThat(taskOutcomes(result))
                .describedAs("after a clean: %s", result.getOutput())
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusBuild", TaskOutcome.FROM_CACHE);
    }

    @Test
    void changedSiblingModuleReachesTheBuiltApplication() throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path checkout = prepareCheckout("checkout");
        runner(checkout, cache, true, ":app:quarkusBuild").build();
        // Changes the content of the sibling's jar, but not the application model
        replaceIn(checkout.resolve("common/src/main/java/org/acme/common/Greeting.java"), "\"hello\"", "\"bonjour\"");

        BuildResult result = runner(checkout, cache, true, ":app:quarkusBuild").build();

        soft.assertThat(taskOutcomes(result))
                .describedAs("after changing the sibling module: %s", result.getOutput())
                .containsEntry(":app:quarkusBuild", TaskOutcome.SUCCESS);
        soft.assertThat(classInJar(checkout.resolve("app/build/quarkus-app/lib/main"), "org/acme/common/Greeting.class"))
                .describedAs("Greeting.class in the built application")
                .contains("bonjour");
    }

    @ParameterizedTest(name = "configuration cache: {0}")
    @ValueSource(booleans = { true, false })
    void outputsAreReusedWithAMavenLocalRepositoryAtAnotherLocation(boolean configurationCache) throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path first = prepareCheckout("first");
        Path second = prepareCheckout("second");
        // The Quarkus artifacts stay where they are; only the extra artifact moves with the Maven local repository
        String quarkusArtifacts = "maven { url = uri(\"" + portable(realMavenLocalRepository()) + "\") }";
        for (Path checkout : List.of(first, second)) {
            appendToBuildScript(checkout, "repositories { " + quarkusArtifacts + " }");
            addDependency(checkout, "org.acme:extra:1.0");
        }
        Path firstRepository = installExtraArtifact(testProjectDir.resolve("first-m2"));
        Path secondRepository = installExtraArtifact(testProjectDir.resolve("second-m2"));

        BuildResult firstBuild = runner(first, cache, configurationCache, "build",
                "-Dmaven.repo.local=" + firstRepository).build();
        BuildResult secondBuild = runner(second, cache, configurationCache, "build",
                "-Dmaven.repo.local=" + secondRepository).build();

        soft.assertThat(taskOutcomes(secondBuild))
                .describedAs("second checkout: %s", secondBuild.getOutput())
                .containsEntry(":app:quarkusGenerateCode", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusBuild", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:test", TaskOutcome.FROM_CACHE);
        String digest = Files.readString(modelFile(second, "quarkus-app-model-build" + KEY));
        soft.assertThat(digest)
                .describedAs("digest of the second checkout's model, first checkout: %s", firstBuild.getOutput())
                .contains("${artifact:org.acme:extra::jar:1.0#0}")
                .doesNotContain(portable(secondRepository.toRealPath()))
                .isEqualTo(Files.readString(modelFile(first, "quarkus-app-model-build" + KEY)));
    }

    @Test
    void fileDependencyOutsideTheProjectIsNotReusedFromAnotherCheckout() throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path first = prepareCheckout("first");
        Path second = prepareCheckout("second");
        for (Path checkout : List.of(first, second)) {
            // The same jar, outside each checkout
            Path jar = writeExtraJar(testProjectDir.resolve(checkout.getFileName() + "-libs").resolve("extra.jar"));
            appendToBuildScript(checkout, "dependencies { implementation(files(\"" + portable(jar) + "\")) }");
        }

        assertMissesInTheSecondCheckout(first, second, cache);
    }

    @Test
    void includedBuildOutsideTheProjectIsReusedFromAnotherCheckout() throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path first = prepareCheckout("first");
        Path second = prepareCheckout("second");
        for (Path checkout : List.of(first, second)) {
            moveSiblingToAnIncludedBuild(checkout);
        }
        build(first, cache, true);
        FileUtils.deleteDirectory(first.toFile());
        FileUtils.deleteDirectory(testProjectDir.resolve("first-included").toFile());

        BuildResult result = build(second, cache, true);

        soft.assertThat(taskOutcomes(result))
                .describedAs("second checkout: %s", result.getOutput())
                .containsEntry(":app:quarkusGenerateCode", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusBuild", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:test", TaskOutcome.FROM_CACHE);
        soft.assertThat(Files.readString(modelFile(second, "quarkus-app-model-build" + KEY)))
                .describedAs("key of the second checkout's model")
                .doesNotContain("second-included");
    }

    @Test
    void changedIncludedBuildIsRebuiltInAnotherCheckout() throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path first = prepareCheckout("first");
        Path second = prepareCheckout("second");
        for (Path checkout : List.of(first, second)) {
            moveSiblingToAnIncludedBuild(checkout);
        }
        runner(first, cache, true, ":app:quarkusBuild").build();
        replaceIn(testProjectDir.resolve("second-included/common/src/main/java/org/acme/common/Greeting.java"),
                "\"hello\"", "\"bonjour\"");

        BuildResult result = runner(second, cache, true, ":app:quarkusBuild").build();

        soft.assertThat(taskOutcomes(result))
                .describedAs("second checkout: %s", result.getOutput())
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.SUCCESS)
                .containsEntry(":app:quarkusBuild", TaskOutcome.SUCCESS);
        soft.assertThat(classInJar(second.resolve("app/build/quarkus-app/lib/main"), "org/acme/common/Greeting.class"))
                .describedAs("Greeting.class in the built application")
                .contains("bonjour");
    }

    @Test
    void modelIsRebuiltWhenTheResourcesDirectoryAppears() throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path checkout = prepareCheckout("checkout");
        FileUtils.deleteDirectory(checkout.resolve("app/src/main/resources").toFile());
        runner(checkout, cache, true, ":app:quarkusBuild").build();
        Files.createDirectories(checkout.resolve("app/src/main/resources"));
        Files.writeString(checkout.resolve("app/src/main/resources/application.properties"), "greeting=hi\n");

        BuildResult result = runner(checkout, cache, true, ":app:quarkusBuild").build();

        ApplicationModel appModel = ToolingUtils.deserializeAppModel(modelFile(checkout, "quarkus-app-model-build.dat"));
        soft.assertThat(appModel.getAppArtifact().getResolvedPaths())
                .describedAs("resolved paths of the application after adding the first resource: %s", result.getOutput())
                .contains(checkout.toRealPath().resolve("app/build/resources/main"));
        soft.assertThat(classInJar(checkout.resolve("app/build/quarkus-app/app"), "application.properties"))
                .describedAs("application.properties in the built application")
                .contains("greeting=hi");
    }

    @Test
    void changedDeploymentArtifactRebuildsTheApplication() throws Exception {
        Path cache = testProjectDir.resolve("cache");
        Path checkout = prepareCheckout("checkout");
        // An extension in a file repository, which Gradle uses in place: its deployment artifact can change
        // without its location changing, as a snapshot in the Maven local repository does
        Path repository = testProjectDir.resolve("extension-repo");
        Path runtimeDir = repository.resolve("org/acme/ext/1.0");
        Path deploymentDir = repository.resolve("org/acme/ext-deployment/1.0");
        writeJar(runtimeDir.resolve("ext-1.0.jar"), Map.of("META-INF/quarkus-extension.properties",
                "deployment-artifact=org.acme\\:ext-deployment\\:1.0\n"));
        Files.writeString(runtimeDir.resolve("ext-1.0.pom"), pom("ext", ""));
        writeJar(deploymentDir.resolve("ext-deployment-1.0.jar"), Map.of("marker.txt", "one"));
        Files.writeString(deploymentDir.resolve("ext-deployment-1.0.pom"), pom("ext-deployment", """
                <dependencies>
                  <dependency>
                    <groupId>org.acme</groupId>
                    <artifactId>ext</artifactId>
                    <version>1.0</version>
                  </dependency>
                </dependencies>
                """));
        appendToBuildScript(checkout, "repositories { maven { url = uri(\"" + portable(repository) + "\") } }");
        addDependency(checkout, "org.acme:ext:1.0");
        BuildResult firstBuild = runner(checkout, cache, true, ":app:quarkusBuild").build();
        writeJar(deploymentDir.resolve("ext-deployment-1.0.jar"), Map.of("marker.txt", "two, and longer"));

        BuildResult result = runner(checkout, cache, true, ":app:quarkusBuild").build();

        soft.assertThat(Files.readString(modelFile(checkout, "quarkus-app-model-build" + KEY)))
                .describedAs("key of the model: %s", firstBuild.getOutput())
                .contains("ext-deployment");
        soft.assertThat(taskOutcomes(result))
                .describedAs("after changing the deployment artifact: %s", result.getOutput())
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.SUCCESS);
    }

    /**
     * Moves the sibling module to a build of its own, next to the checkout. It stays a subproject there: depending
     * on the root project of an included build fails to resolve the deployment classpath.
     */
    private void moveSiblingToAnIncludedBuild(Path checkout) throws IOException {
        Path included = testProjectDir.resolve(checkout.getFileName() + "-included");
        FileUtils.moveDirectory(checkout.resolve("common").toFile(), included.resolve("common").toFile());
        Files.writeString(included.resolve("settings.gradle.kts"),
                "rootProject.name = \"included\"\ninclude(\"common\")\n");
        Path commonBuildFile = included.resolve("common/build.gradle.kts");
        Files.writeString(commonBuildFile,
                Files.readString(commonBuildFile) + "\ngroup = \"org.acme\"\nversion = \"1.0\"\n");
        replaceIn(checkout.resolve("settings.gradle.kts"), "include(\"common\", \"app\")",
                "include(\"app\")\nincludeBuild(\"" + portable(included) + "\")");
        replaceIn(checkout.resolve("app/build.gradle.kts"), "implementation(project(\":common\"))",
                "implementation(\"org.acme:common:1.0\")");
    }

    private static String pom(String artifactId, String body) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.acme</groupId>
                  <artifactId>%s</artifactId>
                  <version>1.0</version>
                  %s
                </project>
                """.formatted(artifactId, body);
    }

    private static void writeJar(Path jar, Map<String, String> entries) throws IOException {
        Files.createDirectories(jar.getParent());
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            for (Map.Entry<String, String> e : new java.util.TreeMap<>(entries).entrySet()) {
                JarEntry entry = new JarEntry(e.getKey());
                entry.setTime(0);
                out.putNextEntry(entry);
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }

    /**
     * Builds both checkouts, the second one after deleting the first, and expects the second to rebuild the
     * application instead of restoring outputs that were built from the first checkout's files.
     */
    private void assertMissesInTheSecondCheckout(Path first, Path second, Path cache) throws IOException {
        build(first, cache, true);
        FileUtils.deleteDirectory(first.toFile());

        BuildResult result = build(second, cache, true);

        soft.assertThat(taskOutcomes(result))
                .describedAs("second checkout: %s", result.getOutput())
                .containsEntry(":app:quarkusGenerateCode", TaskOutcome.SUCCESS)
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.SUCCESS)
                .containsEntry(":app:quarkusBuild", TaskOutcome.SUCCESS)
                .containsEntry(":app:test", TaskOutcome.SUCCESS);
    }

    private BuildResult build(Path checkout, Path cache, boolean configurationCache) {
        return runner(checkout, cache, configurationCache, "build").build();
    }

    private static GradleRunner runner(Path checkout, Path cache, boolean configurationCache, String... tasks) {
        List<String> args = new ArrayList<>(List.of(tasks));
        args.add("--build-cache");
        args.add("--stacktrace");
        args.add(configurationCache ? CONFIGURATION_CACHE : NO_CONFIGURATION_CACHE);
        args.add("-PsharedBuildCache=" + cache.toAbsolutePath());
        // With CI set, quarkusBuild is not stored in the cache by default
        Map<String, String> env = new HashMap<>(System.getenv());
        env.remove("CI");
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(checkout.toFile())
                .withEnvironment(env)
                .withArguments(args);
    }

    private Path prepareCheckout(String name) throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/caching/relocation");
        Path checkout = testProjectDir.resolve(name);
        FileUtils.copyDirectory(new File(url.toURI()), checkout.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), checkout.resolve("gradle.properties").toFile());
        return checkout;
    }

    private static void addDependency(Path checkout, String coordinates) throws IOException {
        appendToBuildScript(checkout, "dependencies { implementation(\"" + coordinates + "\") }");
    }

    private static void appendToBuildScript(Path checkout, String block) throws IOException {
        Path buildFile = checkout.resolve("app/build.gradle.kts");
        Files.writeString(buildFile, Files.readString(buildFile) + "\n" + block + "\n");
    }

    private static void replaceIn(Path file, String target, String replacement) throws IOException {
        String content = Files.readString(file);
        assertThat(content).describedAs("%s", file).contains(target);
        Files.writeString(file, content.replace(target, replacement));
    }

    /**
     * The Maven local repository of the build running the tests, where the Quarkus artifacts under test are installed.
     */
    private static Path realMavenLocalRepository() {
        return Path.of(Objects.requireNonNull(System.getProperty("maven.repo.local"), "maven.repo.local"));
    }

    private static Path installExtraArtifact(Path repository) throws IOException {
        Path dir = repository.resolve("org/acme/extra/1.0");
        writeExtraJar(dir.resolve("extra-1.0.jar"));
        Files.writeString(dir.resolve("extra-1.0.pom"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.acme</groupId>
                  <artifactId>extra</artifactId>
                  <version>1.0</version>
                </project>
                """);
        return repository;
    }

    /**
     * Writes the same bytes wherever it is written, so that only the location differs between checkouts.
     */
    private static Path writeExtraJar(Path jar) throws IOException {
        Files.createDirectories(jar.getParent());
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            JarEntry entry = new JarEntry(JarFile.MANIFEST_NAME);
            entry.setTime(0);
            out.putNextEntry(entry);
            manifest.write(out);
            out.closeEntry();
        }
        return jar;
    }

    private static String classInJar(Path dir, String entryName) throws IOException {
        try (Stream<Path> jars = Files.list(dir)) {
            for (Path jar : jars.filter(p -> p.getFileName().toString().endsWith(".jar")).toList()) {
                try (JarFile jarFile = new JarFile(jar.toFile())) {
                    JarEntry entry = jarFile.getJarEntry(entryName);
                    if (entry != null) {
                        return new String(jarFile.getInputStream(entry).readAllBytes(), StandardCharsets.ISO_8859_1);
                    }
                }
            }
        }
        throw new AssertionError("no jar in " + dir + " contains " + entryName);
    }

    /**
     * A path as it can be written in a Kotlin string literal, and as the digest records it.
     */
    private static String portable(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static Path modelFile(Path checkout, String name) {
        return checkout.resolve(MODEL_DIR).resolve(name);
    }
}
