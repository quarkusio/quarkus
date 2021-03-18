package io.quarkus.maven.it;

import static io.quarkus.devtools.testing.SnapshotTesting.assertThatDirectoryTreeMatchSnapshots;
import static io.quarkus.devtools.testing.SnapshotTesting.assertThatMatchSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@DisableForNative
public class CreateExtensionMojoIT extends QuarkusPlatformAwareMojoTestBase {

    private Invoker invoker;
    private File testDir;

    @Test
    public void testCreateCoreExtension(TestInfo testInfo) throws Throwable {
        testDir = initProject("projects/create-extension-quarkus-core", "output/create-extension-quarkus-core");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("extensionId", "my-ext");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        final Path testDirPath = testDir.toPath();
        assertThatDirectoryTreeMatchSnapshots(testInfo, testDirPath)
                .contains(
                        "extensions/my-ext/pom.xml",
                        "extensions/my-ext/runtime/src/main/resources/META-INF/quarkus-extension.yaml",
                        "extensions/my-ext/deployment/src/main/java/org/acme/my/ext/deployment/MyExtProcessor.java",
                        "integration-tests/my-ext/pom.xml",
                        "integration-tests/my-ext/src/test/java/org/acme/my/ext/it/MyExtResourceTest.java");
        assertThatMatchSnapshot(testInfo, testDirPath, "extensions/my-ext/pom.xml");
        assertThatMatchSnapshot(testInfo, testDirPath,
                "extensions/my-ext/runtime/src/main/resources/META-INF/quarkus-extension.yaml");
        assertThatMatchSnapshot(testInfo, testDirPath, "bom/application/pom.xml");
        assertThatMatchSnapshot(testInfo, testDirPath, "integration-tests/pom.xml");
        assertThatMatchSnapshot(testInfo, testDirPath, "extensions/pom.xml");
    }

    @Test
    public void testCreateCoreExtensionFromExtensionsDir(TestInfo testInfo) throws Throwable {
        testDir = initProject("projects/create-extension-quarkus-core", "output/create-extension-quarkus-core-extensions-dir");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir.toPath().resolve("extensions/").toFile());

        Properties properties = new Properties();
        properties.put("extensionId", "quarkus-my-ext");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        final Path testDirPath = testDir.toPath();
        assertThatDirectoryTreeMatchSnapshots(testInfo, testDirPath)
                .contains(
                        "extensions/my-ext/pom.xml",
                        "extensions/my-ext/deployment/src/main/java/org/acme/my/ext/deployment/MyExtProcessor.java",
                        "integration-tests/my-ext/pom.xml",
                        "integration-tests/my-ext/src/test/java/org/acme/my/ext/it/MyExtResourceTest.java");
        assertThatMatchSnapshot(testInfo, testDirPath, "extensions/my-ext/pom.xml");
        assertThatMatchSnapshot(testInfo, testDirPath,
                "extensions/my-ext/runtime/src/main/resources/META-INF/quarkus-extension.yaml");
        assertThatMatchSnapshot(testInfo, testDirPath, "bom/application/pom.xml");
        assertThatMatchSnapshot(testInfo, testDirPath, "integration-tests/pom.xml");
        assertThatMatchSnapshot(testInfo, testDirPath, "extensions/pom.xml");
    }

    @Test
    public void testCreateQuarkiverseExtension(TestInfo testInfo) throws Throwable {
        testDir = initEmptyProject("output/create-quarkiverse-extension");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("groupId", "io.quarkiverse.my-quarki-ext");
        properties.put("extensionId", "my-quarki-ext");
        properties.put("quarkusVersion", "1.10.5.Final");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        final Path testDirPath = testDir.toPath();
        assertThatDirectoryTreeMatchSnapshots(testInfo, testDirPath)
                .contains(
                        "quarkus-my-quarki-ext/pom.xml",
                        "quarkus-my-quarki-ext/deployment/src/main/java/io/quarkiverse/my/quarki/ext/deployment/MyQuarkiExtProcessor.java",
                        "quarkus-my-quarki-ext/integration-tests/pom.xml",
                        "quarkus-my-quarki-ext/integration-tests/src/test/java/io/quarkiverse/my/quarki/ext/it/MyQuarkiExtResourceTest.java");
        assertThatMatchSnapshot(testInfo, testDirPath, "quarkus-my-quarki-ext/pom.xml");
        assertThatMatchSnapshot(testInfo, testDirPath, "quarkus-my-quarki-ext/runtime/pom.xml");
    }

    @Test
    public void testCreateStandaloneExtension(TestInfo testInfo) throws Throwable {
        testDir = initEmptyProject("output/create-standalone-extension");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("groupId", "io.standalone");
        properties.put("extensionId", "my-own-ext");
        properties.put("namespaceId", "my-org-");
        properties.put("quarkusVersion", "1.10.5.Final");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        final Path testDirPath = testDir.toPath();
        assertThatDirectoryTreeMatchSnapshots(testInfo, testDirPath)
                .contains(
                        "my-org-my-own-ext/pom.xml",
                        "my-org-my-own-ext/deployment/src/main/java/io/standalone/my/own/ext/deployment/MyOwnExtProcessor.java",
                        "my-org-my-own-ext/integration-tests/pom.xml",
                        "my-org-my-own-ext/integration-tests/src/test/java/io/standalone/my/own/ext/it/MyOwnExtResourceTest.java");
        assertThatMatchSnapshot(testInfo, testDirPath, "my-org-my-own-ext/pom.xml");
        assertThatMatchSnapshot(testInfo, testDirPath, "my-org-my-own-ext/runtime/pom.xml");
    }

    private InvocationResult setup(Properties params)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                getMavenPluginGroupId() + ":" + getMavenPluginArtifactId() + ":" + getMavenPluginVersion()
                        + ":create-extension"));
        request.setDebug(false);
        request.setShowErrors(true);
        request.setProperties(params);
        File log = new File(testDir.getParent(), "build-create-extension-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        return invoker.execute(request);
    }

}
