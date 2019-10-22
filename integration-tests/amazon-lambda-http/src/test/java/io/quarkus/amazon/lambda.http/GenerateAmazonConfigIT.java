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
    public static final String PLUGIN = "io.quarkus:quarkus-amazon-lambda-http-maven:configure-aws-lambda";
    private File testDir;

    @Test
    public void testConfigureWithNoTemplate() throws Exception {
        testDir = initProject("projects/configure-new-template");
        String template = "sam.yaml";
        String resource = "new-template";

        invoke(PLUGIN, template, resource);
        doAsserts(template, resource);
    }

    @Test
    public void testConfigureWithConfiguredName() throws Exception {
        testDir = initProject("projects/configure-named-template");
        String template = "properties.yaml";
        String resource = "named-template";

        invoke(PLUGIN, template, resource);
        doAsserts(template, resource);
    }

    @Test
    public void testConfigureWithExistingTemplate() throws Exception {
        testDir = initProject("projects/configure-existing-template");
        String template = "properties.yaml";
        String resource = "ThumbnailFunction";

        invoke(PLUGIN, template, resource);
        doAsserts(template, resource);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConfigureWithApplicationProperties() throws Exception {
        testDir = initProject("projects/configure-with-application-properties");

        invoke(PLUGIN, null, null);
        doAsserts("properties.yaml", "properties-resource");
    }

    @Test
    public void testConfigureDuringLifecycle() throws Exception {
        testDir = initProject("projects/configure-during-lifecycle");

        invoke("compile", null, null);
        doAsserts("properties.yaml", "properties-resource");
    }

    private void invoke(String goal, String template, String resource) throws Exception {

        Properties mavenProperties = new Properties();
        mavenProperties.put("projectGroupId", "org.acme");
        mavenProperties.put("projectArtifactId", "acme");
        mavenProperties.put("projectVersion", "1.0-SNAPSHOT");
        if (template != null) {
            mavenProperties.put("sam.template", template);
        }
        if (resource != null) {
            mavenProperties.put("sam.resource", resource);
        }

        final MavenProcessInvocationResult result = new RunningInvoker(testDir, false)
                .execute(singletonList(goal), emptyMap(), mavenProperties);

        assertThat(result.getProcess().waitFor()).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    private void doAsserts(String template, String resource) throws java.io.IOException {
        Map<String, Object> content = new ObjectMapper(new YAMLFactory())
                .readValue(new File(testDir, template), LinkedHashMap.class);

        Map<String, Object> properties = AmazonLambdaHttpMojo.get(content, "Resources", resource, "Properties");
        Assert.assertEquals(SAM_HANDLER, properties.get("Handler"));
        Assert.assertEquals(SAM_RUNTIME, properties.get("Runtime"));
    }

}
