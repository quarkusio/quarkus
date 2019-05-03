package io.quarkus.amazon.lambda.resteasy;

import static io.quarkus.amazon.lambda.resteasy.deployment.AmazonLambdaResteasyProcessor.SAM_HANDLER;
import static io.quarkus.amazon.lambda.resteasy.deployment.AmazonLambdaResteasyProcessor.SAM_RUNTIME;
import static io.quarkus.amazon.lambda.resteasy.deployment.AmazonLambdaResteasyProcessor.walk;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.maven.it.MojoTestBase;

public class GenerateSamConfig extends MojoTestBase {

    private File testDir;
    private DefaultInvoker invoker;

    @Test
    public void testConfigureSAMWithNoTemplate() throws MavenInvocationException, IOException {
        testDir = initProject("projects/configure-new-template");
        initRepo();

        Map<String, Object> properties = walk(invoke(testDir, "sam.yaml"), "Resources", "new-template", "Properties");
        Assert.assertEquals(SAM_HANDLER, properties.get("Handler"));
        Assert.assertEquals(SAM_RUNTIME, properties.get("Runtime"));
    }

    @Test
    public void testConfigureSAMWithConfiguredName() throws MavenInvocationException, IOException {
        testDir = initProject("projects/configure-named-template");

        Map<String, Object> properties = walk(invoke(testDir, "sam.yaml"), "Resources", "named-template", "Properties");
        Assert.assertEquals(SAM_HANDLER, properties.get("Handler"));
        Assert.assertEquals(SAM_RUNTIME, properties.get("Runtime"));
    }

    @Test
    public void testConfigureSAMWithExistingTemplate() throws MavenInvocationException, IOException {
        testDir = initProject("projects/configure-existing-template");
        initRepo();

        Map<String, Object> properties = walk(invoke(testDir, "template.yaml"), "Resources", "ThumbnailFunction",
                "Properties");
        Assert.assertEquals(SAM_HANDLER, properties.get("Handler"));
        Assert.assertEquals(SAM_RUNTIME, properties.get("Runtime"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadFile(String template) throws IOException {
        return new ObjectMapper(new YAMLFactory())
                .readValue(new File(testDir, template), LinkedHashMap.class);
    }

    private void initRepo() {
        invoker = new DefaultInvoker();
        invoker.setWorkingDirectory(testDir);
        String repo = System.getProperty("maven.repo");
        if (repo == null) {
            repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }
        invoker.setLocalRepositoryDirectory(new File(repo));
    }

    private Map<String, Object> invoke(File testDir, String template) throws MavenInvocationException, IOException {
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("projectVersion", "1.0-SNAPSHOT");

        return invoke(template, properties);
    }

    private Map<String, Object> invoke(String template, Properties properties) throws MavenInvocationException, IOException {
        initRepo();
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setShowErrors(true);
        request.setGoals(asList("package"));
        request.setProperties(properties);
        getEnv().forEach(request::addShellEnvironment);
        File log = new File(testDir, "maven.log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        invoker.execute(request);

        return loadFile(template);
    }
}
