package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterEach;

import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class RunAndCheckMojoTestBase extends MojoTestBase {

    protected RunningInvoker running;
    protected File testDir;

    @AfterEach
    public void cleanup() throws IOException {
        shutdownTheApp();
    }

    public void shutdownTheApp() {
        if (running != null) {
            running.stop();
        }
        DevModeTestUtils.awaitUntilServerDown();
    }

    protected void run(boolean performCompile, String... options) throws FileNotFoundException, MavenInvocationException {
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        final List<String> args = new ArrayList<>(2 + options.length);
        if (performCompile) {
            args.add("compile");
        }
        args.add("quarkus:dev");
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
        args.add("-Djvm.args=-Xmx128m");
        running.execute(args, Collections.emptyMap());
    }

    protected void runAndCheck(String... options) throws FileNotFoundException, MavenInvocationException {
        runAndCheck(true, options);
    }

    protected void runAndCheck(boolean performCompile, String... options)
            throws FileNotFoundException, MavenInvocationException {
        run(performCompile, options);

        String resp = DevModeTestUtils.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = DevModeTestUtils.getHttpResponse("/app/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    protected void runAndExpectError() throws FileNotFoundException, MavenInvocationException {
        assertThat(testDir).isDirectory();
        running = new RunningInvoker(testDir, false);
        final Properties mvnRunProps = new Properties();
        mvnRunProps.setProperty("debug", "false");
        running.execute(Arrays.asList("compile", "quarkus:dev"), Collections.emptyMap(), mvnRunProps);

        DevModeTestUtils.getHttpErrorResponse();
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
