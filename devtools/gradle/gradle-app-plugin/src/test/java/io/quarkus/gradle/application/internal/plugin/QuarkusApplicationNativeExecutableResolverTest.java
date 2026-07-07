package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.nativeimage.NativeResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class QuarkusApplicationNativeExecutableResolverTest {

    private static final String SUITE_NAME = "quarkusParityNativeTest";
    private static final String BUILD_NAME = "parity";

    @TempDir
    Path directory;

    @Test
    void resolvesRelativeExecutableFromNativeReceipt() throws Exception {
        Path outputRoot = directory.resolve("build/quarkus-builds/parity/package");
        Path executable = createFile(outputRoot.resolve("application-runner"));
        Path receipt = receiptPath();
        writeReceipt(receipt, BUILD_NAME, QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                outputRoot, Optional.of(executable));

        assertThat(QuarkusApplicationNativeExecutableResolver.resolve(receipt, SUITE_NAME, BUILD_NAME))
                .isEqualTo(executable.toAbsolutePath().normalize());
        assertThat(Files.readString(receipt))
                .contains("native.executable.path=application-runner");
    }

    @Test
    void resolvesAbsoluteExecutableFromNativeReceipt() throws Exception {
        Path outputRoot = Files.createDirectories(directory.resolve("absolute-output")).toAbsolutePath();
        Path executable = createFile(outputRoot.resolve("application-runner")).toAbsolutePath();
        Path receipt = receiptPath();
        writeFile(receipt, """
                schema.version=1
                result.type=native-executable
                build.name=parity
                native.output-root=%s
                native.output-name=application
                native.executable.path=%s
                native.artifact.count=0
                """.formatted(portablePath(outputRoot), portablePath(executable)));

        assertThat(QuarkusApplicationNativeExecutableResolver.resolve(receipt, SUITE_NAME, BUILD_NAME))
                .isEqualTo(executable.normalize());
    }

    @Test
    void reportsMissingAndMalformedReceiptsWithSuiteAndBuild() throws Exception {
        Path missing = directory.resolve("missing.properties");
        assertDiagnostic(() -> QuarkusApplicationNativeExecutableResolver.resolve(missing, SUITE_NAME, BUILD_NAME))
                .hasMessageContaining("could not read the native result")
                .hasRootCauseInstanceOf(java.nio.file.NoSuchFileException.class);

        Path malformed = receiptPath();
        writeFile(malformed, """
                schema.version=unsupported
                result.type=native-executable
                """);
        assertDiagnostic(() -> QuarkusApplicationNativeExecutableResolver.resolve(malformed, SUITE_NAME, BUILD_NAME))
                .hasMessageContaining("could not read the native result")
                .hasRootCauseMessage("Unsupported schema version in " + malformed + ": unsupported");
    }

    @Test
    void rejectsReceiptForAnotherBuild() throws Exception {
        Path outputRoot = directory.resolve("build/quarkus-builds/other/package");
        Path executable = createFile(outputRoot.resolve("application-runner"));
        Path receipt = receiptPath();
        writeReceipt(receipt, "other", QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                outputRoot, Optional.of(executable));

        assertDiagnostic(() -> QuarkusApplicationNativeExecutableResolver.resolve(receipt, SUITE_NAME, BUILD_NAME))
                .hasMessageContaining("belongs to build 'other'");
    }

    @Test
    void rejectsNativeSourcesReceipt() {
        Path outputRoot = directory.resolve("build/quarkus-builds/parity/package");
        Path receipt = receiptPath();
        writeReceipt(receipt, BUILD_NAME, QuarkusApplicationBuildType.NATIVE_SOURCES,
                outputRoot, Optional.empty());

        assertDiagnostic(() -> QuarkusApplicationNativeExecutableResolver.resolve(receipt, SUITE_NAME, BUILD_NAME))
                .hasMessageContaining("requires build 'parity' to produce a native executable")
                .hasMessageContaining("NATIVE_SOURCES");
    }

    @Test
    void rejectsMissingExecutableField() {
        Path outputRoot = directory.resolve("build/quarkus-builds/parity/package");
        Path receipt = receiptPath();
        writeReceipt(receipt, BUILD_NAME, QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                outputRoot, Optional.empty());

        assertDiagnostic(() -> QuarkusApplicationNativeExecutableResolver.resolve(receipt, SUITE_NAME, BUILD_NAME))
                .hasMessageContaining("did not produce a native executable");
    }

    @Test
    void rejectsMissingOrNonRegularExecutable() throws Exception {
        Path outputRoot = directory.resolve("build/quarkus-builds/parity/package");
        Path missingExecutable = outputRoot.resolve("missing-runner");
        Path receipt = receiptPath();
        writeReceipt(receipt, BUILD_NAME, QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                outputRoot, Optional.of(missingExecutable));

        assertDiagnostic(() -> QuarkusApplicationNativeExecutableResolver.resolve(receipt, SUITE_NAME, BUILD_NAME))
                .hasMessageContaining(missingExecutable.toAbsolutePath().normalize().toString())
                .hasMessageContaining("not a regular file");

        Path directoryExecutable = Files.createDirectories(outputRoot.resolve("directory-runner"));
        writeReceipt(receipt, BUILD_NAME, QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                outputRoot, Optional.of(directoryExecutable));
        assertDiagnostic(() -> QuarkusApplicationNativeExecutableResolver.resolve(receipt, SUITE_NAME, BUILD_NAME))
                .hasMessageContaining(directoryExecutable.toAbsolutePath().normalize().toString())
                .hasMessageContaining("not a regular file");
    }

    private org.assertj.core.api.AbstractThrowableAssert<?, ? extends Throwable> assertDiagnostic(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        return assertThatThrownBy(callable)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining(SUITE_NAME)
                .hasMessageContaining(BUILD_NAME);
    }

    private Path receiptPath() {
        return directory.resolve("build/quarkus-build-results/parity/package/native-result.properties");
    }

    private static Path createFile(Path file) throws Exception {
        Files.createDirectories(file.getParent());
        return Files.createFile(file);
    }

    private static String portablePath(Path path) {
        return path.toString().replace(File.separatorChar, '/');
    }

    private static void writeReceipt(Path receipt, String buildName, QuarkusApplicationBuildType buildType,
            Path outputRoot, Optional<Path> executable) {
        new NativeResultCodec().write(receipt, new NativeResult(
                buildName,
                buildType,
                outputRoot,
                "application",
                executable,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of()));
    }

    private static void writeFile(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
