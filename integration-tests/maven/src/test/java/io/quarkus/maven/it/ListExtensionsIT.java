package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.output.TeeOutputStream;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.jupiter.api.Test;

@DisableForNative
class ListExtensionsIT extends QuarkusPlatformAwareMojoTestBase {

    private static final String VERTX_ARTIFACT_ID = "quarkus-vertx";
    private static final String PROJECT_SOURCE_DIR = "projects/classic";
    private File testDir;
    private Invoker invoker;

    @Test
    void testListExtensions() throws MavenInvocationException, IOException {
        testDir = initProject(PROJECT_SOURCE_DIR, "projects/testListExtensions");
        invoker = initInvoker(testDir);

        List<String> outputLogLines = listExtensions();

        assertThat(outputLogLines).anyMatch(line -> line.contains(VERTX_ARTIFACT_ID));
    }

    @Test
    void testListExtensionsWithManagedDependencyWithoutScope() throws MavenInvocationException, IOException {
        testDir = initProject(PROJECT_SOURCE_DIR, "projects/testListExtensionsWithManagedDependencyWithoutScope");
        invoker = initInvoker(testDir);
        // Edit the pom.xml.
        File source = new File(testDir, "pom.xml");
        filter(source, Collections.singletonMap("<!-- insert managed dependencies here -->",
                "      <dependency>\n" +
                        "        <groupId>org.assertj</groupId>\n" +
                        "        <artifactId>assertj-core</artifactId>\n" +
                        "        <version>3.16.1</version>\n" +
                        "      </dependency>"));

        List<String> outputLogLines = listExtensions();

        assertThat(outputLogLines).anyMatch(line -> line.contains(VERTX_ARTIFACT_ID));
    }

    private List<String> listExtensions()
            throws MavenInvocationException, IOException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                getMavenPluginGroupId() + ":" + getMavenPluginArtifactId() + ":" + getMavenPluginVersion()
                        + ":list-extensions"));
        getEnv().forEach(request::addShellEnvironment);

        File outputLog = new File(testDir, "output.log");
        InvocationOutputHandler outputHandler = new PrintStreamHandler(
                new PrintStream(new TeeOutputStream(System.out, Files.newOutputStream(outputLog.toPath())), true, "UTF-8"),
                true);
        invoker.setOutputHandler(outputHandler);

        File invokerLog = new File(testDir, "invoker.log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(invokerLog), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);

        invoker.execute(request);
        return Files.readAllLines(outputLog.toPath());
    }
}
