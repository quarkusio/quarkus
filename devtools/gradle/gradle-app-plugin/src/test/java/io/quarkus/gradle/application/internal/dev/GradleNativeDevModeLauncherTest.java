package io.quarkus.gradle.application.internal.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import io.quarkus.deployment.dev.DevModeCommandLineBuilder;
import io.quarkus.deployment.dev.ExtensionDevModeJvmOptionFilter;
import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode;

class GradleNativeDevModeLauncherTest {

    @TempDir
    Path testDirectory;

    @Test
    void excludesPhantomOptionalClassOutputRoots() throws Exception {
        Path classes = Files.createDirectories(testDirectory.resolve("classes"));
        Path missingGeneratedClasses = testDirectory.resolve("generated/ksp/test/classes");

        assertThat(GradleNativeDevModeLauncher.existingPaths(
                List.of(classes.toFile(), missingGeneratedClasses.toFile())))
                .containsExactly(classes);
    }

    @Test
    void translatesDebugOptionsToTheCoreLauncherContract() {
        assertThat(GradleNativeDevModeLauncher.debugValue(null, null)).isNull();
        assertThat(GradleNativeDevModeLauncher.debugValue(true, null)).isEqualTo("true");
        assertThat(GradleNativeDevModeLauncher.debugValue(false, null)).isEqualTo("false");
        assertThat(GradleNativeDevModeLauncher.debugValue(null, QuarkusApplicationDevDebugMode.LISTEN))
                .isEqualTo("true");
        assertThat(GradleNativeDevModeLauncher.debugValue(null, QuarkusApplicationDevDebugMode.CONNECT))
                .isEqualTo("client");
        assertThat(GradleNativeDevModeLauncher.debugValue(true, QuarkusApplicationDevDebugMode.CONNECT))
                .isEqualTo("client");
        assertThat(GradleNativeDevModeLauncher.debugValue(false, QuarkusApplicationDevDebugMode.LISTEN))
                .isEqualTo("false");
        assertThat(GradleNativeDevModeLauncher.debugValue(false, QuarkusApplicationDevDebugMode.CONNECT))
                .isEqualTo("false");
    }

    @Test
    void appliesNullableLaunchOptionsWithoutInventingDefaults() {
        DevModeCommandLineBuilder builder = mock(DevModeCommandLineBuilder.class, RETURNS_SELF);

        GradleNativeDevModeLauncher.applyLaunchOptions(builder, parameters(
                Map.of(), null, null, null, null, null, null, false, List.of()));

        verify(builder).forceC2((Boolean) null);
        verify(builder).debug((String) null);
        verify(builder).debugHost(null);
        verify(builder).debugPort(null);
        verify(builder).suspend(null);
        ArgumentCaptor<ExtensionDevModeJvmOptionFilter> filterCaptor = ArgumentCaptor
                .forClass(ExtensionDevModeJvmOptionFilter.class);
        verify(builder).extensionDevModeJvmOptionFilter(filterCaptor.capture());
        assertThat(filterCaptor.getValue().isDisableAll()).isFalse();
        assertThat(filterCaptor.getValue().getDisableFor()).isEmpty();
    }

    @Test
    void appliesConfiguredLaunchOptionsAndCreatesAFreshExtensionFilter() {
        DevModeCommandLineBuilder builder = mock(DevModeCommandLineBuilder.class, RETURNS_SELF);
        GradleNativeDevModeLauncher.Parameters parameters = parameters(
                Map.of(), true, QuarkusApplicationDevDebugMode.CONNECT, "debug.example.test", 7007, true, false,
                true, List.of("org.acme:first", "org.acme:second"));

        GradleNativeDevModeLauncher.applyLaunchOptions(builder, parameters);
        GradleNativeDevModeLauncher.applyLaunchOptions(builder, parameters);

        verify(builder, times(2)).forceC2(false);
        verify(builder, times(2)).debug("client");
        verify(builder, times(2)).debugHost("debug.example.test");
        verify(builder, times(2)).debugPort("7007");
        verify(builder, times(2)).suspend("true");
        ArgumentCaptor<ExtensionDevModeJvmOptionFilter> filterCaptor = ArgumentCaptor
                .forClass(ExtensionDevModeJvmOptionFilter.class);
        verify(builder, times(2)).extensionDevModeJvmOptionFilter(filterCaptor.capture());
        assertThat(filterCaptor.getAllValues())
                .hasSize(2)
                .allSatisfy(filter -> {
                    assertThat(filter.isDisableAll()).isTrue();
                    assertThat(filter.getDisableFor()).containsExactly("org.acme:first", "org.acme:second");
                });
        assertThat(filterCaptor.getAllValues().get(0)).isNotSameAs(filterCaptor.getAllValues().get(1));
    }

    @Test
    void appliesForceC2AndSelectiveExtensionFilters() {
        DevModeCommandLineBuilder builder = mock(DevModeCommandLineBuilder.class, RETURNS_SELF);

        GradleNativeDevModeLauncher.applyLaunchOptions(builder, parameters(
                Map.of(), null, null, null, null, null, true, false, List.of("org.acme:selected")));

        verify(builder).forceC2(true);
        ArgumentCaptor<ExtensionDevModeJvmOptionFilter> filterCaptor = ArgumentCaptor
                .forClass(ExtensionDevModeJvmOptionFilter.class);
        verify(builder).extensionDevModeJvmOptionFilter(filterCaptor.capture());
        assertThat(filterCaptor.getValue().isDisableAll()).isFalse();
        assertThat(filterCaptor.getValue().getDisableFor()).containsExactly("org.acme:selected");
    }

    @Test
    void commandLineBuilderUsesTheSelectedJavaExecutable() throws Exception {
        Path selectedJava = testDirectory.resolve("selected-java");
        Path buildDirectory = Files.createDirectories(testDirectory.resolve("command-line-build"));

        var commandLine = GradleNativeDevModeLauncher.commandLineBuilder(selectedJava)
                .projectDir(testDirectory.toFile())
                .buildDir(buildDirectory.toFile())
                .outputDir(buildDirectory.toFile())
                .applicationName("test-application")
                .applicationVersion("1.0")
                .build();

        assertThat(commandLine.getArguments()).first().isEqualTo(selectedJava.toString());
    }

    @Test
    void buildsProcessWithSelectedWorkingDirectoryAndMergedEnvironment() throws Exception {
        Path workingDirectory = Files.createDirectories(testDirectory.resolve("working"));
        String environmentName = "QUARKUS_GRADLE_LAUNCHER_TEST";
        GradleNativeDevModeLauncher.Parameters parameters = parameters(
                workingDirectory, Map.of(environmentName, "configured"));

        ProcessBuilder processBuilder = GradleNativeDevModeLauncher.processBuilder(parameters,
                List.of("java", "-version"));

        assertThat(processBuilder.directory()).isEqualTo(workingDirectory.toFile());
        assertThat(processBuilder.command()).containsExactly("java", "-version");
        assertThat(processBuilder.environment()).containsEntry(environmentName, "configured");
        System.getenv().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(environmentName))
                .findFirst()
                .ifPresent(entry -> assertThat(processBuilder.environment())
                        .containsEntry(entry.getKey(), entry.getValue()));
    }

    @Test
    void parametersDefensivelyCopyMutableLaunchCollections() {
        Map<String, String> environment = new HashMap<>(Map.of("INITIAL", "value"));
        List<String> disabledExtensions = new ArrayList<>(List.of("org.acme:initial"));

        GradleNativeDevModeLauncher.Parameters parameters = parameters(
                environment, null, null, null, null, null, null, false, disabledExtensions);
        environment.put("LATER", "mutation");
        disabledExtensions.add("org.acme:later");

        assertThat(parameters.environmentVariables()).containsExactlyEntriesOf(Map.of("INITIAL", "value"));
        assertThat(parameters.disableExtensionJvmOptionsFor()).containsExactly("org.acme:initial");
        assertThatThrownBy(() -> parameters.environmentVariables().put("INVALID", "mutation"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> parameters.disableExtensionJvmOptionsFor().add("org.acme:invalid"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private GradleNativeDevModeLauncher.Parameters parameters(Path workingDirectory,
            Map<String, String> environmentVariables) {
        return parameters(workingDirectory, environmentVariables, null, null, null, null, null, null, false, List.of());
    }

    private GradleNativeDevModeLauncher.Parameters parameters(
            Map<String, String> environmentVariables,
            Boolean debug,
            QuarkusApplicationDevDebugMode debugMode,
            String debugHost,
            Integer debugPort,
            Boolean suspend,
            Boolean forceC2,
            boolean disableAllExtensionJvmOptions,
            List<String> disableExtensionJvmOptionsFor) {
        return parameters(testDirectory.resolve("working"), environmentVariables, debug, debugMode, debugHost,
                debugPort, suspend, forceC2, disableAllExtensionJvmOptions, disableExtensionJvmOptionsFor);
    }

    private GradleNativeDevModeLauncher.Parameters parameters(
            Path workingDirectory,
            Map<String, String> environmentVariables,
            Boolean debug,
            QuarkusApplicationDevDebugMode debugMode,
            String debugHost,
            Integer debugPort,
            Boolean suspend,
            Boolean forceC2,
            boolean disableAllExtensionJvmOptions,
            List<String> disableExtensionJvmOptionsFor) {
        Path projectDirectory = testDirectory.resolve("project");
        return new GradleNativeDevModeLauncher.Parameters(
                Path.of(System.getProperty("java.home"), "bin", "java"),
                projectDirectory.resolve("application-model.dat"),
                null,
                false,
                true,
                List.<File> of(),
                List.<File> of(),
                List.<File> of(),
                List.<File> of(),
                projectDirectory,
                projectDirectory.resolve("build"),
                workingDirectory,
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
                environmentVariables,
                debug,
                debugMode,
                debugHost,
                debugPort,
                suspend,
                forceC2,
                disableAllExtensionJvmOptions,
                disableExtensionJvmOptionsFor);
    }
}
