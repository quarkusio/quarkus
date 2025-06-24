package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeClient;
import io.quarkus.utilities.JavaBinFinder;

public class RunAndCheckWithAgentMojoTestBase extends MojoTestBase {

    protected RunningInvoker runningAgent;
    private Process runningRemote;
    protected File agentDir;
    protected File testDir;

    protected DevModeClient devModeClient = new DevModeClient();

    @AfterEach
    public void cleanup() throws IOException {
        try {
            if (runningRemote != null) {
                runningRemote.destroyForcibly().waitFor();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (runningAgent != null) {
                runningAgent.stop();
            }
            devModeClient.awaitUntilServerDown();
        }
    }

    protected void runAndCheck(String... options) throws FileNotFoundException, MavenInvocationException {
        try {
            RunningInvoker running = new RunningInvoker(testDir, false);

            MavenProcessInvocationResult result = running
                    .execute(Arrays.asList("package", "-DskipTests", "-Dquarkus.analytics.disabled=true"),
                            Collections.emptyMap());

            await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
            assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
            running.stop();

            Path jar = testDir.toPath().toAbsolutePath()
                    .resolve(Paths.get("target/quarkus-app/quarkus-run.jar"));
            Assertions.assertTrue(Files.exists(jar));
            File output = new File(testDir, "target/output.log");
            output.createNewFile();

            runningRemote = doLaunch(jar, output);

            // Wait until server up
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/", 200));

            runningAgent = new RunningInvoker(agentDir, false);
            runningAgent.execute(Arrays.asList("compile", "quarkus:remote-dev", "-Dquarkus.analytics.disabled=true"),
                    Collections.emptyMap());

            Thread.sleep(1000);
            await().pollDelay(100, TimeUnit.MILLISECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .atMost(1, TimeUnit.MINUTES)
                    .until(() -> runningAgent.log().contains("Connected to remote server"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Process doLaunch(Path jar, File output) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-jar");
        commands.add(jar.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        processBuilder.environment().put("QUARKUS_LAUNCH_DEVMODE", "true");
        return processBuilder.start();
    }
}
