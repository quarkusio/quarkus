package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.utilities.JavaBinFinder;

public class JarRunnerIT extends MojoTestBase {

    @Test
    public void testThatJarRunnerConsoleOutputWorksCorrectly() throws MavenInvocationException, IOException {
        File testDir = initProject("projects/classic", "projects/project-classic-console-output");
        RunningInvoker running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running.execute(Arrays.asList("package", "-DskipTests"), Collections.emptyMap());
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        assertThat(running.log()).containsIgnoringCase("BUILD SUCCESS");
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath().resolve(Paths.get("target/acme-1.0-SNAPSHOT-runner.jar"));
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
        // Wait until server up
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/hello/package", 200));

        String logs = FileUtils.readFileToString(output, "UTF-8");

        assertThatOutputWorksCorrectly(logs);

        process.destroy();
    }

    @Test
    public void testCommandLineArgsInjection() throws MavenInvocationException, IOException {
        File testDir = initProject("projects/classic", "projects/project-classic-injected-cli-args");
        File source = new File(testDir, "src/main/java/org/acme/InjectedCliArgsResource.java");
        String injectedCliArgsResource = "package org.acme;\n" +
                "\n" +
                "import javax.ws.rs.GET;\n" +
                "import javax.ws.rs.Path;\n" +
                "import java.util.List;\n" +
                "import javax.ws.rs.Produces;\n" +
                "import javax.inject.Inject;\n" +
                "import io.quarkus.arc.runtime.Parameters;\n" +
                "import javax.ws.rs.core.MediaType;\n" +
                "\n" +
                "@Path(\"/args\")\n" +
                "public class InjectedCliArgsResource {\n" +
                "    @Inject @Parameters List<String> params;\n" +
                "    @GET\n" +
                "    @Produces(MediaType.APPLICATION_JSON)\n" +
                "    public String params() {\n" +
                "        return params.toString();\n" +
                "    }\n" +
                "}\n";
        FileUtils.write(source, injectedCliArgsResource, Charset.forName("UTF-8"));

        RunningInvoker running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Arrays.asList("package", "-DskipTests"), Collections.emptyMap());
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        running.stop();

        Path jar = testDir.toPath().toAbsolutePath().resolve(Paths.get("target/acme-1.0-SNAPSHOT-runner.jar"));
        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-jar");
        commands.add(jar.toString());
        commands.add("arg1");
        commands.add("arg2");
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        Process process = processBuilder.start();

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse("/app/args", 200));

        String args = getHttpResponse("/app/args");
        assertThat(args).contains("arg1");
        assertThat(args).contains("arg2");

        process.destroy();
    }
}
