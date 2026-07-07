package io.quarkus.gradle.application.internal.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.api.SoftAssertions;
import org.gradle.api.GradleException;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode;

class QuarkusApplicationDevDeploymentsTest {

    @TempDir
    Path testDirectory;

    @Test
    void launchConfigurationFieldsAffectTheConfigurationFingerprint() {
        String baseline = fingerprint(options -> {
        });
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(fingerprint(options -> options.javaExecutable = testDirectory.resolve("other-java")))
                .as("java executable")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.workingDirectory = testDirectory.resolve("other-working")))
                .as("working directory")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.environmentVariables = Map.of("QUARKUS_TEST", "configured")))
                .as("environment variables")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.debug = true))
                .as("debug")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.debugMode = QuarkusApplicationDevDebugMode.CONNECT))
                .as("debug mode")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.debugHost = "debug.example.test"))
                .as("debug host")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.debugPort = 7007))
                .as("debug port")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.suspend = true))
                .as("debug suspend")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.forceC2 = true))
                .as("force C2")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.disableAllExtensionJvmOptions = true))
                .as("disable all extension JVM options")
                .isNotEqualTo(baseline);
        softly.assertThat(fingerprint(options -> options.disableExtensionJvmOptionsFor = List.of("org.acme:extension")))
                .as("extension JVM option filters")
                .isNotEqualTo(baseline);

        softly.assertAll();
    }

    @Test
    void configurationFingerprintIgnoresEnvironmentAndFilterOrdering() {
        LinkedHashMap<String, String> firstEnvironment = new LinkedHashMap<>();
        firstEnvironment.put("FIRST", "one");
        firstEnvironment.put("SECOND", "two");
        LinkedHashMap<String, String> reverseEnvironment = new LinkedHashMap<>();
        reverseEnvironment.put("SECOND", "two");
        reverseEnvironment.put("FIRST", "one");

        String first = fingerprint(options -> {
            options.environmentVariables = firstEnvironment;
            options.disableExtensionJvmOptionsFor = List.of("org.acme:first", "org.acme:second");
        });
        String reverse = fingerprint(options -> {
            options.environmentVariables = reverseEnvironment;
            options.disableExtensionJvmOptionsFor = List.of("org.acme:second", "org.acme:first");
        });

        assertThat(reverse).isEqualTo(first);
    }

    @Test
    void configurationFingerprintSeparatesPathCollectionBoundaries() {
        File sharedPath = testDirectory.resolve("shared-path").toFile();

        String devModeClasspath = fingerprint(options -> options.devModeClasspath = List.of(sharedPath));
        String testSourceDirectories = fingerprint(options -> options.testSourceDirectories = List.of(sharedPath));

        assertThat(devModeClasspath).isNotEqualTo(testSourceDirectories);
    }

    @Test
    void replayTriggerPathAffectsConfigurationFingerprint() {
        GradleNativeDevModeLauncher.Parameters parameters = parameters(options -> {
        });

        assertThat(QuarkusApplicationDevDeployments.configFingerprint(parameters,
                testDirectory.resolve("first-trigger")))
                .isNotEqualTo(QuarkusApplicationDevDeployments.configFingerprint(parameters,
                        testDirectory.resolve("second-trigger")));
    }

    @Test
    void environmentFingerprintIgnoresOrderButIncludesValues() {
        LinkedHashMap<String, String> first = new LinkedHashMap<>();
        first.put("FIRST", "one");
        first.put("SECOND", "two");
        LinkedHashMap<String, String> reverse = new LinkedHashMap<>();
        reverse.put("SECOND", "two");
        reverse.put("FIRST", "one");

        String firstFingerprint = QuarkusApplicationDevDeployments.environmentFingerprint(first);

        assertThat(QuarkusApplicationDevDeployments.environmentFingerprint(reverse))
                .isEqualTo(firstFingerprint);
        assertThat(QuarkusApplicationDevDeployments.environmentFingerprint(
                Map.of("FIRST", "changed", "SECOND", "two")))
                .isNotEqualTo(firstFingerprint);
    }

    @Test
    void rejectsAnExistingSessionWithDifferentLaunchConfiguration() {
        DeploymentRegistry registry = mock(DeploymentRegistry.class);
        QuarkusApplicationDevDeploymentHandle existing = mock(QuarkusApplicationDevDeploymentHandle.class);
        when(registry.get("deployment", QuarkusApplicationDevDeploymentHandle.class)).thenReturn(existing);
        when(existing.configFingerprint()).thenReturn("existing-fingerprint");
        GradleNativeDevModeLauncher.Parameters launchParameters = parameters(options -> options.debug = true);
        QuarkusApplicationDevDeployments.Parameters parameters = new QuarkusApplicationDevDeployments.Parameters(
                "changed-fingerprint", launchParameters, testDirectory.resolve("closed"),
                testDirectory.resolve("replay-trigger"));

        assertThatThrownBy(() -> QuarkusApplicationDevDeployments.getOrStart(registry, "deployment", parameters))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("different configuration")
                .hasMessageContaining("restart this task");
    }

    @Test
    void classifiesHealthyReuseAndRecoveryOfAnExistingHandle() {
        DeploymentRegistry registry = mock(DeploymentRegistry.class);
        QuarkusApplicationDevDeploymentHandle existing = mock(QuarkusApplicationDevDeploymentHandle.class);
        when(registry.get("deployment", QuarkusApplicationDevDeploymentHandle.class)).thenReturn(existing);
        when(existing.configFingerprint()).thenReturn("fingerprint");
        QuarkusApplicationDevDeployments.Parameters parameters = new QuarkusApplicationDevDeployments.Parameters(
                "fingerprint", parameters(options -> {
                }), testDirectory.resolve("closed"), testDirectory.resolve("replay-trigger"));

        when(existing.acquire()).thenReturn(QuarkusApplicationDevDeployments.Acquisition.EXISTING_READY);
        QuarkusApplicationDevDeployments.AcquiredHandle healthy = QuarkusApplicationDevDeployments
                .getOrStart(registry, "deployment", parameters);
        assertThat(healthy.started()).isFalse();
        assertThat(healthy.restarted()).isFalse();

        when(existing.acquire()).thenReturn(QuarkusApplicationDevDeployments.Acquisition.RESTARTED_AFTER_FAILURE);
        QuarkusApplicationDevDeployments.AcquiredHandle recovered = QuarkusApplicationDevDeployments
                .getOrStart(registry, "deployment", parameters);
        assertThat(recovered.started()).isTrue();
        assertThat(recovered.restarted()).isTrue();
        verify(existing, org.mockito.Mockito.times(2)).acquire();
    }

    private String fingerprint(Consumer<ParameterOptions> configuration) {
        return QuarkusApplicationDevDeployments.configFingerprint(parameters(configuration),
                testDirectory.resolve("replay-trigger"));
    }

    private GradleNativeDevModeLauncher.Parameters parameters(Consumer<ParameterOptions> configuration) {
        ParameterOptions options = new ParameterOptions();
        configuration.accept(options);
        Path projectDirectory = testDirectory.resolve("project");
        return new GradleNativeDevModeLauncher.Parameters(
                options.javaExecutable,
                projectDirectory.resolve("application-model.dat"),
                null,
                false,
                true,
                options.devModeClasspath,
                options.testSourceDirectories,
                List.<File> of(),
                List.<File> of(),
                projectDirectory,
                projectDirectory.resolve("build"),
                options.workingDirectory,
                "test-application",
                "1.0",
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                List.of(),
                List.of(),
                false,
                Map.of(),
                options.environmentVariables,
                options.debug,
                options.debugMode,
                options.debugHost,
                options.debugPort,
                options.suspend,
                options.forceC2,
                options.disableAllExtensionJvmOptions,
                options.disableExtensionJvmOptionsFor);
    }

    private final class ParameterOptions {

        private Path javaExecutable = testDirectory.resolve("java");
        private Path workingDirectory = testDirectory.resolve("working");
        private List<File> devModeClasspath = List.of();
        private List<File> testSourceDirectories = List.of();
        private Map<String, String> environmentVariables = Map.of();
        private Boolean debug;
        private QuarkusApplicationDevDebugMode debugMode;
        private String debugHost;
        private Integer debugPort;
        private Boolean suspend;
        private Boolean forceC2;
        private boolean disableAllExtensionJvmOptions;
        private List<String> disableExtensionJvmOptionsFor = List.of();
    }
}
