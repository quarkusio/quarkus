package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.smallrye.common.process.AbnormalExitException;

class NativeImageBuildRunnerTest {

    @TempDir
    Path target;

    @Test
    void capturesStreamsSeparatelyAndReplacesPreviousLogs() throws Exception {
        Files.writeString(target.resolve("runner-build-stdout.log"), "stale stdout");
        Files.writeString(target.resolve("runner-build-stderr.log"), "stale stderr");
        build(false, true);
        assertThat(Files.readString(target.resolve("runner-build-stdout.log")))
                .isEqualTo("native stdout" + System.lineSeparator());
        assertThat(Files.readString(target.resolve("runner-build-stderr.log")))
                .isEqualTo("native stderr" + System.lineSeparator());
    }

    @Test
    void preservesBothLogsOnFailure() {
        assertThatThrownBy(() -> build(true, true)).isInstanceOf(AbnormalExitException.class);
        assertThat(target.resolve("runner-build-stdout.log")).content()
                .isEqualTo("native stdout" + System.lineSeparator());
        assertThat(target.resolve("runner-build-stderr.log")).content()
                .isEqualTo("native stderr" + System.lineSeparator());
    }

    @Test
    void doesNotCreateLogsByDefault() throws Exception {
        build(false, false);
        assertThat(target.resolve("runner-build-stdout.log")).doesNotExist();
        assertThat(target.resolve("runner-build-stderr.log")).doesNotExist();
    }

    @ParameterizedTest
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    @ResourceLock(Resources.SYSTEM_OUT)
    @ResourceLock(Resources.SYSTEM_ERR)
    void preservesConsoleStreams(boolean enableReports, boolean fail) throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try (PrintStream capturedOut = new PrintStream(stdout, true, StandardCharsets.UTF_8);
                PrintStream capturedErr = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            System.setErr(capturedErr);
            try {
                if (fail) {
                    assertThatThrownBy(() -> build(true, enableReports)).isInstanceOf(AbnormalExitException.class);
                } else {
                    build(false, enableReports);
                }
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        }
        assertThat(stdout.toString(StandardCharsets.UTF_8)).contains("native stdout").doesNotContain("native stderr");
        assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("native stderr").doesNotContain("native stdout");
    }

    private void build(boolean fail, boolean enableReports) throws IOException, InterruptedException {
        Path outputDir = Files.createDirectories(target.resolve("native-image-source-jar"));
        NativeImageBuildRunner runner = new NativeImageBuildRunner() {
            @Override
            public boolean isContainer() {
                return false;
            }

            @Override
            protected String[] getGraalVMVersionCommand(List<String> args) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected String[] getBuildCommand(Path outputDir, List<String> args) {
                List<String> command = new ArrayList<>();
                command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
                command.add("-cp");
                command.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
                command.add(BuildProcess.class.getName());
                command.add(Boolean.toString(fail));
                return command.toArray(String[]::new);
            }

            @Override
            protected void objcopy(Path outputDir, String... args) {
            }
        };
        if (enableReports) {
            runner.build(List.of(), "runner", "runner", outputDir, null, false, false, true);
        } else {
            runner.build(List.of(), "runner", "runner", outputDir, null, false, false);
        }
    }

    public static class BuildProcess {
        public static void main(String[] args) {
            System.out.println("native stdout");
            System.err.println("native stderr");
            if (Boolean.parseBoolean(args[0])) {
                System.exit(1);
            }
        }
    }
}
