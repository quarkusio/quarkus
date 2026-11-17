package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
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

    private static Path modelFile(Path checkout, String name) {
        return checkout.resolve(MODEL_DIR).resolve(name);
    }
}
