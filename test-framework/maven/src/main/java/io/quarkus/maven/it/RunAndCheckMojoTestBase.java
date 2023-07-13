package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
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
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class RunAndCheckMojoTestBase extends MojoTestBase {

    protected RunningInvoker running;
    protected File testDir;

    @AfterEach
    public void cleanup() {
        shutdownTheApp();
    }

    public void shutdownTheApp() {
        if (running != null) {
            running.stop();
        }
        DevModeTestUtils.awaitUntilServerDown();
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
        running = new RunningInvoker(testDir, false);

        final List<String> args = new ArrayList<>(3 + options.length);
        if (performCompile) {
            args.add("compile");
        }
        args.add("quarkus:" + mode.getDefaultProfile());
        if (skipAnalytics) {
            args.add("-Dquarkus.analytics.disabled=true");
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
        args.add("-Djvm.args=-Xmx192m");
        running.execute(args, Map.of());
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

        String resp = DevModeTestUtils.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = DevModeTestUtils.getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    protected void runAndExpectError() throws MavenInvocationException {
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        final Properties mvnRunProps = new Properties();
        mvnRunProps.setProperty("debug", "false");
        running.execute(Arrays.asList("compile", "quarkus:dev"), Map.of(), mvnRunProps);

        DevModeTestUtils.getHttpErrorResponse();
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
        return DevModeTestUtils.getHttpErrorResponse(getBrokenReason());
    }

    public String getHttpResponse() {
        return DevModeTestUtils.getHttpResponse(getBrokenReason());
    }

    protected Supplier<String> getBrokenReason() {
        return () -> null;
    }
}
