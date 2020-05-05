package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.assertions.SetupVerifier;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.platform.tools.ToolsConstants;

@DisableForNative
public class SetupIT extends QuarkusPlatformAwareMojoTestBase {

    private Invoker invoker;
    private RunningInvoker running;
    private File testDir;

    @Test
    public void testSetupOnExistingPom() throws Exception {
        testDir = new File("target/test-classes", "projects/setup-on-existing-pom");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        setup(new Properties());

        File pomFile = new File(testDir, "pom.xml");
        assertThat(pomFile).isFile();
        SetupVerifier.verifySetup(pomFile);
    }

    @Test
    public void testSetupOnMinPom() throws Exception {
        testDir = new File("target/test-classes", "projects/setup-on-min-pom");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        setup(new Properties());

        File pomFile = new File(testDir, "pom.xml");
        assertThat(pomFile).isFile();
        SetupVerifier.verifySetup(pomFile);
    }

    @Test
    public void testSetupWithCustomQuarkusVersion() throws Exception {
        testDir = new File("target/test-classes", "projects/setup-with-custom-quarkus-version");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties params = new Properties();
        params.setProperty("quarkusVersion", "0.0.0");
        setup(params);

        File pomFile = new File(testDir, "pom.xml");
        assertThat(pomFile).isFile();
        SetupVerifier.verifySetupWithVersion(pomFile);
    }

    @AfterEach
    public void cleanup() {
        if (running != null) {
            running.stop();
        }
    }

    private InvocationResult setup(Properties params)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {

        params.setProperty("platformGroupId", ToolsConstants.IO_QUARKUS);
        params.setProperty("platformArtifactId", "quarkus-bom");
        params.setProperty("platformVersion", getPluginVersion());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                getPluginGroupId() + ":" + getPluginArtifactId() + ":" + getPluginVersion() + ":create"));
        request.setDebug(false);
        request.setShowErrors(false);
        request.setProperties(params);
        getEnv().forEach(request::addShellEnvironment);
        File log = new File(testDir, "build-create-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        return invoker.execute(request);
    }
}
