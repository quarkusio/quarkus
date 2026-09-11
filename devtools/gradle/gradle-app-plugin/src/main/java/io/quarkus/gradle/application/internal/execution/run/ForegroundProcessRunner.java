package io.quarkus.gradle.application.internal.execution.run;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gradle.api.GradleException;

public final class ForegroundProcessRunner {

    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration OUTPUT_TIMEOUT = Duration.ofSeconds(5);

    private final ProcessLauncher processLauncher;
    private final ShutdownHooks shutdownHooks;
    private final OutputStream standardOutput;
    private final OutputStream errorOutput;
    private final Duration stopTimeout;
    private final Duration outputTimeout;

    public ForegroundProcessRunner() {
        this(ProcessBuilder::start, new RuntimeShutdownHooks(), System.out, System.err, STOP_TIMEOUT, OUTPUT_TIMEOUT);
    }

    ForegroundProcessRunner(ProcessLauncher processLauncher, ShutdownHooks shutdownHooks, OutputStream standardOutput,
            OutputStream errorOutput, Duration stopTimeout, Duration outputTimeout) {
        this.processLauncher = processLauncher;
        this.shutdownHooks = shutdownHooks;
        this.standardOutput = standardOutput;
        this.errorOutput = errorOutput;
        this.stopTimeout = stopTimeout;
        this.outputTimeout = outputTimeout;
    }

    public void run(RunCommand command, Path defaultWorkingDirectory, Map<String, String> environment) {
        Path workingDirectory = command.workingDirectory().orElse(defaultWorkingDirectory);
        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(command.arguments())
                    .directory(workingDirectory.toFile())
                    .redirectInput(ProcessBuilder.Redirect.INHERIT);
            builder.environment().putAll(environment);
            process = processLauncher.start(builder);
        } catch (IOException e) {
            throw new GradleException("Failed to launch Quarkus run command '" + command.name() + "'", e);
        }

        OutputForwarder outputForwarder = forwardOutput(process.getInputStream(), standardOutput,
                command.name() + "-stdout");
        OutputForwarder errorForwarder = forwardOutput(process.getErrorStream(), errorOutput,
                command.name() + "-stderr");
        Thread shutdownHook = new Thread(() -> {
            if (!stop(process)) {
                reportCleanupFailure("Quarkus run command '" + command.name()
                        + "' is still alive after forced termination");
            }
        }, "quarkus-run-process-shutdown");
        shutdownHooks.add(shutdownHook);
        try {
            int exitCode = process.waitFor();
            waitForOutput(outputForwarder, errorForwarder);
            if (exitCode != 0) {
                throw new GradleException("Quarkus run command '" + command.name()
                        + "' exited with status " + exitCode);
            }
        } catch (InterruptedException e) {
            boolean terminated = stop(process);
            closeAndWaitForOutput(outputForwarder, errorForwarder);
            Thread.currentThread().interrupt();
            String message = "Interrupted while waiting for Quarkus run command '" + command.name() + "'";
            if (!terminated) {
                message += "; the child is still alive after forced termination";
            }
            throw new GradleException(message, e);
        } finally {
            try {
                shutdownHooks.remove(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM shutdown is already in progress and the hook is running.
            }
        }
    }

    private OutputForwarder forwardOutput(InputStream stream, OutputStream target, String threadName) {
        OutputForwarder forwarder = new OutputForwarder(stream, target, errorOutput, "quarkus-run-" + threadName);
        forwarder.start();
        return forwarder;
    }

    private void waitForOutput(OutputForwarder... forwarders) throws InterruptedException {
        for (OutputForwarder forwarder : forwarders) {
            if (!forwarder.await(outputTimeout)) {
                forwarder.close();
                if (!forwarder.await(outputTimeout)) {
                    forwarder.interrupt();
                }
            }
        }
    }

    private void closeAndWaitForOutput(OutputForwarder... forwarders) {
        boolean interrupted = false;
        for (OutputForwarder forwarder : forwarders) {
            forwarder.close();
            try {
                if (!forwarder.await(outputTimeout)) {
                    forwarder.interrupt();
                }
            } catch (InterruptedException e) {
                interrupted = true;
                forwarder.interrupt();
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean stop(Process process) {
        if (!process.isAlive()) {
            return true;
        }
        process.destroy();
        boolean interrupted = false;
        try {
            try {
                if (process.waitFor(stopTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    return true;
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            try {
                return process.waitFor(stopTimeout.toMillis(), TimeUnit.MILLISECONDS) || !process.isAlive();
            } catch (InterruptedException e) {
                interrupted = true;
                return !process.isAlive();
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void reportCleanupFailure(String message) {
        try {
            errorOutput.write((message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            errorOutput.flush();
        } catch (IOException ignored) {
            // The original process-cleanup failure is more useful than a secondary diagnostic failure.
        }
    }

    @FunctionalInterface
    interface ProcessLauncher {
        Process start(ProcessBuilder processBuilder) throws IOException;
    }

    interface ShutdownHooks {

        void add(Thread hook);

        void remove(Thread hook);
    }

    private static final class RuntimeShutdownHooks implements ShutdownHooks {

        @Override
        public void add(Thread hook) {
            Runtime.getRuntime().addShutdownHook(hook);
        }

        @Override
        public void remove(Thread hook) {
            Runtime.getRuntime().removeShutdownHook(hook);
        }
    }

    private static final class OutputForwarder {

        private static final int BUFFER_SIZE = 8192;

        private final InputStream source;
        private final Thread thread;
        private volatile boolean closing;

        private OutputForwarder(InputStream source, OutputStream target, OutputStream errorOutput, String threadName) {
            this.source = source;
            this.thread = new Thread(() -> copy(source, target, errorOutput), threadName);
            this.thread.setDaemon(true);
        }

        private void start() {
            thread.start();
        }

        private boolean await(Duration timeout) throws InterruptedException {
            thread.join(timeout.toMillis());
            return !thread.isAlive();
        }

        private void close() {
            closing = true;
            try {
                source.close();
            } catch (IOException ignored) {
                // The process may have closed the stream concurrently.
            }
        }

        private void interrupt() {
            thread.interrupt();
        }

        private void copy(InputStream source, OutputStream target, OutputStream errorOutput) {
            byte[] buffer = new byte[BUFFER_SIZE];
            try (source) {
                int read;
                while ((read = source.read(buffer)) != -1) {
                    target.write(buffer, 0, read);
                    target.flush();
                }
            } catch (IOException e) {
                if (!closing && !Thread.currentThread().isInterrupted()) {
                    reportFailure(errorOutput, e);
                }
            }
        }

        private static void reportFailure(OutputStream errorOutput, IOException failure) {
            String message = "Failed to read Quarkus run command output: " + failure.getMessage()
                    + System.lineSeparator();
            try {
                errorOutput.write(message.getBytes(StandardCharsets.UTF_8));
                errorOutput.flush();
            } catch (IOException ignored) {
                // There is no remaining output channel on which to report this failure.
            }
        }
    }
}
