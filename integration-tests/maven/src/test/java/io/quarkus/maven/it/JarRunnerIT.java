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
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.runner.ReAugmentEntryPoint;
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

        Process process = doLaunch(jar, output);
        try {
            // Wait until server up
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly();
            assertResourceReadingFromClassPathWorksCorrectly("");
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

        Process process = doLaunch(jar, output);
        try {
            // Wait until server up
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly();
            assertResourceReadingFromClassPathWorksCorrectly("");
        } finally {
            process.destroy();
        }

    }

    @Test
    public void testThatMutableFastJarWorks() throws MavenInvocationException, IOException, InterruptedException {
        File testDir = initProject("projects/classic", "projects/project-classic-console-output-mutable-fast-jar");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running
                .execute(Arrays.asList("package", "-DskipTests", "-Dquarkus.package.type=fast-jar",
                        "-Dquarkus.package.mutable-application=true"), Collections.emptyMap());

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

        Process process = doLaunch(jar, output);
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

        //now reaugment
        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-Dquarkus.http.root-path=/moved");
        commands.add("-cp");
        commands.add(jar.toString());
        commands.add(ReAugmentEntryPoint.class.getName());
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        Assertions.assertEquals(0, processBuilder.start().waitFor());

        process = doLaunch(jar, output);
        try {
            // Wait until server up
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/moved/app/hello/package", 200));

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly("/moved");

            assertResourceReadingFromClassPathWorksCorrectly("/moved");
        } finally {
            process.destroy();
        }
    }

    /**
     * Tests that quarkus.arc.exclude-dependency.* can be used for modules in a multimodule project
     */
    @Test
    public void testArcExcludeDependencyOnLocalModule() throws Exception {
        File testDir = initProject("projects/arc-exclude-dependencies");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running.execute(Arrays.asList("package", "-DskipTests"), Collections.emptyMap());
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        File targetDir = new File(testDir.getAbsoluteFile(), "runner" + File.separator + "target");
        Path jar = targetDir.toPath().toAbsolutePath()
                .resolve(Paths.get("acme-1.0-SNAPSHOT-runner.jar"));
        File output = new File(targetDir, "output.log");
        output.createNewFile();

        Process process = doLaunch(jar, output);
        try {
            // Wait until server up
            AtomicReference<String> response = new AtomicReference<>();
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES).until(() -> {
                        String ret = DevModeTestUtils.getHttpResponse("/hello", true);
                        response.set(ret);
                        return ret.contains("hello:");
                    });

            // Test that bean is not resolvable
            assertThat(response.get()).containsIgnoringCase("hello:false");
        } finally {
            process.destroy();
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
        return processBuilder.start();
    }

    static void assertResourceReadingFromClassPathWorksCorrectly(String path) {
        try {
            URL url = new URL("http://localhost:8080" + path + "/app/classpathResources");
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
