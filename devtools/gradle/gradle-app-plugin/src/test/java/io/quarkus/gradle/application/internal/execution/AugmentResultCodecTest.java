package io.quarkus.gradle.application.internal.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.JarResult;

class AugmentResultCodecTest {

    private final AugmentResultCodec codec = new AugmentResultCodec();

    @TempDir
    Path directory;

    @Test
    void writesDeterministicArtifactResultFacts() throws Exception {
        Path file = directory.resolve("image/augmentation-result.properties");
        var result = new AugmentResult(
                List.of(new ArtifactResult(Path.of("build/app"), "jar-container",
                        Map.of("container-image", "quay.io/acme/app:1.0"))),
                new JarResult(Path.of("build/quarkus-run.jar"), Path.of("build/app.jar"), Path.of("build/lib"),
                        false, "runner"),
                Path.of("build/app-runner"),
                Map.of("java-version", "21"));

        codec.write(file, result);

        assertThat(java.nio.file.Files.readString(file)).isEqualTo("""
                graalvm.java-version=21
                jar.classifier=runner
                jar.library-dir=build/lib
                jar.mutable=false
                jar.original-artifact=build/app.jar
                jar.path=build/quarkus-run.jar
                native.result=build/app-runner
                result.0.metadata.container-image=quay.io/acme/app\\:1.0
                result.0.path=build/app
                result.0.type=jar-container
                result.count=1
                schema.version=1
                """);
        assertThat(codec.readArtifactResults(file))
                .singleElement()
                .satisfies(artifact -> {
                    assertThat(artifact.getPath()).isEqualTo(Path.of("build/app"));
                    assertThat(artifact.getType()).isEqualTo("jar-container");
                    assertThat(artifact.getMetadata()).containsEntry("container-image", "quay.io/acme/app:1.0");
                });
        AugmentResult read = codec.read(file);
        assertThat(read.getJar().getPath()).isEqualTo(Path.of("build/quarkus-run.jar"));
        assertThat(read.getJar().getOriginalArtifact()).isEqualTo(Path.of("build/app.jar"));
        assertThat(read.getJar().getLibraryDir()).isEqualTo(Path.of("build/lib"));
        assertThat(read.getJar().mutable()).isFalse();
        assertThat(read.getJar().getClassifier()).isEqualTo("runner");
        assertThat(read.getNativeResult()).isEqualTo(Path.of("build/app-runner"));
        assertThat(read.getGraalVMInfo()).containsEntry("java-version", "21");
    }
}
