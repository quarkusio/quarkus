package io.quarkus.gradle.application.internal.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class NativeResultCodecTest {

    private final NativeResultCodec codec = new NativeResultCodec();

    @TempDir
    Path directory;

    @Test
    void writesDeterministicNativeExecutableReceipt() throws Exception {
        Path outputRoot = directory.resolve("build/quarkus-builds/native1/package");
        Path file = directory.resolve("build/quarkus-build-results/native1/package/native-result.properties");
        var result = new NativeResult(
                "native1",
                QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                outputRoot,
                "my-native",
                Optional.of(outputRoot.resolve("my-native-runner")),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of("java-version", "21"),
                List.of(new NativeResult.Artifact(
                        Optional.of(outputRoot.resolve("my-native-runner")),
                        "native",
                        Map.of("graalvm", "metadata"))));

        codec.write(file, result);

        assertThat(java.nio.file.Files.readString(file)).isEqualTo("""
                build.name=native1
                native.artifact.0.metadata.graalvm=metadata
                native.artifact.0.path=my-native-runner
                native.artifact.0.type=native
                native.artifact.count=1
                native.executable.path=my-native-runner
                native.graalvm.java-version=21
                native.output-name=my-native
                native.output-root=../../../quarkus-builds/native1/package
                result.type=native-executable
                schema.version=1
                """);

        NativeResult read = codec.read(file);
        assertThat(read.buildType()).isEqualTo(QuarkusApplicationBuildType.NATIVE_EXECUTABLE);
        assertThat(read.executablePath()).contains(outputRoot.resolve("my-native-runner"));
        assertThat(read.graalVMInfo()).containsEntry("java-version", "21");
    }

    @Test
    void keepsNativeSourcesDirectorySeparateFromSourceJarPath() throws Exception {
        Path outputRoot = directory.resolve("build/quarkus-builds/sources1/package");
        Path file = directory.resolve("build/quarkus-build-results/sources1/package/native-result.properties");
        var result = new NativeResult(
                "sources1",
                QuarkusApplicationBuildType.NATIVE_SOURCES,
                outputRoot,
                "my-native",
                Optional.empty(),
                Optional.of(outputRoot.resolve("native-sources")),
                Optional.of(outputRoot.resolve("native-sources-source-jar/app.jar")),
                Optional.of(outputRoot.resolve("native-sources/native-image.args")),
                Map.of(),
                List.of());

        codec.write(file, result);

        NativeResult read = codec.read(file);
        assertThat(read.buildType()).isEqualTo(QuarkusApplicationBuildType.NATIVE_SOURCES);
        assertThat(read.sourcesDirectory()).contains(outputRoot.resolve("native-sources"));
        assertThat(read.sourceJarPath()).contains(outputRoot.resolve("native-sources-source-jar/app.jar"));
        assertThat(read.nativeImageArgsPath()).contains(outputRoot.resolve("native-sources/native-image.args"));
    }

    @Test
    void rejectsMalformedReceipt() throws Exception {
        Path file = directory.resolve("native-result.properties");
        java.nio.file.Files.writeString(file, """
                schema.version=1
                result.type=native-executable
                """);

        assertThatThrownBy(() -> codec.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("native.output-root")
                .hasMessageContaining(file.toString());
    }
}
