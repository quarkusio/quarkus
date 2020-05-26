package io.quarkus.maven.it;

import static io.quarkus.maven.it.ApplicationNameAndVersionTestUtil.assertApplicationPropertiesSetCorrectly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeTestUtils;
import io.quarkus.utilities.JavaBinFinder;

@DisableForNative
public class JarRunnerIT extends MojoTestBase {

    @Test
    public void testThatJarRunnerConsoleOutputWorksCorrectly() throws MavenInvocationException, IOException {
        File testDir = initProject("projects/classic", "projects/project-classic-console-output");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running.execute(Arrays.asList("package", "-DskipTests"), Collections.emptyMap());
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/acme-1.0-SNAPSHOT-runner.jar"));
        File output = new File(testDir, "target/output.log");
        output.createNewFile();

        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-jar");
        commands.add(jar.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        Process process = processBuilder.start();
        try {
            // Wait until server up
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly();
        } finally {
            process.destroy();
        }

    }

    @Test
    public void testThatFastJarFormatWorks() throws MavenInvocationException, IOException {
        File testDir = initProject("projects/classic", "projects/project-classic-console-output-fast-jar");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running
                .execute(Arrays.asList("package", "-DskipTests", "-Dquarkus.package.type=fast-jar"), Collections.emptyMap());

        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/acme-1.0-SNAPSHOT-runner.jar"));
        Assertions.assertFalse(Files.exists(jar));

        jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/acme-1.0-SNAPSHOT/quarkus-run.jar"));
        Assertions.assertTrue(Files.exists(jar));
        File output = new File(testDir, "target/output.log");
        output.createNewFile();

        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-jar");
        commands.add(jar.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        Process process = processBuilder.start();
        try {
            // Wait until server up
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly();

            assertResourceReadingFromClassPathWorksCorrectly();
        } finally {
            process.destroy();
        }

    }

    static void assertResourceReadingFromClassPathWorksCorrectly() {
        try {
            URL url = new URL("http://localhost:8080/app/classpathResources");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.2
            connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                failResourcesFromTheClasspath();
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String output = br.readLine();
                assertThat(output).isEqualTo("success");
            }
        } catch (IOException e) {
            failResourcesFromTheClasspath();
        }
    }

    private static void failResourcesFromTheClasspath() {
        fail("Failed to assert that the application properly reads resources from the classpath");
    }
}
