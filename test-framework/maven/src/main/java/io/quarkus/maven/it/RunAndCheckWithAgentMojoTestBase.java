package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterEach;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeClient;
import io.quarkus.utilities.JavaBinFinder;

public class RunAndCheckWithAgentMojoTestBase extends MojoTestBase {

    private static final String MUTABLE_JAR_TYPE_ARG = "-Dquarkus.package.jar.type=mutable-jar";
    private static final String LIVE_RELOAD_PWD_ARG = "-Dquarkus.live-reload.password=secret";
    private static final String LIVE_RELOAD_URL_ARG = "-Dquarkus.live-reload.url=http://localhost:8080";
    private static final String QUARKUS_ANALYTICS_DISABLED_TRUE = "-Dquarkus.analytics.disabled=true";

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

    protected void runAndCheck() throws FileNotFoundException, MavenInvocationException {
        runAndCheckModule(null);
    }

    protected void runAndCheckModule(String module) {
        try {
            RunningInvoker running = new RunningInvoker(testDir, false);

            MavenProcessInvocationResult result = running
                    .execute(
                            List.of("package", "-DskipTests", MUTABLE_JAR_TYPE_ARG, QUARKUS_ANALYTICS_DISABLED_TRUE),
                            Map.of());

            await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
            assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
            running.stop();

            Path runnerTargetDir = testDir.toPath().toAbsolutePath();
            if (module != null) {
                runnerTargetDir = runnerTargetDir.resolve(module);
            }
            runnerTargetDir = runnerTargetDir.resolve("target");

            Path jar = runnerTargetDir.resolve("quarkus-app/quarkus-run.jar");
            assertThat(jar).exists();
            File output = runnerTargetDir.resolve("output.log").toFile();
            output.createNewFile();

            runningRemote = doLaunch(jar, output);

            // Wait until server up
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/", 200));

            runningAgent = new RunningInvoker(agentDir, false);
            runningAgent.execute(
                    List.of("compile", "quarkus:remote-dev", MUTABLE_JAR_TYPE_ARG, LIVE_RELOAD_PWD_ARG,
                            LIVE_RELOAD_URL_ARG, QUARKUS_ANALYTICS_DISABLED_TRUE),
                    Map.of());

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
        final String[] commands = {
                JavaBinFinder.findBin(),
                LIVE_RELOAD_PWD_ARG,
                "-jar",
                jar.toString()
        };
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        processBuilder.environment().put("QUARKUS_LAUNCH_DEVMODE", "true");
        return processBuilder.start();
    }
}
