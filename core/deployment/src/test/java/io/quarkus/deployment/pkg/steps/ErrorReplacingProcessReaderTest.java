package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ErrorReplacingProcessReaderTest {

    @Test
    void hintsOnMissingCCompiler() {
        assertThat(ErrorReplacingProcessReader.hintForLine(
                "Error: Default native-compiler executable 'cl.exe' not found via environment variable PATH"))
                .isNotNull()
                .contains("C toolchain");
    }

    @Test
    void noHintForOrdinaryOutput() {
        assertThat(ErrorReplacingProcessReader.hintForLine("[1/8] Initializing... (2.3s @ 0.50GB)")).isNull();
        assertThat(ErrorReplacingProcessReader.hintForLine("Error: Cannot allocate memory")).isNull();
    }
}
