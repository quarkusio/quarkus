package io.quarkus.test.common;

import java.io.ByteArrayOutputStream;

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
