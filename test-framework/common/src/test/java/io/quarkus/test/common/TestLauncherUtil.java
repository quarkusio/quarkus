package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.runtime.ValueRegistryImpl;
import io.smallrye.config.Config;
import io.smallrye.config.SmallRyeConfigBuilder;

@EnabledOnOs({ OS.LINUX, OS.MAC })
@ExtendWith(SoftAssertionsExtension.class)
public class TestLauncherUtil {
    @InjectSoftAssertions
    protected SoftAssertions soft;

    @TempDir
    Path tempDir;

    @Test
    public void lastLineNoEOL() throws Exception {
        var proc = new ProcessBuilder("/bin/bash", "-c",
                "echo hello; echo world; echo oops > /dev/stderr; echo -n dogs; echo -n \" are great!\"").start();

        var outCapture = new ByteArrayOutputStream();
        var errCapture = new ByteArrayOutputStream();

        var captureThread = new Thread(new LauncherUtil.ProcessReader(proc, outCapture, errCapture));
        captureThread.start();
        captureThread.join(120_000L);

        soft.assertThatCode(proc::exitValue).doesNotThrowAnyException();
        soft.assertThat(outCapture.toString()).isEqualTo("""
                hello
                world
                dogs are great!""");
        soft.assertThat(errCapture.toString()).isEqualTo("""
                oops
                """);
    }

    @Test
    public void exitNon0() throws Exception {
        var proc = new ProcessBuilder("/bin/bash", "-c", "echo hello; echo world; echo oops > /dev/stderr; echo dogs; exit 1")
                .start();

        var outCapture = new ByteArrayOutputStream();
        var errCapture = new ByteArrayOutputStream();

        var captureThread = new Thread(new LauncherUtil.ProcessReader(proc, outCapture, errCapture));
        captureThread.start();
        captureThread.join(120_000L);

        soft.assertThatCode(proc::exitValue).doesNotThrowAnyException();
        soft.assertThat(outCapture.toString()).isEqualTo("""
                hello
                world
                dogs
                """);
        soft.assertThat(errCapture.toString()).isEqualTo("""
                oops
                """);
    }

    @Test
    public void lotsOfChars() throws Exception {
        var longString = "a".repeat(10_000);
        var proc = new ProcessBuilder("/bin/bash", "-c",
                "echo hello; echo " + longString + "; echo oops > /dev/stderr; echo " + longString + " > /dev/stderr")
                .start();

        var outCapture = new ByteArrayOutputStream();
        var errCapture = new ByteArrayOutputStream();

        var captureThread = new Thread(new LauncherUtil.ProcessReader(proc, outCapture, errCapture) {
            // This override ensures that ProcessReader.flush() receive the "long strings" that
            // exceed the read buffer size.
            @Override
            int checkExited(int exited) {
                try {
                    assertThat(proc.waitFor(120_000L, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return super.checkExited(exited);
            }
        });
        captureThread.start();
        captureThread.join(120_000L);

        soft.assertThatCode(proc::exitValue).doesNotThrowAnyException();
        soft.assertThat(outCapture.toString()).isEqualTo("""
                hello
                """ + longString + "\n");
        soft.assertThat(errCapture.toString()).isEqualTo("""
                oops
                """ + longString + "\n");
    }

    @Test
    public void exitImmediately() throws Exception {
        var proc = new ProcessBuilder("/bin/bash", "-c", "exit 1")
                .start();

        var outCapture = new ByteArrayOutputStream();
        var errCapture = new ByteArrayOutputStream();

        var captureThread = new Thread(new LauncherUtil.ProcessReader(proc, outCapture, errCapture));
        captureThread.start();
        captureThread.join(120_000L);

        soft.assertThatCode(proc::exitValue).doesNotThrowAnyException();
        soft.assertThat(outCapture.toString()).isEmpty();
        soft.assertThat(errCapture.toString()).isEmpty();
    }

    @Test
    public void waitForCapturedListeningDataCapturesManagementPort() throws Exception {
        Path logFile = tempDir.resolve("quarkus.log");
        var proc = new ProcessBuilder("/bin/bash", "-c",
                "printf '%s\\n' '2026-07-16 10:00:00,000 INFO  [io.quarkus] (main) started in 1.000s. Listening on: http://0.0.0.0:38133. Management interface listening on http://0.0.0.0:37247.' > "
                        + logFile + "; sleep 120")
                .start();

        try {
            Optional<ListeningAddress> listeningAddress = LauncherUtil.waitForCapturedListeningData(proc, logFile, 5);

            soft.assertThat(listeningAddress).isPresent();
            soft.assertThat(listeningAddress.get().port()).isEqualTo(38133);
            soft.assertThat(listeningAddress.get().protocol()).isEqualTo("http");
            soft.assertThat(listeningAddress.get().managementPort()).isEqualTo(37247);
            soft.assertThat(listeningAddress.get().managementProtocol()).isEqualTo("http");
        } finally {
            proc.destroyForcibly();
        }
    }

    @Test
    public void listeningAddressRegistersManagementPort() {
        var valueRegistry = ValueRegistryImpl.builder().build();

        Config config = new SmallRyeConfigBuilder().build();
        new ListeningAddress(38133, "http", 37247, "http").register(valueRegistry, config);

        soft.assertThat(valueRegistry.getOrDefault(ListeningAddress.HTTP_TEST_PORT, -1)).isEqualTo(38133);
        soft.assertThat(valueRegistry.getOrDefault(ListeningAddress.MANAGEMENT_TEST_PORT, -1)).isEqualTo(37247);
    }
}
