package io.quarkus.gradle.application.internal.execution.run;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForegroundProcessRunnerTest {

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);

    @TempDir
    Path testDirectory;

    @Test
    void forwardsPartialAndRawOutputBeforeTheChildExits() throws Exception {
        Path releaseFile = testDirectory.resolve("release");
        RecordingOutputStream standardOutput = new RecordingOutputStream();
        RecordingOutputStream errorOutput = new RecordingOutputStream();
        TestShutdownHooks shutdownHooks = new TestShutdownHooks();
        ForegroundProcessRunner runner = runner(ProcessBuilder::start, shutdownHooks, standardOutput, errorOutput);
        RunningInvocation invocation = runAsync(runner, childCommand("output", releaseFile.toString()));

        byte[] prompt = "prompt:\u001b[35mvalue\u001b[0m".getBytes(UTF_8);
        assertThat(standardOutput.awaitContains(prompt, TEST_TIMEOUT)).isTrue();
        assertThat(invocation.isAlive()).isTrue();

        Files.createFile(releaseFile);
        invocation.awaitCompletion();

        assertThat(invocation.failure()).isNull();
        assertThat(standardOutput.bytes()).containsExactly(
                "prompt:\u001b[35mvalue\u001b[0m\ncomplete\n".getBytes(UTF_8));
        assertThat(errorOutput.bytes()).containsExactly("error:\u001b[31mraw\u001b[0m".getBytes(UTF_8));
        assertThat(shutdownHooks.wasRemoved()).isTrue();
        assertNoOutputForwarders();
    }

    @Test
    void reportsANonZeroExitAndRemovesTheShutdownHook() throws Exception {
        TestShutdownHooks shutdownHooks = new TestShutdownHooks();
        RunningInvocation invocation = runAsync(
                runner(ProcessBuilder::start, shutdownHooks, OutputStream.nullOutputStream(), OutputStream.nullOutputStream()),
                childCommand("exit", "17"));

        invocation.awaitCompletion();

        assertThat(invocation.failure())
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("child")
                .hasMessageContaining("status 17");
        assertThat(shutdownHooks.wasRemoved()).isTrue();
    }

    @Test
    void interruptionTerminatesTheChildAndLetsItsShutdownHookComplete() throws Exception {
        Path shutdownFile = testDirectory.resolve("shutdown");
        RecordingOutputStream standardOutput = new RecordingOutputStream();
        TrackingProcessLauncher processLauncher = new TrackingProcessLauncher();
        TestShutdownHooks shutdownHooks = new TestShutdownHooks();
        RunningInvocation invocation = runAsync(
                runner(processLauncher, shutdownHooks, standardOutput, OutputStream.nullOutputStream()),
                childCommand("wait", shutdownFile.toString()));

        assertThat(standardOutput.awaitContains("ready".getBytes(UTF_8), TEST_TIMEOUT)).isTrue();
        invocation.interrupt();
        invocation.awaitCompletion();

        assertThat(invocation.failure())
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Interrupted while waiting");
        assertThat(invocation.wasInterrupted()).isTrue();
        assertThat(processLauncher.process()).isNotNull();
        assertThat(processLauncher.process().isAlive()).isFalse();
        if (!isWindows()) {
            assertThat(Files.readString(shutdownFile, UTF_8)).isEqualTo("stopped");
        }
        assertThat(shutdownHooks.wasRemoved()).isTrue();
        assertNoOutputForwarders();
    }

    @Test
    void interruptionForcesAChildThatDoesNotStopGracefully() throws Exception {
        UnstoppableProcess process = new UnstoppableProcess();
        TestShutdownHooks shutdownHooks = new TestShutdownHooks();
        ForegroundProcessRunner runner = new ForegroundProcessRunner(
                ignored -> process,
                shutdownHooks,
                OutputStream.nullOutputStream(),
                OutputStream.nullOutputStream(),
                Duration.ofMillis(20),
                Duration.ofMillis(20));
        RunningInvocation invocation = runAsync(runner, childCommand("unused"));

        assertThat(shutdownHooks.awaitAdded(TEST_TIMEOUT)).isTrue();
        invocation.interrupt();
        invocation.awaitCompletion();

        assertThat(process.destroyCalled).isTrue();
        assertThat(process.destroyForciblyCalled).isTrue();
        assertThat(process.isAlive()).isFalse();
        assertThat(invocation.failure()).isInstanceOf(GradleException.class);
        assertThat(shutdownHooks.wasRemoved()).isTrue();
    }

    @Test
    void interruptionReportsAChildThatSurvivesForcedTermination() throws Exception {
        ForceResistantProcess process = new ForceResistantProcess();
        TestShutdownHooks shutdownHooks = new TestShutdownHooks();
        ForegroundProcessRunner runner = new ForegroundProcessRunner(
                ignored -> process,
                shutdownHooks,
                OutputStream.nullOutputStream(),
                OutputStream.nullOutputStream(),
                Duration.ofMillis(20),
                Duration.ofMillis(20));
        RunningInvocation invocation = runAsync(runner, childCommand("unused"));

        assertThat(shutdownHooks.awaitAdded(TEST_TIMEOUT)).isTrue();
        invocation.interrupt();
        invocation.awaitCompletion();

        assertThat(process.destroyCalled).isTrue();
        assertThat(process.destroyForciblyCalled).isTrue();
        assertThat(process.isAlive()).isTrue();
        assertThat(invocation.failure())
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("still alive after forced termination");
        assertThat(invocation.wasInterrupted()).isTrue();
        assertThat(shutdownHooks.wasRemoved()).isTrue();
    }

    private ForegroundProcessRunner runner(ForegroundProcessRunner.ProcessLauncher processLauncher,
            TestShutdownHooks shutdownHooks, OutputStream standardOutput, OutputStream errorOutput) {
        return new ForegroundProcessRunner(processLauncher, shutdownHooks, standardOutput, errorOutput,
                Duration.ofSeconds(2), Duration.ofSeconds(2));
    }

    private RunCommand childCommand(String... arguments) {
        var commandLine = new java.util.ArrayList<String>();
        commandLine.add(javaExecutable().toString());
        commandLine.add("-cp");
        commandLine.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        commandLine.add(ChildProcess.class.getName());
        commandLine.addAll(List.of(arguments));
        return new RunCommand("child", commandLine, Optional.empty(), Optional.empty(), false, Optional.empty());
    }

    private static Path javaExecutable() {
        String executable = isWindows() ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private static void assertNoOutputForwarders() {
        assertThat(Thread.getAllStackTraces().keySet())
                .filteredOn(Thread::isAlive)
                .extracting(Thread::getName)
                .noneMatch(name -> name.startsWith("quarkus-run-child-"));
    }

    private RunningInvocation runAsync(ForegroundProcessRunner runner, RunCommand command) {
        RunningInvocation invocation = new RunningInvocation(
                () -> runner.run(command, testDirectory, Map.of("FOREGROUND_RUNNER_TEST", "true")));
        invocation.start();
        return invocation;
    }

    public static final class ChildProcess {

        private ChildProcess() {
        }

        public static void main(String[] arguments) throws Exception {
            switch (arguments[0]) {
                case "output" -> output(Path.of(arguments[1]));
                case "exit" -> System.exit(Integer.parseInt(arguments[1]));
                case "wait" -> waitUntilStopped(Path.of(arguments[1]));
                default -> throw new IllegalArgumentException("Unknown child mode: " + arguments[0]);
            }
        }

        private static void output(Path releaseFile) throws Exception {
            System.out.write("prompt:\u001b[35mvalue\u001b[0m".getBytes(UTF_8));
            System.out.flush();
            System.err.write("error:\u001b[31mraw\u001b[0m".getBytes(UTF_8));
            System.err.flush();
            try (var watchService = FileSystems.getDefault().newWatchService()) {
                releaseFile.toAbsolutePath().getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
                if (!Files.exists(releaseFile)
                        && watchService.poll(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS) == null) {
                    throw new IllegalStateException("Timed out waiting for release file " + releaseFile);
                }
            }
            System.out.write("\ncomplete\n".getBytes(UTF_8));
            System.out.flush();
        }

        private static void waitUntilStopped(Path shutdownFile) throws Exception {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.writeString(shutdownFile, "stopped", UTF_8);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }, "foreground-runner-test-shutdown"));
            System.out.write("ready".getBytes(UTF_8));
            System.out.flush();
            new CountDownLatch(1).await();
        }
    }

    private static final class RunningInvocation {

        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final Thread thread;

        private RunningInvocation(Runnable invocation) {
            thread = new Thread(() -> {
                try {
                    invocation.run();
                } catch (Throwable t) {
                    failure.set(t);
                }
            }, "foreground-process-runner-test");
        }

        private void start() {
            thread.start();
        }

        private void interrupt() {
            thread.interrupt();
        }

        private void awaitCompletion() throws InterruptedException {
            thread.join(TEST_TIMEOUT.toMillis());
            assertThat(thread.isAlive()).as("foreground runner thread stopped").isFalse();
        }

        private boolean isAlive() {
            return thread.isAlive();
        }

        private boolean wasInterrupted() {
            return thread.isInterrupted();
        }

        private Throwable failure() {
            return failure.get();
        }
    }

    private static final class RecordingOutputStream extends ByteArrayOutputStream {

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            super.write(bytes, offset, length);
            notifyAll();
        }

        private synchronized boolean awaitContains(byte[] expected, Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (!contains(toByteArray(), expected)) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
            }
            return true;
        }

        private synchronized byte[] bytes() {
            return toByteArray();
        }

        private static boolean contains(byte[] haystack, byte[] needle) {
            for (int start = 0; start <= haystack.length - needle.length; start++) {
                int index = 0;
                while (index < needle.length && haystack[start + index] == needle[index]) {
                    index++;
                }
                if (index == needle.length) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class TestShutdownHooks implements ForegroundProcessRunner.ShutdownHooks {

        private final CountDownLatch added = new CountDownLatch(1);
        private final AtomicReference<Thread> registered = new AtomicReference<>();
        private final AtomicReference<Thread> removed = new AtomicReference<>();

        @Override
        public void add(Thread hook) {
            registered.set(hook);
            added.countDown();
        }

        @Override
        public void remove(Thread hook) {
            removed.set(hook);
        }

        private boolean awaitAdded(Duration timeout) throws InterruptedException {
            return added.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        private boolean wasRemoved() {
            return removed.get() != null && removed.get() == registered.get();
        }
    }

    private static final class TrackingProcessLauncher implements ForegroundProcessRunner.ProcessLauncher {

        private volatile Process process;

        @Override
        public Process start(ProcessBuilder processBuilder) throws IOException {
            process = processBuilder.start();
            return process;
        }

        private Process process() {
            return process;
        }
    }

    private static final class UnstoppableProcess extends Process {

        private final CountDownLatch completion = new CountDownLatch(1);
        private volatile boolean alive = true;
        private volatile boolean destroyCalled;
        private volatile boolean destroyForciblyCalled;

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            completion.await();
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            return completion.await(timeout, unit);
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("Process is still running");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalled = true;
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalled = true;
            alive = false;
            completion.countDown();
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }
    }

    private static final class ForceResistantProcess extends Process {

        private final CountDownLatch completion = new CountDownLatch(1);
        private volatile boolean destroyCalled;
        private volatile boolean destroyForciblyCalled;

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            completion.await();
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public int exitValue() {
            throw new IllegalThreadStateException("Process is still running");
        }

        @Override
        public void destroy() {
            destroyCalled = true;
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalled = true;
            return this;
        }

        @Override
        public boolean isAlive() {
            return true;
        }
    }
}
