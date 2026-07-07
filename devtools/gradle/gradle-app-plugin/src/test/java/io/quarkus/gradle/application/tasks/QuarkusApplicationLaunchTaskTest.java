package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.work.InputChanges;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.model.QuarkusApplicationLaunchKind;

class QuarkusApplicationLaunchTaskTest {

    private static final String SENSITIVE_VALUE = "must-not-appear-in-error";

    @Test
    void continuousTestTaskUsesTheSharedContinuousSessionTaskContract() {
        Project project = ProjectBuilder.builder().build();
        QuarkusApplicationContinuousTestTask task = project.getTasks().register("quarkusAppContinuousTest",
                QuarkusApplicationContinuousTestTask.class).get();

        assertThat(task).isInstanceOf(QuarkusApplicationDevTask.class);
        assertThat(task.getContinuousTesting().get()).isFalse();
    }

    @Test
    void continuousTestTaskRequiresGradleContinuousBuild() {
        Project project = ProjectBuilder.builder().build();
        QuarkusApplicationContinuousTestTask task = project.getTasks().register("quarkusApplicationContinuousTest",
                QuarkusApplicationContinuousTestTask.class).get();
        task.getContinuousTesting().set(true);

        assertThatThrownBy(() -> task.executeDevIteration(mock(InputChanges.class)))
                .hasMessageContaining("requires Gradle continuous build")
                .hasMessageContaining("--continuous");
    }

    @Test
    void continuousTestingRejectsLegacyTestOwnership() {
        Project project = ProjectBuilder.builder().build();
        QuarkusApplicationContinuousTestTask task = project.getTasks().register("quarkusApplicationContinuousTest",
                QuarkusApplicationContinuousTestTask.class).get();
        task.getContinuousBuild().set(true);
        task.getContinuousTesting().set(true);
        task.getLegacyTestsOwned().set(true);

        assertThatThrownBy(() -> task.executeDevIteration(mock(InputChanges.class)))
                .hasMessageContaining("legacy plugin 'io.quarkus' owns Quarkus test execution")
                .hasMessageContaining("Use legacy 'quarkusTest'");
    }

    @Test
    void dedicatedContinuousTestTaskCannotDisableContinuousTesting() {
        Project project = ProjectBuilder.builder().build();
        QuarkusApplicationContinuousTestTask task = project.getTasks().register("quarkusApplicationContinuousTest",
                QuarkusApplicationContinuousTestTask.class).get();
        task.getLaunchKind().set(QuarkusApplicationLaunchKind.CONTINUOUS_TEST);
        task.getContinuousBuild().set(true);
        task.getContinuousTesting().set(false);

        assertThatThrownBy(() -> task.executeDevIteration(mock(InputChanges.class)))
                .hasMessageContaining("always runs continuous testing")
                .hasMessageContaining("cannot be used with --no-continuous-testing");
    }

    @Test
    void commandLineEnvironmentMergesOverDslByKey() {
        Map<String, String> effective = QuarkusApplicationDevTask.mergeEnvironmentVariables(
                Map.of("DSL_ONLY", "dsl", "OVERRIDDEN", "dsl-value"),
                List.of(
                        "OVERRIDDEN=first",
                        "EMPTY=",
                        "WITH_EQUALS=left=right",
                        "OVERRIDDEN=last"));

        assertThat(effective)
                .containsEntry("DSL_ONLY", "dsl")
                .containsEntry("OVERRIDDEN", "last")
                .containsEntry("EMPTY", "")
                .containsEntry("WITH_EQUALS", "left=right");
    }

    @Test
    void rejectsMalformedCommandLineEnvironmentWithoutExposingValues() {
        assertInvalidEnvironment(Map.of(), List.of(SENSITIVE_VALUE), "must use NAME=VALUE syntax");
        assertInvalidEnvironment(Map.of(), List.of("=" + SENSITIVE_VALUE), "empty or blank name");
        assertInvalidEnvironment(Map.of(), List.of("  =" + SENSITIVE_VALUE), "empty or blank name");
        assertInvalidEnvironment(Map.of(), List.of("NAME=value\u0000" + SENSITIVE_VALUE), "value must not contain a NUL");
        assertInvalidEnvironment(Map.of(), List.of("NAME\u0000=" + SENSITIVE_VALUE), "name must not contain a NUL");
        assertInvalidEnvironment(Map.of(), Arrays.asList((String) null), "must not be null");
    }

    @Test
    void rejectsMalformedDslEnvironmentWithoutExposingValues() {
        assertInvalidEnvironment(environmentWith(null, SENSITIVE_VALUE), List.of(), "empty or blank name");
        assertInvalidEnvironment(environmentWith("", SENSITIVE_VALUE), List.of(), "empty or blank name");
        assertInvalidEnvironment(environmentWith("  ", SENSITIVE_VALUE), List.of(), "empty or blank name");
        assertInvalidEnvironment(environmentWith("BAD=NAME", SENSITIVE_VALUE), List.of(), "name must not contain '='");
        assertInvalidEnvironment(environmentWith("BAD\u0000NAME", SENSITIVE_VALUE), List.of(),
                "name must not contain a NUL");
        assertInvalidEnvironment(environmentWith("NAME", null), List.of(), "value must not be null");
        assertInvalidEnvironment(environmentWith("NAME", "value\u0000" + SENSITIVE_VALUE), List.of(),
                "value must not contain a NUL");
    }

    @Test
    void validatesWorkingDirectory(@TempDir Path directory) throws IOException {
        Path regularFile = Files.createFile(directory.resolve("regular-file"));

        assertThatCode(() -> QuarkusApplicationDevTask.validateWorkingDirectory(":dev", directory))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> QuarkusApplicationDevTask.validateWorkingDirectory(":dev", regularFile))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("does not exist or is not a directory")
                .hasMessageContaining("--working-directory");
        assertThatThrownBy(() -> QuarkusApplicationDevTask.validateWorkingDirectory(":dev", directory.resolve("missing")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("does not exist or is not a directory");
    }

    @Test
    void validatesDebugOptions() {
        assertThatCode(() -> QuarkusApplicationDevTask.validateDebugHost(":dev", null))
                .doesNotThrowAnyException();
        assertThatCode(() -> QuarkusApplicationDevTask.validateDebugPort(":dev", -1))
                .doesNotThrowAnyException();
        assertThatCode(() -> QuarkusApplicationDevTask.validateDebugPort(":dev", 65535))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> QuarkusApplicationDevTask.validateDebugHost(":dev", " "))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("debug host must not be blank");
        assertThatThrownBy(() -> QuarkusApplicationDevTask.validateDebugPort(":dev", 65536))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("debug port must be at most 65535");
    }

    @Test
    void validatesExtensionJvmOptionPatterns() {
        assertThatCode(() -> QuarkusApplicationDevTask.validateExtensionJvmOptionPatterns(
                ":dev", List.of("org.acme:acme-extension", "org.acme:*:*")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> QuarkusApplicationDevTask.validateExtensionJvmOptionPatterns(
                ":dev", List.of("org.acme:artifact:classifier:type")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("invalid artifact pattern")
                .hasMessageContaining("extensionJvmOptions.disableFor");
    }

    private static Map<String, String> environmentWith(String name, String value) {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put(name, value);
        return environment;
    }

    private static void assertInvalidEnvironment(Map<String, String> configured, List<String> commandLineEntries,
            String message) {
        assertThatThrownBy(() -> QuarkusApplicationDevTask.mergeEnvironmentVariables(configured, commandLineEntries))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining(message)
                .hasMessageNotContaining(SENSITIVE_VALUE);
    }
}
