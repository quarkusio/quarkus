package io.quarkus.gradle.application.internal.nativeimage;

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
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.execution.BuildRequest;
import io.quarkus.gradle.application.internal.planning.OutputLayout;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class NativeResultFactoryTest {

    private final NativeResultFactory factory = new NativeResultFactory();

    @TempDir
    Path directory;

    @Test
    void extractsNativeExecutableFactsFromAugmentResult() {
        Path root = directory.resolve("build/quarkus-builds/native1/package");
        var result = factory.fromAugmentResult(
                request(QuarkusApplicationBuildType.NATIVE_EXECUTABLE, root),
                new AugmentResult(
                        List.of(new ArtifactResult(root.resolve("my-native-runner"), "native", Map.of("key", "value"))),
                        null,
                        root.resolve("my-native-runner"),
                        Map.of("java-version", "21")));

        assertThat(result.buildType()).isEqualTo(QuarkusApplicationBuildType.NATIVE_EXECUTABLE);
        assertThat(result.executablePath()).contains(root.resolve("my-native-runner"));
        assertThat(result.sourcesDirectory()).isEmpty();
        assertThat(result.graalVMInfo()).containsEntry("java-version", "21");
        assertThat(result.artifacts()).singleElement().satisfies(artifact -> {
            assertThat(artifact.type()).isEqualTo("native");
            assertThat(artifact.path()).contains(root.resolve("my-native-runner"));
        });
    }

    @Test
    void extractsNativeSourcesFactsWithoutUsingArtifactPathAsOutputDirectory() {
        Path root = directory.resolve("build/quarkus-builds/sources1/package");
        Path sourceJar = root.resolve("native-image-source-jar/app.jar");
        var result = factory.fromAugmentResult(
                request(QuarkusApplicationBuildType.NATIVE_SOURCES, root),
                new AugmentResult(
                        List.of(new ArtifactResult(sourceJar, "native-sources", Map.of())),
                        null,
                        null,
                        Map.of()));

        assertThat(result.buildType()).isEqualTo(QuarkusApplicationBuildType.NATIVE_SOURCES);
        assertThat(result.executablePath()).isEmpty();
        assertThat(result.sourcesDirectory()).contains(root.resolve("native-sources"));
        assertThat(result.sourceJarPath()).contains(sourceJar);
        assertThat(result.nativeImageArgsPath()).contains(root.resolve("native-sources/native-image.args"));
    }

    @Test
    void rejectsMissingNativeExecutablePath() {
        Path root = directory.resolve("build/quarkus-builds/native1/package");

        assertThatThrownBy(() -> factory.fromAugmentResult(
                request(QuarkusApplicationBuildType.NATIVE_EXECUTABLE, root),
                new AugmentResult(List.of(), null, null, Map.of())))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("did not report a native executable path");
    }

    private static BuildRequest request(QuarkusApplicationBuildType type, Path root) {
        return new BuildRequest(
                QuarkusApplicationBuildDescriptor.of(type.isNativeSources() ? "sources1" : "native1", type),
                root,
                root.resolve("app-model.dat"),
                List.of(),
                Set.of(),
                new EffectiveConfigPlan(Map.of(), Map.of(), Map.of(), Map.of()),
                Map.of("quarkus.package.output-name", "my-native"),
                Map.of(),
                true,
                new OutputLayout(root, root.resolve("gen"), root.resolve("app"), Optional.empty()));
    }
}
