package io.quarkus.deployment.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Runs owned helper processes with bounded execution and termination waits.
 * <p>
 * Output capture is redirected through a temporary file so a child that writes more than an operating-system pipe
 * buffer cannot block. A process started by this utility is either reaped before a normal return or subjected to
 * bounded forced termination before an exception is reported.
 */
public final class BoundedProcessRunner {

    private BoundedProcessRunner() {
    }

    /**
     * Runs a process, captures its merged output, and proves that it has exited before returning.
     * <p>
     * This method takes ownership of the process it starts. It replaces the builder's output and error redirects,
     * merges both streams, and removes its temporary output after the child has been reaped. On timeout or
     * interruption it forcibly destroys the child and performs a bounded wait before returning control to the caller.
     *
     * @param processBuilder configured process builder; its output and error redirects are replaced
     * @param timeout maximum normal execution time
     * @param forceTimeout maximum time to wait after forced termination
     * @param description description used in failure messages
     * @return the exit code and merged output
     * @throws IOException if the process cannot be started, times out, or cannot be reaped
     * @throws InterruptedException if the caller is interrupted after bounded cleanup is attempted
     */
    public static Result capture(java.lang.ProcessBuilder processBuilder, Duration timeout, Duration forceTimeout,
            String description) throws IOException, InterruptedException {
        Path outputFile = Files.createTempFile("quarkus-process-", ".log");
        Process process = null;
        boolean deleteOutput = true;
        try {
            process = processBuilder
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();
            try {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    if (!forceTerminateAndWait(process, forceTimeout)) {
                        deleteOutput = false;
                        throw new IOException(description + " timed out and did not terminate after it was forcibly destroyed");
                    }
                    throw new IOException(description + " timed out");
                }
            } catch (InterruptedException e) {
                if (!forceTerminateAndWait(process, forceTimeout)) {
                    deleteOutput = false;
                    e.addSuppressed(new IOException(
                            description + " did not terminate after it was forcibly destroyed"));
                }
                throw e;
            }
            return new Result(process.exitValue(), Files.readString(outputFile, StandardCharsets.UTF_8));
        } finally {
            if (process != null && process.isAlive()) {
                deleteOutput = false;
            }
            if (deleteOutput) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException e) {
                    outputFile.toFile().deleteOnExit();
                }
            } else {
                outputFile.toFile().deleteOnExit();
            }
        }
    }

    /**
     * Forcibly terminates a process and waits for it to exit for at most the given duration.
     * Interruptions during cleanup are restored before this method returns.
     *
     * @param process the process to terminate and reap
     * @param timeout the maximum termination wait
     * @return {@code true} if the process is no longer alive, otherwise {@code false}
     */
    public static boolean forceTerminateAndWait(Process process, Duration timeout) {
        if (!process.isAlive()) {
            return true;
        }
        process.destroyForcibly();
        boolean interrupted = false;
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (process.isAlive()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                try {
                    if (process.waitFor(remaining, TimeUnit.NANOSECONDS)) {
                        return true;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            return true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * The completed process result returned by {@link #capture(java.lang.ProcessBuilder, Duration, Duration, String)}.
     *
     * @param exitCode the child process exit code
     * @param output the merged standard output and standard error decoded as UTF-8
     */
    public record Result(int exitCode, String output) {
    }
}
