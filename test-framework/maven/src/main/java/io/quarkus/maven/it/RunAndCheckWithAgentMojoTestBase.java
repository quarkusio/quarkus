package io.quarkus.maven.it;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterEach;

import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class RunAndCheckWithAgentMojoTestBase extends RunAndCheckMojoTestBase {

    protected RunningInvoker runningAgent;
    protected File agentDir;

    @AfterEach
    @Override
    public void cleanup() throws IOException {
        try {
            super.cleanup();
        } finally {
            if (runningAgent != null) {
                runningAgent.stop();
            }
            DevModeTestUtils.awaitUntilServerDown();
        }
    }

    @Override
    protected void runAndCheck(String... options) throws FileNotFoundException, MavenInvocationException {
        super.runAndCheck(options);

        try {
            //file granularity is 1s on some platforms
            //we check for changes since agent start
            //so we need to make sure we start at least 1s after copying the files
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runningAgent = new RunningInvoker(agentDir, false);
        runningAgent.execute(Arrays.asList("compile", "quarkus:remote-dev"), Collections.emptyMap());

        try {
            Thread.sleep(1000);
            await().pollDelay(100, TimeUnit.MILLISECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .atMost(1, TimeUnit.MINUTES)
                    .until(() -> runningAgent.log().contains("Connected to remote server"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected Supplier<String> getBrokenReason() {
        return () -> {
            if (running != null && !running.getResult().getProcess().isAlive()) {
                try {
                    return running.log();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (runningAgent != null && !runningAgent.getResult().getProcess().isAlive()) {
                try {
                    return runningAgent.log();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        };
    }
}
