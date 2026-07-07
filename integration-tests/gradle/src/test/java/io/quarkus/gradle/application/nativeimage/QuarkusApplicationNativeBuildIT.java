package io.quarkus.gradle.application.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;
import io.quarkus.gradle.application.QuarkusApplicationGradleTestBase;

public class QuarkusApplicationNativeBuildIT extends QuarkusApplicationGradleTestBase {

    private static final List<String> NATIVE_PROPERTIES = List.of(
            "quarkus.native.container-build",
            "quarkus.native.builder-image");
    private static final String BUILD_NAME = "parity";
    private static final String OUTPUT_NAME = "application-native-custom-2.0";
    private static final String EXECUTABLE_STEM = OUTPUT_NAME + "-binary";

    @Test
    void namedNativeExecutableUsesConfiguredArchiveShapeAndRunsNamedNativeTest() throws Exception {
        File projectDir = getProjectDir("application-plugin/native");

        BuildResult result = runNativeTest(projectDir);

        assertThat(result.unsuccessfulTasks()).isEmpty();
        assertThat(result.getTasks()).containsEntry(":quarkusParityBuild", BuildResult.SUCCESS_OUTCOME);
        assertThat(result.getTasks()).containsEntry(":quarkusParityNativeTest", BuildResult.SUCCESS_OUTCOME);
        assertThat(result.getOutput())
                .contains("Initializing...")
                .contains("Performing analysis...")
                .contains("Finished generating '" + EXECUTABLE_STEM)
                .containsAnyOf("Configuration cache entry stored.", "Configuration cache entry reused.");

        Path project = projectDir.toPath().toAbsolutePath().normalize();
        Path expectedOutputRoot = project.resolve("build/quarkus-builds/parity/package");
        Path receipt = project.resolve("build/quarkus-build-results/parity/package/native-result.properties");
        Properties nativeResult = readProperties(receipt);
        assertThat(nativeResult)
                .containsEntry("schema.version", "1")
                .containsEntry("result.type", "native-executable")
                .containsEntry("build.name", BUILD_NAME)
                .containsEntry("native.output-name", OUTPUT_NAME);

        Path outputRoot = resolve(receipt.getParent(), nativeResult.getProperty("native.output-root"));
        Path executable = resolve(outputRoot, nativeResult.getProperty("native.executable.path"));
        assertThat(outputRoot).isEqualTo(expectedOutputRoot);
        assertThat(executable.getFileName().toString()).isIn(EXECUTABLE_STEM, EXECUTABLE_STEM + ".exe");
        assertThat(executable).isRegularFile();
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            assertThat(Files.isExecutable(executable)).as("native executable permission").isTrue();
        }

        Path testReport = project
                .resolve("build/test-results/quarkusParityNativeTest/TEST-org.acme.GreetingResourceIT.xml");
        assertThat(testReport).isRegularFile();
        assertThat(Files.readString(testReport))
                .contains("tests=\"1\"")
                .contains("failures=\"0\"")
                .contains("errors=\"0\"");
    }

    private BuildResult runNativeTest(File projectDir) throws IOException, InterruptedException {
        List<String> arguments = new ArrayList<>(List.of("clean", "quarkusParityNativeTest"));
        for (String property : NATIVE_PROPERTIES) {
            String value = System.getProperty(property);
            if (value != null && !value.equals("${" + property + "}")) {
                arguments.add("-D" + property + "=" + value);
            }
        }
        return runApplicationGradleWrapper(projectDir, arguments.toArray(String[]::new));
    }

    private static Properties readProperties(Path file) throws IOException {
        assertThat(file).isRegularFile();
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        return properties;
    }

    private static Path resolve(Path base, String value) {
        assertThat(value).isNotBlank();
        Path path = Path.of(value);
        return (path.isAbsolute() ? path : base.resolve(path)).normalize();
    }
}
