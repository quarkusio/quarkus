package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import io.quarkus.gradle.testing.BaseGradleTest;

abstract class ContinuousBuildTestSupport extends BaseGradleTest {

    protected static final Duration BUILD_START_TIMEOUT = Duration.ofMinutes(2);
    protected static final Duration RELOAD_TIMEOUT = Duration.ofMinutes(1);

    private static final Duration CANCEL_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration AWAIT_POLL_INTERVAL = Duration.ofMillis(200);

    protected ContinuousBuild startContinuousBuild(String taskName, String... taskArguments) {
        return new ContinuousBuild(taskName, List.copyOf(Arrays.asList(taskArguments)));
    }

    protected static String pluginClasspathFiles() {
        return TestKitPluginClasspath.implementationClasspath().stream()
                .map(File::getAbsolutePath)
                .map(ContinuousBuildTestSupport::singleQuotedGroovyString)
                .collect(Collectors.joining(", "));
    }

    protected static boolean fileContains(Path file, String... fragments) {
        try {
            if (!Files.isRegularFile(file)) {
                return false;
            }
            String content = Files.readString(file);
            return Arrays.stream(fragments).allMatch(content::contains);
        } catch (IOException e) {
            return false;
        }
    }

    protected static int occurrences(String value, String fragment) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(fragment, offset)) >= 0) {
            count++;
            offset += fragment.length();
        }
        return count;
    }

    protected static void assertDirectoryCanBeMoved(Path directory) throws IOException {
        Path moved = directory.resolveSibling(directory.getFileName() + "-after-cancellation");
        Files.move(directory, moved);
        Files.move(moved, directory);
    }

    private static String singleQuotedGroovyString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    protected final class ContinuousBuild implements AutoCloseable {

        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        private final CancellationTokenSource tokenSource = GradleConnector.newCancellationTokenSource();
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Future<?> build;

        private ContinuousBuild(String taskName, List<String> taskArguments) {
            build = executor.submit(() -> runContinuousBuild(taskName, taskArguments));
        }

        void await(String description, Duration timeout, BooleanSupplier condition) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                awaitNextPollOrFail(description, 0);
                if (condition.getAsBoolean()) {
                    awaitNextPollOrFail(description, 0);
                    return;
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    break;
                }
                awaitNextPollOrFail(description, Math.min(remaining, AWAIT_POLL_INTERVAL.toNanos()));
            }
            fail("Timed out waiting for %s.%nstdout:%n%s%nstderr:%n%s",
                    description, stdout(), stderr());
        }

        String stdout() {
            return output(stdout);
        }

        @Override
        public void close() throws InterruptedException {
            awaitNextPollOrFail("test cleanup", 0);
            tokenSource.cancel();
            try {
                awaitCancellation();
            } finally {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(CANCEL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
            }
        }

        private void runContinuousBuild(String taskName, List<String> taskArguments) {
            var arguments = new String[4 + taskArguments.size()];
            arguments[0] = "--continuous";
            arguments[1] = NO_CONFIGURATION_CACHE;
            arguments[2] = STACKTRACE;
            arguments[3] = taskName;
            for (int i = 0; i < taskArguments.size(); i++) {
                arguments[i + 4] = taskArguments.get(i);
            }
            try (ProjectConnection connection = GradleConnector.newConnector()
                    .forProjectDirectory(testProjectDir.toFile())
                    .connect()) {
                connection.newBuild()
                        // Gradle currently cannot combine continuous build with configuration-cache reuse. Isolated
                        // projects imply the configuration cache, so this smoke intentionally tests continuous mode alone.
                        .withArguments(arguments)
                        .withCancellationToken(tokenSource.token())
                        .setStandardOutput(stdout)
                        .setStandardError(stderr)
                        .run();
            }
        }

        private void awaitNextPollOrFail(String description, long timeoutNanos) throws InterruptedException {
            try {
                build.get(timeoutNanos, TimeUnit.NANOSECONDS);
                fail("Continuous build completed unexpectedly while waiting for %s.%nstdout:%n%s%nstderr:%n%s",
                        description, stdout(), stderr());
            } catch (CancellationException e) {
                throw new AssertionError("Continuous build was cancelled unexpectedly while waiting for " + description
                        + ".\nstdout:\n" + stdout() + "\nstderr:\n" + stderr(), e);
            } catch (ExecutionException e) {
                throw new AssertionError("Continuous build failed unexpectedly while waiting for " + description
                        + ".\nstdout:\n" + stdout() + "\nstderr:\n" + stderr(), e.getCause());
            } catch (TimeoutException ignored) {
                // The continuous build is still running; check the awaited condition again.
            }
        }

        private void awaitCancellation() throws InterruptedException {
            try {
                build.get(CANCEL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                if (!isExpectedCancellation(e.getCause())) {
                    fail("Continuous build failed before cancellation completed.%nstdout:%n%s%nstderr:%n%s",
                            stdout(), stderr(), e.getCause());
                }
            } catch (TimeoutException e) {
                fail("Timed out cancelling continuous build.%nstdout:%n%s%nstderr:%n%s",
                        stdout(), stderr(), e);
            }
        }

        private String stderr() {
            return output(stderr);
        }
    }

    private static boolean isExpectedCancellation(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            String className = current.getClass().getName();
            String message = Objects.toString(current.getMessage(), "");
            if (className.equals("org.gradle.tooling.BuildCancelledException")
                    || className.equals("org.gradle.tooling.exceptions.BuildCancelledException")
                    || message.contains("Build cancelled")
                    || message.contains("cancelled")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String output(ByteArrayOutputStream output) {
        return output.toString(StandardCharsets.UTF_8);
    }
}
