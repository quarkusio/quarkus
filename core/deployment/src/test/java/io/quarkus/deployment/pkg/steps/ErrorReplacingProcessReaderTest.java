package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class ErrorReplacingProcessReaderTest {

    @Test
    void hintsOnMissingCCompiler() throws IOException {
        String output = run("[1/8] Initializing... (2.3s @ 0.50GB)\n"
                + "Error: Default native-compiler executable 'cl.exe' not found via environment variable PATH\n");
        assertThat(output).contains("native-compiler executable 'cl.exe'").contains("Hint: ").contains("C toolchain");
    }

    @Test
    void noHintForOrdinaryOutput() throws IOException {
        String output = run("[1/8] Initializing... (2.3s @ 0.50GB)\nError: Cannot allocate memory\n");
        assertThat(output).contains("Cannot allocate memory").doesNotContain("Hint: ");
    }

    private static String run(String nativeImageOutput) throws IOException {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            new ErrorReplacingProcessReader(new BufferedReader(new StringReader(nativeImageOutput)),
                    new File("does-not-exist")).run();
        } finally {
            System.setErr(originalErr);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}
