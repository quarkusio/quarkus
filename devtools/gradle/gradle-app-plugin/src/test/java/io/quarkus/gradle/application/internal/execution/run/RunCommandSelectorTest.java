package io.quarkus.gradle.application.internal.execution.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

class RunCommandSelectorTest {

    private final RunCommandSelector selector = new RunCommandSelector();

    @Test
    void selectsExplicitRunTarget() {
        RunCommand java = command("java", "java", "-jar", "app.jar");
        RunCommand nativeImageAgent = command("native-image-agent", "java", "-agentlib:native-image-agent", "-jar",
                "app.jar");

        assertThat(selector.select(Map.of(
                "java", java,
                "native-image-agent", nativeImageAgent), Optional.of("java")))
                .isSameAs(java);
    }

    @Test
    void selectsNonJavaTargetWhenJavaAndOneSpecializedTargetExist() {
        RunCommand nativeImageAgent = command("native-image-agent", "java", "-agentlib:native-image-agent", "-jar",
                "app.jar");

        assertThat(selector.select(Map.of(
                "java", command("java", "java", "-jar", "app.jar"),
                "native-image-agent", nativeImageAgent), Optional.empty()))
                .isSameAs(nativeImageAgent);
    }

    @Test
    void rejectsMissingExplicitTarget() {
        assertThatThrownBy(() -> selector.select(Map.of("java", command("java", "java", "-jar", "app.jar")),
                Optional.of("missing")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("missing")
                .hasMessageContaining("java");
    }

    @Test
    void rejectsAmbiguousTargets() {
        assertThatThrownBy(() -> selector.select(Map.of(
                "java", command("java", "java", "-jar", "app.jar"),
                "one", command("one", "one"),
                "two", command("two", "two")), Optional.empty()))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Multiple Quarkus run targets")
                .hasMessageContaining("java, one, two");
    }

    @Test
    void insertsJvmArgumentsBeforeJarAndAppendsApplicationArguments() {
        RunCommand command = selector.withArguments(
                command("java", "java", "-jar", "app.jar"),
                List.of("-Xmx128m", "-Dexample=true"),
                List.of("--profile=test"));

        assertThat(command.arguments())
                .containsExactly("java", "-Xmx128m", "-Dexample=true", "-jar", "app.jar", "--profile=test");
    }

    @Test
    void appendsApplicationArgumentsToNonJavaCommands() {
        RunCommand command = selector.withArguments(
                command("native", "app"),
                List.of("-Xmx128m"),
                List.of("--profile=test"));

        assertThat(command.arguments()).containsExactly("app", "--profile=test");
    }

    private static RunCommand command(String name, String... arguments) {
        return new RunCommand(name, List.of(arguments), Optional.empty(), Optional.empty(), false, Optional.empty());
    }
}
