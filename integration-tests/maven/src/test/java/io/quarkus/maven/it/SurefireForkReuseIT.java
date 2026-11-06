package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@DisableForNative
public class SurefireForkReuseIT extends RunAndCheckMojoTestBase {

    private static final String STARTUP_MARKER = "QUARKUS_FORK_STARTUP:";

    @Test
    public void reusesQuarkusApplicationWithinEachFork() throws MavenInvocationException, IOException {
        testDir = initProject("projects/surefire-fork-reuse", "projects/surefire-fork-reuse-processed");
        running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running.execute(List.of("test"), Collections.emptyMap());
        await().atMost(5, TimeUnit.MINUTES)
                .until(() -> result.getProcess() != null && !result.getProcess().isAlive());

        assertThat(running.getResult().getExitCode()).isZero();
        String processLog = running.log();
        assertThat(processLog).containsIgnoringCase("BUILD SUCCESS");

        List<String> startupLines = processLog.lines()
                .filter(line -> line.contains(STARTUP_MARKER))
                .toList();
        assertThat(startupLines)
                .as("one Quarkus application should start in each of the two Maven forks")
                .hasSize(2);
        assertThat(startupLines)
                .extracting(line -> line.substring(line.indexOf(STARTUP_MARKER) + STARTUP_MARKER.length()).trim())
                .doesNotHaveDuplicates();
    }
}
