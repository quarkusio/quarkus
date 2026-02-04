package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

@EnabledOnOs({ OS.LINUX, OS.MAC })
@ExtendWith(SoftAssertionsExtension.class)
public class TestLauncherUtil {
    @InjectSoftAssertions
    protected SoftAssertions soft;

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
}
