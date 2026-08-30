package io.quarkus.gradle.application.internal.packaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class PackageResultCodecTest {

    private final PackageResultCodec codec = new PackageResultCodec();

    @TempDir
    Path directory;

    @Test
    void writesDeterministicPackageReceipt() throws Exception {
        Path outputRoot = directory.resolve("build/quarkus-builds/app/package");
        Path file = directory.resolve("build/quarkus-build-results/app/package/package-result.properties");
        var result = new PackageResult(
                "app",
                QuarkusApplicationBuildType.FAST_JAR,
                outputRoot,
                "app",
                outputRoot.resolve("quarkus-run.jar"),
                Optional.of(outputRoot.resolve("app/app.jar")),
                Optional.of(outputRoot.resolve("lib")),
                false,
                false,
                Optional.of("runner"),
                List.of(new PackageResult.Artifact(
                        Optional.of(outputRoot.resolve("quarkus-run.jar")),
                        "jar",
                        Map.of("library-dir", "lib"))));

        codec.write(file, result);

        assertThat(java.nio.file.Files.readString(file)).isEqualTo("""
                build.name=app
                package.artifact.0.metadata.library-dir=lib
                package.artifact.0.path=quarkus-run.jar
                package.artifact.0.type=jar
                package.artifact.count=1
                package.classifier=runner
                package.jar.path=quarkus-run.jar
                package.library-dir=lib
                package.mutable=false
                package.original-artifact=app/app.jar
                package.output-name=app
                package.output-root=../../../quarkus-builds/app/package
                package.type=fast-jar
                package.uber=false
                result.type=jvm-package
                schema.version=1
                """);

        PackageResult read = codec.read(file);
        assertThat(read.buildName()).isEqualTo("app");
        assertThat(read.buildType()).isEqualTo(QuarkusApplicationBuildType.FAST_JAR);
        assertThat(read.outputRoot()).isEqualTo(outputRoot);
        assertThat(read.jarPath()).isEqualTo(outputRoot.resolve("quarkus-run.jar"));
        assertThat(read.libraryDirectory()).contains(outputRoot.resolve("lib"));
        assertThat(read.originalArtifact()).contains(outputRoot.resolve("app/app.jar"));
        assertThat(read.classifier()).contains("runner");
        assertThat(read.artifacts()).singleElement().satisfies(artifact -> {
            assertThat(artifact.type()).isEqualTo("jar");
            assertThat(artifact.path()).contains(outputRoot.resolve("quarkus-run.jar"));
            assertThat(artifact.metadata()).containsEntry("library-dir", "lib");
        });
    }

    @Test
    void rejectsMalformedReceipt() throws Exception {
        Path file = directory.resolve("package-result.properties");
        java.nio.file.Files.writeString(file, """
                schema.version=1
                result.type=jvm-package
                """);

        assertThatThrownBy(() -> codec.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("package.output-root")
                .hasMessageContaining(file.toString());
    }
}
