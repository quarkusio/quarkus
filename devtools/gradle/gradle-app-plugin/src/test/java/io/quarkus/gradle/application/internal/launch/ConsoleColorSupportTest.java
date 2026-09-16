package io.quarkus.gradle.application.internal.launch;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.api.logging.configuration.ConsoleOutput;
import org.junit.jupiter.api.Test;

class ConsoleColorSupportTest {

    @Test
    void onlyPlainGradleConsoleForcesPlainQuarkusConsole() {
        for (ConsoleOutput consoleOutput : ConsoleOutput.values()) {
            assertThat(ConsoleColorSupport.forcePlainConsole(consoleOutput, null))
                    .as("Gradle console output %s", consoleOutput)
                    .isEqualTo(consoleOutput == ConsoleOutput.Plain);
        }
    }

    @Test
    void nonEmptyNoColorForcesPlainQuarkusConsole() {
        assertThat(ConsoleColorSupport.forcePlainConsole(ConsoleOutput.Auto, "1")).isTrue();
        assertThat(ConsoleColorSupport.forcePlainConsole(ConsoleOutput.Auto, "")).isFalse();
    }

    @Test
    void defaultsColorSupportFromPlainConsoleDecision() {
        assertThat(ConsoleColorSupport.jvmArgument(false, null))
                .isEqualTo("-Dio.quarkus.force-color-support=true");
        assertThat(ConsoleColorSupport.jvmArgument(true, null))
                .isEqualTo("-Dio.quarkus.force-color-support=false");
    }

    @Test
    void explicitRuntimeSettingOverridesPlainConsoleDecision() {
        assertThat(ConsoleColorSupport.jvmArgument(false, "false"))
                .isEqualTo("-Dio.quarkus.force-color-support=false");
        assertThat(ConsoleColorSupport.jvmArgument(true, "true"))
                .isEqualTo("-Dio.quarkus.force-color-support=true");
    }
}
