package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.gradle.BuildResult;

@DisabledOnOs(OS.WINDOWS)
class QuarkusApplicationStartupArchiveHostTest extends QuarkusApplicationGradleTestBase {

    private static final String HOTSPOT_JAVA_HOME = "QUARKUS_GRADLE_HOTSPOT_JAVA_HOME";
    private static final String SEMERU_JAVA_HOME = "QUARKUS_GRADLE_SEMERU_JAVA_HOME";
    private static final String RESPONSE = "hello from startup-archive training";

    @Test
    void hotspotJava25ProducesAndConsumesAotCache() throws Exception {
        JavaInstallation java = javaInstallation(HOTSPOT_JAVA_HOME, RuntimeFamily.HOTSPOT);
        File projectDirectory = getProjectDir("application-plugin/startup-archive-host");

        BuildResult result = runApplicationGradleWrapper(projectDirectory,
                "clean",
                "quarkusHotspotStartupArchiveValidation",
                "-PstartupArchiveJavaHome=" + java.home());

        assertThat(result.unsuccessfulTasks()).isEmpty();
        Path buildDirectory = projectDirectory.toPath().resolve("build");
        Path archive = buildDirectory.resolve(
                "quarkus-builds/hotspot/startup-archive-training/HotspotTraining/app.aot");
        assertThat(archive).isRegularFile();
        assertThat(Files.size(archive)).isPositive();
        assertThat(buildDirectory.resolve(
                "quarkus-builds/semeru/startup-archive-training/SemeruTraining/app-scc"))
                .doesNotExist();

        Path jar = buildDirectory.resolve("quarkus-builds/hotspot/package/quarkus-run.jar");
        assertJarApplication(java.executable(), jar, List.of("-XX:AOTCache=" + archive),
                buildDirectory.resolve("hotspot-aot-consumption.log"), Map.of(), RESPONSE);
    }

    @Test
    void semeruJava25ProducesAndConsumesScc() throws Exception {
        JavaInstallation java = javaInstallation(SEMERU_JAVA_HOME, RuntimeFamily.SEMERU);
        File projectDirectory = getProjectDir("application-plugin/startup-archive-host");

        BuildResult result = runApplicationGradleWrapper(projectDirectory,
                "clean",
                "quarkusSemeruStartupArchiveValidation",
                "-PstartupArchiveJavaHome=" + java.home());

        assertThat(result.unsuccessfulTasks()).isEmpty();
        Path buildDirectory = projectDirectory.toPath().resolve("build");
        Path archive = buildDirectory.resolve(
                "quarkus-builds/semeru/startup-archive-training/SemeruTraining/app-scc");
        assertThat(archive).isDirectory();
        try (var entries = Files.list(archive)) {
            assertThat(entries).isNotEmpty();
        }
        assertThat(buildDirectory.resolve(
                "quarkus-builds/hotspot/startup-archive-training/HotspotTraining/app.aot"))
                .doesNotExist();

        Path jar = buildDirectory.resolve("quarkus-builds/semeru/package/quarkus-run.jar");
        assertJarApplication(java.executable(), jar,
                List.of("-Xshareclasses:name=quarkus-app,cacheDir=" + archive + ",readonly"),
                buildDirectory.resolve("semeru-scc-consumption.log"), Map.of(), RESPONSE);
    }

    private static JavaInstallation javaInstallation(String dedicatedEnvironmentVariable, RuntimeFamily expected)
            throws Exception {
        String configuredHome = System.getenv(dedicatedEnvironmentVariable);
        String source = dedicatedEnvironmentVariable;
        if (configuredHome == null || configuredHome.isBlank()) {
            configuredHome = System.getenv("JAVA_HOME");
            source = "JAVA_HOME";
        }
        String configuredSource = source;
        assumeTrue(configuredHome != null && !configuredHome.isBlank(),
                () -> "Set " + dedicatedEnvironmentVariable
                        + " (or run the test with a matching JAVA_HOME) to a Java 25 " + expected.displayName());

        Path home = Path.of(configuredHome).toAbsolutePath().normalize();
        Path executable = home.resolve("bin/java");
        assumeTrue(Files.isExecutable(executable),
                () -> configuredSource + " does not contain an executable bin/java: " + home);
        CommandResult version = runCommand(executable.toString(), "-version");
        assumeTrue(version.exitCode() == 0,
                () -> executable + " -version failed:\n" + version.output());
        String normalized = version.output().toLowerCase(Locale.ROOT);
        assumeTrue(normalized.contains("version \"25") || normalized.contains("openjdk 25"),
                () -> executable + " is not Java 25:\n" + version.output());
        boolean semeru = normalized.contains("semeru") || normalized.contains("openj9");
        assumeTrue(expected == RuntimeFamily.SEMERU ? semeru : !semeru,
                () -> executable + " is not a " + expected.displayName() + " runtime:\n" + version.output());
        return new JavaInstallation(home, executable);
    }

    private static CommandResult runCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        boolean exited = process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS);
        if (!exited) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            return new CommandResult(-1, "Command timed out: " + String.join(" ", command));
        }
        return new CommandResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private enum RuntimeFamily {
        HOTSPOT("HotSpot/OpenJDK"),
        SEMERU("Semeru/OpenJ9");

        private final String displayName;

        RuntimeFamily(String displayName) {
            this.displayName = displayName;
        }

        private String displayName() {
            return displayName;
        }
    }

    private record JavaInstallation(Path home, Path executable) {
    }

    private record CommandResult(int exitCode, String output) {
    }
}
