package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeClient;

@DisableForNative
public class JLinkTreeShakeIT extends MojoTestBase {

    private final DevModeClient devModeClient = new DevModeClient();

    @Test
    void testJLinkImageWithTreeShake() throws Exception {
        File testDir = initProject("projects/jlink-tree-shake");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running.execute(
                List.of("package", "-DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor(10, TimeUnit.MINUTES))
                .as("Maven build timed out").isTrue();
        assertThat(result.getProcess().exitValue()).isEqualTo(0);
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");

        // verify module tree-shaking ran and removed modules
        assertThat(running.log()).contains("Module tree-shaking removed");

        running.stop();

        // launch the image and verify it serves HTTP requests
        Path imageDir = testDir.toPath().resolve("target/jlink-output/image");
        assertThat(imageDir).isDirectory();
        String launcherName = OS.current() == OS.WINDOWS ? "my-app.bat" : "my-app";
        Path launcher = imageDir.resolve("bin").resolve(launcherName);
        File outputLog = new File(testDir, "target/output.log");
        outputLog.createNewFile();

        ProcessBuilder pb = new ProcessBuilder(launcher.toAbsolutePath().toString());
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputLog));
        pb.redirectError(ProcessBuilder.Redirect.appendTo(outputLog));
        Process process = pb.start();
        try {
            await().pollDelay(1, TimeUnit.SECONDS)
                    .atMost(TestUtils.getDefaultTimeout(), TimeUnit.MINUTES)
                    .until(() -> devModeClient.getHttpResponse("/hello", 200));

            assertThat(devModeClient.getHttpResponse("/hello")).isEqualTo("hello");
        } catch (Exception e) {
            String logs = Files.readString(outputLog.toPath());
            System.out.println("####### APP LOG DUMP ON FAILURE ######");
            System.out.println(logs);
            System.out.println("######################################");
            throw e;
        } finally {
            process.destroy();
            process.waitFor(10, TimeUnit.SECONDS);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
