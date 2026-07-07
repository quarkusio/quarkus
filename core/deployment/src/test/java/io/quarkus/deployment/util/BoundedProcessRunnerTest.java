package io.quarkus.deployment.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class BoundedProcessRunnerTest {

    private static final Duration FORCE_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void capturesOutputLargerThanAProcessPipe() throws Exception {
        BoundedProcessRunner.Result result = BoundedProcessRunner.capture(
                javaProcess(OutputFlood.class),
                Duration.ofSeconds(10),
                FORCE_TIMEOUT,
                "large-output helper");

        String output = result.output();
        String terminator = "done" + System.lineSeparator();
        assertThat(result.exitCode()).isZero();
        assertThat(output.length()).isEqualTo(2 * 1024 * 1024 + terminator.length());
        assertThat(output.substring(output.length() - terminator.length())).isEqualTo(terminator);
    }

    @Test
    void timesOutAndReapsProcess() {
        assertThatThrownBy(() -> BoundedProcessRunner.capture(
                javaProcess(Sleeper.class),
                Duration.ofMillis(50),
                FORCE_TIMEOUT,
                "sleeping helper"))
                .isInstanceOf(java.io.IOException.class)
                .hasMessage("sleeping helper timed out");
    }

    @Test
    void interruptionPerformsBoundedCleanupBeforeReturning() throws Exception {
        var failure = new AtomicReference<Throwable>();
        Thread runner = new Thread(() -> {
            try {
                BoundedProcessRunner.capture(
                        javaProcess(Sleeper.class),
                        Duration.ofMinutes(1),
                        FORCE_TIMEOUT,
                        "interrupted helper");
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        runner.start();
        Thread.sleep(250);

        runner.interrupt();
        runner.join(Duration.ofSeconds(10));

        assertThat(runner.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(InterruptedException.class);
    }

    private static java.lang.ProcessBuilder javaProcess(Class<?> mainClass) {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path testClasses;
        try {
            testClasses = Path.of(BoundedProcessRunnerTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return new java.lang.ProcessBuilder(java.toString(), "-cp", testClasses.toString(), mainClass.getName());
    }

    public static final class OutputFlood {

        public static void main(String[] args) {
            System.out.print("x".repeat(2 * 1024 * 1024));
            System.out.println("done");
        }
    }

    public static final class Sleeper {

        public static void main(String[] args) throws InterruptedException {
            Thread.sleep(Duration.ofMinutes(1));
        }
    }
}
