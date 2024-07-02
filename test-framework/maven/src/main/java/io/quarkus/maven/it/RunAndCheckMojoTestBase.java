package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterEach;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.devmode.util.DevModeClient;

public class RunAndCheckMojoTestBase extends MojoTestBase {

    protected RunningInvoker running;
    protected File testDir;
    protected DevModeClient devModeClient = new DevModeClient(getPort());
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_MEMORY_IN_MB = 128;

    /**
     * Default to port 8080, but allow subtests to override.
     */
    protected int getPort() {
        return DEFAULT_PORT;
    }

    /**
     * Default to quite constrained memory, but allow subclasses to override, for hungrier tests.
     */

    protected int getAllowedHeapInMb() {
        return DEFAULT_MEMORY_IN_MB;
    }

    @AfterEach
    public void cleanup() {
        shutdownTheApp();
    }

    public void shutdownTheApp() {
        if (running != null) {
            running.stop();
        }
        devModeClient.awaitUntilServerDown();
    }

    /**
     * Quarkus can be launched as `quarkus:dev` or `quarkus:test`.
     * In most cases it doesn't matter and dev mode is fine, but sometimes it's useful to cover test mode,
     * since it sometimes behaves differently.
     */
    protected LaunchMode getDefaultLaunchMode() {
        return LaunchMode.DEVELOPMENT;
    }

    protected void run(boolean performCompile, String... options) throws FileNotFoundException, MavenInvocationException {
        run(performCompile, getDefaultLaunchMode(), options);
    }

    protected void run(boolean performCompile, LaunchMode mode, String... options)
            throws MavenInvocationException, FileNotFoundException {
        run(performCompile, mode, true, options);
    }

    protected void run(boolean performCompile, LaunchMode mode, boolean skipAnalytics, String... options)
            throws FileNotFoundException, MavenInvocationException {
        assertThat(testDir).isDirectory();
        assertThatPortIsFree();

        running = new RunningInvoker(testDir, false);

        final List<String> args = new ArrayList<>(3 + options.length);
        if (performCompile) {
            args.add("compile");
        }
        args.add("quarkus:" + mode.getDefaultProfile());
        if (skipAnalytics) {
            args.add("-Dquarkus.analytics.disabled=true");
        }

        // If the test has set a different port, pass that on to the application
        if (getPort() != DEFAULT_PORT) {
            int port = getPort();
            int testPort = getPort() + 1;
            args.add("-Dquarkus.http.port=" + port);
            args.add("-Dquarkus.http.test-port=" + testPort);
        }

        boolean hasDebugOptions = false;
        for (String option : options) {
            args.add(option);
            if (option.trim().startsWith("-Ddebug=") || option.trim().startsWith("-Dsuspend=")) {
                hasDebugOptions = true;
            }
        }
        if (!hasDebugOptions) {
            // if no explicit debug options have been specified, let's just disable debugging
            args.add("-Ddebug=false");
        }

        //we need to limit the memory consumption, as we can have a lot of these processes
        //running at once, if they add default to 75% of total mem we can easily run out
        //of physical memory as they will consume way more than what they need instead of
        //just running GC
        args.add("-Djvm.args=-Xmx" + getAllowedHeapInMb() + "m");
        running.execute(args, Map.of());
    }

    private void assertThatPortIsFree() {
        try {
            // Call get(), which doesn't retry - otherwise, tests will go very slow
            devModeClient.get();
            fail("The port " + getPort()
                    + " appears to be in use before starting the test instance of Quarkus, so any tests will give unpredictable results.");
        } catch (IOException e) {
            // All good, we wanted this
        }
    }

    protected void runAndCheck(String... options) throws FileNotFoundException, MavenInvocationException {
        runAndCheck(true, options);
    }

    protected void runAndCheck(LaunchMode mode, String... options) throws FileNotFoundException, MavenInvocationException {
        runAndCheck(true, mode, options);
    }

    protected void runAndCheck(boolean performCompile, String... options)
            throws MavenInvocationException, FileNotFoundException {
        runAndCheck(performCompile, getDefaultLaunchMode(), options);
    }

    protected void runAndCheck(boolean performCompile, LaunchMode mode, String... options)
            throws FileNotFoundException, MavenInvocationException {
        run(performCompile, mode, options);

        String resp = devModeClient.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = devModeClient.getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    protected void runAndExpectError() throws MavenInvocationException {
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        final Properties mvnRunProps = new Properties();
        mvnRunProps.setProperty("debug", "false");
        running.execute(Arrays.asList("compile", "quarkus:dev"), Map.of(), mvnRunProps);

        devModeClient.getHttpErrorResponse();
    }

    protected void install(final File baseDir, final boolean performClean) throws Exception {
        final MavenProcessInvocationResult result = new RunningInvoker(baseDir, false)
                .execute(performClean ? List.of("clean", "install") : List.of("install"), Map.of());
        final Process process = result.getProcess();
        if (process == null) {
            if (result.getExecutionException() == null) {
                throw new IllegalStateException("Failed to build project");
            }
            throw result.getExecutionException();
        }
        process.waitFor();
    }

    public String getHttpErrorResponse() {
        return devModeClient.getHttpErrorResponse(getBrokenReason());
    }

    public String getHttpResponse() {
        return devModeClient.getHttpResponse(getBrokenReason());
    }

    protected Supplier<String> getBrokenReason() {
        return () -> null;
    }
}
