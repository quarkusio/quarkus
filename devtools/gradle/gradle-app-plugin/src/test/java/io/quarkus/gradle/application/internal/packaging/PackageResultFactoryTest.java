package io.quarkus.gradle.application.internal.packaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.execution.BuildRequest;
import io.quarkus.gradle.application.internal.planning.OutputLayout;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class PackageResultFactoryTest {

    private final PackageResultFactory factory = new PackageResultFactory();

    @TempDir
    Path directory;

    @Test
    void extractsFastJarFactsFromAugmentResult() {
        Path root = directory.resolve("build/quarkus-builds/app/package");
        var result = factory.fromAugmentResult(
                request(QuarkusApplicationBuildType.FAST_JAR, root),
                new AugmentResult(
                        List.of(new ArtifactResult(root.resolve("quarkus-run.jar"), "jar",
                                Map.of("library-dir", root.resolve("lib").toString()))),
                        new JarResult(root.resolve("quarkus-run.jar"), root.resolve("app/app.jar"), root.resolve("lib"),
                                false, "runner"),
                        null,
                        Map.of()));

        assertThat(result.buildName()).isEqualTo("app");
        assertThat(result.buildType()).isEqualTo(QuarkusApplicationBuildType.FAST_JAR);
        assertThat(result.jarPath()).isEqualTo(root.resolve("quarkus-run.jar"));
        assertThat(result.libraryDirectory()).contains(root.resolve("lib"));
        assertThat(result.mutable()).isFalse();
        assertThat(result.uberJar()).isFalse();
        assertThat(result.classifier()).contains("runner");
        assertThat(result.artifacts()).singleElement().satisfies(artifact -> {
            assertThat(artifact.type()).isEqualTo("jar");
            assertThat(artifact.metadata()).containsKey("library-dir");
        });
    }

    @Test
    void extractsAllJvmPackageShapes() {
        Path root = directory.resolve("build/quarkus-builds/app/package");

        assertThat(factory.fromAugmentResult(
                request(QuarkusApplicationBuildType.LEGACY_JAR, root),
                augmentResult(root, false, root.resolve("lib"))).buildType())
                .isEqualTo(QuarkusApplicationBuildType.LEGACY_JAR);
        assertThat(factory.fromAugmentResult(
                request(QuarkusApplicationBuildType.MUTABLE_JAR, root),
                augmentResult(root, true, root.resolve("lib"))).mutable())
                .isTrue();
        assertThat(factory.fromAugmentResult(
                request(QuarkusApplicationBuildType.UBER_JAR, root),
                augmentResult(root, false, null)).uberJar())
                .isTrue();
    }

    @Test
    void rejectsShapeMismatch() {
        Path root = directory.resolve("build/quarkus-builds/app/package");

        assertThatThrownBy(() -> factory.fromAugmentResult(
                request(QuarkusApplicationBuildType.FAST_JAR, root),
                new AugmentResult(
                        List.of(),
                        new JarResult(root.resolve("app-runner.jar"), root.resolve("app.jar"), null, false, null),
                        null,
                        Map.of())))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("expected fast-jar")
                .hasMessageContaining("uber jar");
    }

    private static BuildRequest request(QuarkusApplicationBuildType type, Path root) {
        return new BuildRequest(
                QuarkusApplicationBuildDescriptor.of("app", type),
                root,
                root.resolve("app-model.dat"),
                List.of(),
                Set.of(),
                new EffectiveConfigPlan(Map.of(), Map.of(), Map.of(), Map.of()),
                Map.of("quarkus.package.output-name", "app"),
                Map.of(),
                true,
                new OutputLayout(root, root.resolve("gen"), root.resolve("app"), Optional.empty()));
    }

    private static AugmentResult augmentResult(Path root, boolean mutable, Path libraryDirectory) {
        return new AugmentResult(
                List.of(),
                new JarResult(root.resolve("app-runner.jar"), root.resolve("app.jar"), libraryDirectory, mutable, null),
                null,
                Map.of());
    }
}
