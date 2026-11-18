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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

    private static final String MODEL_DIR = "app/build/quarkus/application-model";
    private static final List<String> MODELS = List.of("quarkus-app-model", "quarkus-app-test-model",
            "quarkus-app-model-build");

    @InjectSoftAssertions
    SoftAssertions soft;

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

    private static void replaceIn(Path file, String target, String replacement) throws IOException {
        String content = Files.readString(file);
        assertThat(content).describedAs("%s", file).contains(target);
        Files.writeString(file, content.replace(target, replacement));
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

    private static Path modelFile(Path checkout, String name) {
        return checkout.resolve(MODEL_DIR).resolve(name);
    }
}
