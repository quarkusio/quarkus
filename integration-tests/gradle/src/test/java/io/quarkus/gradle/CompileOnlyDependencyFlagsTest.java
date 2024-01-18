package io.quarkus.gradle;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.wrapper.GradleUserHomeLookup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.runtime.LaunchMode;

public class CompileOnlyDependencyFlagsTest {

    @Test
    public void compileOnlyFlags() throws Exception {
        var projectDir = QuarkusGradleTestBase.getProjectDir("compile-only-dependency-flags");

        final String componly = ArtifactCoords.jar("org.acme", "componly", "1.0.0-SNAPSHOT").toCompactCoords();
        final String common = ArtifactCoords.jar("org.acme", "common", "1.0.0-SNAPSHOT").toCompactCoords();
        var expectedCompileOnly = Set.of(componly, common);

        final Map<String, Map<String, Integer>> compileOnlyDeps;
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(new File(projectDir, "quarkus"))
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome())
                .connect()) {
            final GradleActionOutcome<Map<String, Map<String, Integer>>> outcome = GradleActionOutcome.of();
            connection.action((BuildAction<Map<String, Map<String, Integer>>>) controller -> {
                var result = new HashMap<String, Map<String, Integer>>();
                result.put(LaunchMode.DEVELOPMENT.name(), readCompileOnlyDeps(controller, LaunchMode.DEVELOPMENT.name()));
                result.put(LaunchMode.TEST.name(), readCompileOnlyDeps(controller, LaunchMode.TEST.name()));
                result.put(LaunchMode.NORMAL.name(), readCompileOnlyDeps(controller, LaunchMode.NORMAL.name()));
                return result;
            }).run(outcome);
            compileOnlyDeps = outcome.getResult();
        }

        var compileOnly = compileOnlyDeps.get(LaunchMode.DEVELOPMENT.name());
        // the following line results in ClassNotFoundException: com.sun.jna.Library
        // assertThat(compileOnly).containsOnlyKeys(expectedCompileOnly);
        // so I am not using the assertj api here
        assertEqual(compileOnly, expectedCompileOnly);
        assertOnlyFlagsSet(common, compileOnly.get(common),
                DependencyFlags.COMPILE_ONLY,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.RELOADABLE,
                DependencyFlags.WORKSPACE_MODULE,
                DependencyFlags.DIRECT);
        assertOnlyFlagsSet(componly, compileOnly.get(componly),
                DependencyFlags.COMPILE_ONLY,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.RELOADABLE,
                DependencyFlags.WORKSPACE_MODULE,
                DependencyFlags.DIRECT);

        compileOnly = compileOnlyDeps.get(LaunchMode.TEST.name());
        assertEqual(compileOnly, expectedCompileOnly);
        assertOnlyFlagsSet(common, compileOnly.get(common),
                DependencyFlags.COMPILE_ONLY,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.RELOADABLE,
                DependencyFlags.WORKSPACE_MODULE,
                DependencyFlags.DIRECT);
        assertOnlyFlagsSet(componly, compileOnly.get(componly),
                DependencyFlags.COMPILE_ONLY);

        compileOnly = compileOnlyDeps.get(LaunchMode.NORMAL.name());
        assertEqual(compileOnly, expectedCompileOnly);
        assertOnlyFlagsSet(common, compileOnly.get(common),
                DependencyFlags.COMPILE_ONLY,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.DIRECT);
        assertOnlyFlagsSet(componly, compileOnly.get(componly),
                DependencyFlags.COMPILE_ONLY);
    }

    private static void assertOnlyFlagsSet(String coords, int flags, int... expectedFlags) {
        int expected = 0;
        for (var i : expectedFlags) {
            expected |= i;
        }
        if (expected == flags) {
            return;
        }
        StringBuilder sb = null;
        for (var flag : expectedFlags) {
            if ((flags & flag) != flag) {
                if (sb == null) {
                    sb = new StringBuilder().append("Expected ").append(coords).append(" to have ").append(flag);
                } else {
                    sb.append(", ").append(flag);
                }
            }
        }
        if (sb != null) {
            Assertions.fail(sb.toString());
        }
        Assertions.fail("Extra flags are set for " + coords + ": " + (flags - expected));
    }

    private static void assertEqual(Map<String, Integer> compileOnly, Set<String> expectedCompileOnly) {
        if (!compileOnly.keySet().equals(expectedCompileOnly)) {
            Assertions.fail("Expected " + expectedCompileOnly + " but got " + compileOnly.keySet());
        }
    }

    private static Map<String, Integer> readCompileOnlyDeps(BuildController controller, String modeName) {
        var model = controller.getModel(ApplicationModel.class, ModelParameter.class, mode -> mode.setMode(modeName));
        var result = new HashMap<String, Integer>();
        for (var d : model.getDependencies(DependencyFlags.COMPILE_ONLY)) {
            result.put(ArtifactCoords.of(
                    d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion()).toCompactCoords(),
                    d.getFlags());
        }
        return result;
    }

    public static class GradleActionOutcome<T> implements ResultHandler<T> {

        public static <T> GradleActionOutcome<T> of() {
            return new GradleActionOutcome<T>();
        }

        private CompletableFuture<T> future = new CompletableFuture<>();
        private Exception error;

        public T getResult() {
            try {
                T result = future.get();
                if (error == null) {
                    return result;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to perform a Gradle action", e);
            }
            throw new RuntimeException("Failed to perform a Gradle action", error);
        }

        @Override
        public void onComplete(T result) {
            future.complete(result);
        }

        @Override
        public void onFailure(GradleConnectionException failure) {
            this.error = failure;
            future.complete(null);
        }
    }
}
