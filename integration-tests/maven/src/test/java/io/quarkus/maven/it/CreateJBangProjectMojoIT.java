package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
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

import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.platform.tools.ToolsConstants;

@DisableForNative
public class CreateJBangProjectMojoIT extends QuarkusPlatformAwareMojoTestBase {

    private Invoker invoker;
    private RunningInvoker running;
    private File testDir;

    @Test
    public void testProjectGeneration() throws MavenInvocationException, IOException {
        testDir = initEmptyProject("projects/jbang-project-generation");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("outputDirectory", "jbang");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();
    }

    private InvocationResult setup(Properties params)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {

        params.setProperty("platformGroupId", ToolsConstants.IO_QUARKUS);
        params.setProperty("platformArtifactId", "quarkus-bom");
        params.setProperty("platformVersion", getQuarkusCoreVersion());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                getMavenPluginGroupId() + ":" + getMavenPluginArtifactId() + ":" + getMavenPluginVersion() + ":create-jbang"));
        request.setDebug(false);
        request.setShowErrors(true);
        request.setProperties(params);
        File log = new File(testDir, "build-create-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        return invoker.execute(request);
    }
}
