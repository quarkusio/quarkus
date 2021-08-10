package io.quarkus.maven.it;

import static io.quarkus.maven.it.ApplicationNameAndVersionTestUtil.assertApplicationPropertiesSetCorrectly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.awaitility.core.ConditionTimeoutException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import io.quarkus.deployment.pkg.steps.JarResultBuildStep;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeTestUtils;
import io.quarkus.utilities.JavaBinFinder;

@DisableForNative
public class JarRunnerIT extends MojoTestBase {

    /**
     * Tests that a Quarkus project builds fine if the project is hosted in a directory
     * path that contains non-ASCII characters
     *
     * @throws MavenInvocationException
     * @throws IOException
     * @see <a href="https://github.com/quarkusio/quarkus/issues/11511"/>
     */
    @Test
    public void testNonAsciiDir() throws Exception {
        final File testDir = initProject("projects/classic", "projects/ěščřžýáíéůú");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        final MavenProcessInvocationResult result = running.execute(Arrays.asList("install", "-DskipTests"),
                Collections.emptyMap());
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        File output = new File(testDir, "target/output.log");
        output.createNewFile();

        Process process = doLaunch(new File(testDir, "target/quarkus-app"), Paths.get("quarkus-run.jar"), output,
                Collections.emptyList()).start();
        try {
            // Wait until server up
            dumpFileContentOnFailure(() -> {
                await().pollDelay(1, TimeUnit.SECONDS)
                        .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));
                return null;
            }, output, ConditionTimeoutException.class);
        } finally {
            process.destroy();
        }

    }

    @Test
    public void testThatJarRunnerConsoleOutputWorksCorrectly() throws MavenInvocationException, IOException {
        File testDir = initProject("projects/classic", "projects/project-classic-console-output");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running.execute(Arrays.asList("package", "-DskipTests"), Collections.emptyMap());
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/quarkus-app/quarkus-run.jar"));
        File output = new File(testDir, "target/output.log");
        output.createNewFile();

        Process process = doLaunch(jar, output).start();
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
            assertUsingProtectionDomainWorksCorrectly("");
        } finally {
            process.destroy();
        }

    }

    @Test
    public void testPlatformPropertiesOverridenInApplicationProperties() throws Exception {
        final File testDir = initProject("projects/platform-properties-overrides",
                "projects/platform-props-overriden-in-app-props");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        final MavenProcessInvocationResult result = running.execute(Arrays.asList("install"),
                Collections.emptyMap());
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        File output = new File(testDir, "app/target/output.log");
        output.createNewFile();

        Process process = doLaunch(new File(testDir, "app/target/quarkus-app"), Paths.get("quarkus-run.jar"), output,
                Collections.emptyList()).start();
        try {
            Assertions.assertEquals("builder-image is customized", DevModeTestUtils.getHttpResponse("/hello"));
        } finally {
            process.destroy();
        }
    }

    @Test
    public void testPlatformPropertiesOverridenOnCommandLine() throws Exception {
        final File testDir = initProject("projects/platform-properties-overrides",
                "projects/platform-props-overriden-on-cmd-line");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        final MavenProcessInvocationResult result = running.execute(
                Arrays.asList("install -Dquarkus.native.builder-image=commandline -DskipTests"),
                Collections.emptyMap());
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        File output = new File(testDir, "app/target/output.log");
        output.createNewFile();

        Process process = doLaunch(new File(testDir, "app/target/quarkus-app"), Paths.get("quarkus-run.jar"), output,
                Collections.emptyList()).start();
        try {
            Assertions.assertEquals("builder-image is commandline", DevModeTestUtils.getHttpResponse("/hello"));
        } finally {
            process.destroy();
        }
    }

    @Test
    public void testThatFastJarFormatWorks() throws Exception {
        assertThatFastJarFormatWorks(null);
    }

    @Test
    public void testThatFastJarCustomOutputDirFormatWorks() throws Exception {
        assertThatFastJarFormatWorks("custom");
    }

    @Test
    public void testThatMutableFastJarWorks() throws Exception {
        assertThatMutableFastJarWorks("providers", "providers");
    }

    @Test
    public void testThatMutableFastJarWorksProvidersDirOutsideOutputDir() throws Exception {
        assertThatMutableFastJarWorks("outsidedir", ".." + File.separator + "providers");
    }

    @Test
    public void testThatLegacyJarFormatWorks() throws Exception {
        File testDir = initProject("projects/rr-with-json-logging", "projects/rr-with-json-logging-legacy-jar");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running
                .execute(Arrays.asList("package",
                        "-DskipTests",
                        "-Dquarkus.package.type=legacy-jar"), Collections.emptyMap());

        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target",
                        JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME,
                        "quarkus-run.jar"));
        Assertions.assertFalse(Files.exists(jar));

        jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/acme-1.0-SNAPSHOT-runner.jar"));
        Assertions.assertTrue(Files.exists(jar));

        Properties quarkusArtifactProperties = new Properties();
        quarkusArtifactProperties
                .load(new FileInputStream(testDir.toPath().resolve("target").resolve("quarkus-artifact.properties").toFile()));
        Assertions.assertEquals("jar", quarkusArtifactProperties.get("type"));
        Assertions.assertEquals("acme-1.0-SNAPSHOT-runner.jar", quarkusArtifactProperties.get("path"));

        File output = new File(testDir, "target/output.log");
        output.createNewFile();

        Properties properties = new Properties();
        properties
                .load(new FileInputStream(testDir.toPath().resolve("target").resolve("quarkus-artifact.properties").toFile()));
        Assertions.assertEquals("jar", properties.get("type"));
        Assertions.assertEquals("acme-1.0-SNAPSHOT-runner.jar", properties.get("path"));

        Process process = doLaunch(jar, output).start();
        try {
            // Wait until server up
            dumpFileContentOnFailure(() -> {
                await()
                        .pollDelay(1, TimeUnit.SECONDS)
                        .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));
                return null;
            }, output, ConditionTimeoutException.class);

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThat(logs).isNotEmpty().contains("resteasy-reactive");

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly();
            assertResourceReadingFromClassPathWorksCorrectly("");
            assertUsingProtectionDomainWorksCorrectly("");
        } finally {
            process.destroy();
        }
    }

    private void assertThatMutableFastJarWorks(String targetDirSuffix, String providersDir) throws Exception {
        File testDir = initProject("projects/classic",
                "projects/project-classic-console-output-mutable-fast-jar" + targetDirSuffix);
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running
                .execute(Arrays.asList("package", "-DskipTests", "-Dquarkus.package.type=mutable-jar",
                        "-Dquarkus.package.user-providers-directory=" + providersDir), Collections.emptyMap());

        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/acme-1.0-SNAPSHOT-runner.jar"));
        Assertions.assertFalse(Files.exists(jar));

        jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/quarkus-app/quarkus-run.jar"));
        Assertions.assertTrue(Files.exists(jar));

        Path providers = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/quarkus-app/" + providersDir));
        Assertions.assertTrue(Files.exists(providers));

        File output = new File(testDir, "target/output.log");
        output.createNewFile();

        Process process = doLaunch(jar, output).start();
        try {
            // Wait until server up
            dumpFileContentOnFailure(() -> {
                await()
                        .pollDelay(1, TimeUnit.SECONDS)
                        .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));
                return null;
            }, output, ConditionTimeoutException.class);
            performRequest("/app/added", 404);

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly();
        } finally {
            process.destroy();
        }

        //add a user jar to the providers dir, and make sure it is picked up in re-augmentation
        File addedJar = providers.resolve("added.jar").toFile();
        ShrinkWrap.create(JavaArchive.class).addClass(AddedRestEndpoint.class)
                .as(ZipExporter.class).exportTo(addedJar);

        //now reaugment
        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-Dquarkus.http.root-path=/moved");
        commands.add("-Dquarkus.launch.rebuild=true");
        commands.add("-jar");
        commands.add(jar.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        Assertions.assertEquals(0, processBuilder.start().waitFor());

        process = doLaunch(jar, output).start();
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
            assertUsingProtectionDomainWorksCorrectly("/moved");
            Assertions.assertEquals("added endpoint", performRequest("/moved/app/added", 200));
        } finally {
            process.destroy();
        }

        //now remove it, and make sure everything is back to the way it was

        //add a user jar to the providers dir, and make sure it is picked up in re-augmentation
        addedJar.delete();

        //now reaugment
        commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-Dquarkus.http.root-path=/anothermove");
        commands.add("-Dquarkus.launch.rebuild=true");
        commands.add("-jar");
        commands.add(jar.toString());
        processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        Assertions.assertEquals(0, processBuilder.start().waitFor());

        process = doLaunch(jar, output).start();
        try {
            // Wait until server up
            await()
                    .pollDelay(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES)
                    .until(() -> DevModeTestUtils.getHttpResponse("/anothermove/app/hello/package", 200));

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly("/anothermove");

            assertResourceReadingFromClassPathWorksCorrectly("/anothermove");
            assertUsingProtectionDomainWorksCorrectly("/anothermove");
            performRequest("/anothermove/app/added", 404);
        } finally {
            process.destroy();
        }

    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_11)
    public void testThatAppCDSAreUsable() throws Exception {
        File testDir = initProject("projects/classic", "projects/project-classic-console-output-appcds");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running
                .execute(Arrays.asList("package", "-DskipTests", "-Dquarkus.package.create-appcds=true"),
                        Collections.emptyMap());

        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/quarkus-app/quarkus-run.jar"));
        File output = new File(testDir, "target/output.log");
        output.createNewFile();

        // by using '-Xshare:on' we ensure that the JVM will fail if for any reason is cannot use the AppCDS
        // '-Xlog:class+path=info' will print diagnostic information that is useful for debugging if something goes wrong
        Process process = doLaunch(jar.getFileName(), output,
                Arrays.asList("-XX:SharedArchiveFile=app-cds.jsa", "-Xshare:on", "-Xlog:class+path=info"))
                        .directory(jar.getParent().toFile()).start();
        try {
            // Wait until server up
            dumpFileContentOnFailure(() -> {
                await()
                        .pollDelay(1, TimeUnit.SECONDS)
                        .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));
                return null;
            }, output, ConditionTimeoutException.class);

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThatOutputWorksCorrectly(logs);
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
                .resolve(Paths.get("quarkus-app/quarkus-run.jar"));
        File output = new File(targetDir, "output.log");
        output.createNewFile();

        Process process = doLaunch(jar, output).start();
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

    private ProcessBuilder doLaunch(Path jar, File output) throws IOException {
        return doLaunch(null, jar, output, Collections.emptyList());
    }

    private ProcessBuilder doLaunch(Path jar, File output, Collection<String> vmArgs) throws IOException {
        return doLaunch(null, jar, output, vmArgs);
    }

    static ProcessBuilder doLaunch(final File workingDir, final Path jar, File output, Collection<String> vmArgs)
            throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.addAll(vmArgs);
        commands.add("-jar");
        commands.add(jar.toString());
        // write out the command used to launch the process, into the log file
        Files.write(output.toPath(), commands);
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        if (workingDir != null) {
            processBuilder.directory(workingDir);
        }
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(output));
        processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(output));
        return processBuilder;
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

    static void assertUsingProtectionDomainWorksCorrectly(String path) {
        try {
            URL url = new URL("http://localhost:8080" + path + "/app/protectionDomain");
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
            failProtectionDomain();
        }
    }

    static String performRequest(String path, int expectedCode) {
        try {
            URL url = new URL("http://localhost:8080" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.2
            connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
            if (connection.getResponseCode() != expectedCode) {
                Assertions.fail("Invalid response code " + connection.getResponseCode());
            }
            return new String(IoUtil.readBytes(connection.getInputStream()), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            if (expectedCode == 404) {
                return "";
            }
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void failResourcesFromTheClasspath() {
        fail("Failed to assert that the application properly reads resources from the classpath");
    }

    private static void failProtectionDomain() {
        fail("Failed to assert that the use of ProtectionDomain works correctly");
    }

    /**
     * Calls the {@link Callable} and if that call results in an exception of type {@code failureType} then,
     * prints out the contents of the passed {@code logFile} and throws back the original exception
     *
     * @param operation The operation to invoke
     * @param logFile The file which contains the logs generated when the operation was running
     * @param failureType The type of failure that should trigger printing out the logs
     * @throws Exception
     */
    private void dumpFileContentOnFailure(final Callable<Void> operation, final File logFile,
            final Class<? extends Throwable> failureType) throws Exception {
        try {
            operation.call();
        } catch (Throwable t) {
            if (failureType != null && failureType.isInstance(t)) {
                final String logs = FileUtils.readFileToString(logFile, "UTF-8");
                System.out.println("####### LOG DUMP ON FAILURE (start) ######");
                System.out.println("Dumping logs that were generated in " + logFile + " for an operation that resulted in "
                        + t.getClass().getName() + ":");
                System.out.println();
                System.out.println(logs);
                System.out.println("####### LOG DUMP ON FAILURE (end) ######");
            }
            throw t;
        }
    }

    private void assertThatFastJarFormatWorks(String outputDir) throws Exception {
        File testDir = initProject("projects/rr-with-json-logging", "projects/rr-with-json-logging" + outputDir);
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running
                .execute(Arrays.asList("package",
                        "-DskipTests",
                        "-Dquarkus.package.type=fast-jar",
                        outputDir == null ? "" : "-Dquarkus.package.output-directory=" + outputDir), Collections.emptyMap());

        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target/acme-1.0-SNAPSHOT-runner.jar"));
        Assertions.assertFalse(Files.exists(jar));

        jar = testDir.toPath().toAbsolutePath()
                .resolve(Paths.get("target",
                        outputDir == null ? JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME : outputDir,
                        "quarkus-run.jar"));
        Assertions.assertTrue(Files.exists(jar));
        File output = new File(testDir, "target/output.log");
        output.createNewFile();

        Properties properties = new Properties();
        properties
                .load(new FileInputStream(testDir.toPath().resolve("target").resolve("quarkus-artifact.properties").toFile()));
        Assertions.assertEquals("jar", properties.get("type"));
        Assertions.assertTrue(properties.get("path").toString().startsWith(outputDir == null ? "quarkus-app" : outputDir));
        Assertions.assertTrue(properties.get("path").toString().endsWith("quarkus-run.jar"));

        Process process = doLaunch(jar, output).start();
        try {
            // Wait until server up
            dumpFileContentOnFailure(() -> {
                await()
                        .pollDelay(1, TimeUnit.SECONDS)
                        .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/package", 200));
                return null;
            }, output, ConditionTimeoutException.class);

            String logs = FileUtils.readFileToString(output, "UTF-8");

            assertThat(logs).isNotEmpty().contains("resteasy-reactive");

            // test that the application name and version are properly set
            assertApplicationPropertiesSetCorrectly();
            assertResourceReadingFromClassPathWorksCorrectly("");
            assertUsingProtectionDomainWorksCorrectly("");
        } finally {
            process.destroy();
        }

    }
}
