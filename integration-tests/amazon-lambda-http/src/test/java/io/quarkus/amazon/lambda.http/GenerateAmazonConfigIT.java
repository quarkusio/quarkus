package io.quarkus.amazon.lambda.http;

import static io.quarkus.amazon.lambda.http.AmazonLambdaHttpMojo.SAM_HANDLER;
import static io.quarkus.amazon.lambda.http.AmazonLambdaHttpMojo.SAM_RUNTIME;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

public class GenerateAmazonConfigIT extends MojoTestBase {
    private File testDir;
    private String template = "properties.yaml";
    private String resource = "Quarkus";

    @Test
    public void testConfigureWithNoTemplate() throws Exception {
        testDir = initProject("projects/configure-new-template");
        template = "sam.yaml";
        resource = "new-template";

        invoke();
    }

    @Test
    public void testConfigureWithConfiguredName() throws Exception {
        testDir = initProject("projects/configure-named-template");
        resource = "named-template";

        invoke();
    }

    @Test
    public void testConfigureWithExistingTemplate() throws Exception {
        testDir = initProject("projects/configure-existing-template");
        resource = "ThumbnailFunction";

        invoke();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConfigureWithApplicationProperties() throws Exception {
        testDir = initProject("projects/configure-with-application-properties");
        template = null;
        resource = null;

        invoke();
        Map<String, Object> content = new ObjectMapper(new YAMLFactory())
                .readValue(new File(testDir, "properties.yaml"), LinkedHashMap.class);

        Map<String, Object> properties = AmazonLambdaHttpMojo.get(content, "Resources", "properties-resource", "Properties");
        Assert.assertEquals(SAM_HANDLER, properties.get("Handler"));
        Assert.assertEquals(SAM_RUNTIME, properties.get("Runtime"));

    }

    @Test
    public void testConfigureDuringLifecycle() throws Exception {
        testDir = initProject("projects/configure-during-lifecycle");

        compile();
    }

    @SuppressWarnings("unchecked")
    private void compile() throws Exception {
        Properties mavenProperties = new Properties();
        mavenProperties.put("projectGroupId", "org.acme");
        mavenProperties.put("projectArtifactId", "acme");
        mavenProperties.put("projectVersion", "1.0-SNAPSHOT");

        RunningInvoker running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(singletonList("compile"), emptyMap(),
                mavenProperties);

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        Map<String, Object> content = new ObjectMapper(new YAMLFactory())
                .readValue(new File(testDir, "properties.yaml"), LinkedHashMap.class);

        Map<String, Object> properties = AmazonLambdaHttpMojo.get(content, "Resources", "properties-resource", "Properties");
        Assert.assertEquals(SAM_HANDLER, properties.get("Handler"));
        Assert.assertEquals(SAM_RUNTIME, properties.get("Runtime"));
    }

    @SuppressWarnings("unchecked")
    private void invoke() throws Exception {
        Properties mavenProperties = new Properties();
        mavenProperties.put("projectGroupId", "org.acme");
        mavenProperties.put("projectArtifactId", "acme");
        mavenProperties.put("projectVersion", "1.0-SNAPSHOT");
        if (template != null)
            mavenProperties.put("sam.template", template);
        if (resource != null)
            mavenProperties.put("sam.resource", resource);

        RunningInvoker running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(
                singletonList("io.quarkus:quarkus-amazon-lambda-http-maven:configure-aws-lambda"), emptyMap(),
                mavenProperties);

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        if (template != null && resource != null) {
            Map<String, Object> content = new ObjectMapper(new YAMLFactory())
                    .readValue(new File(testDir, template), LinkedHashMap.class);

            Map<String, Object> properties = AmazonLambdaHttpMojo.get(content, "Resources", resource, "Properties");
            Assert.assertEquals(SAM_HANDLER, properties.get("Handler"));
            Assert.assertEquals(SAM_RUNTIME, properties.get("Runtime"));
        }
    }
}
