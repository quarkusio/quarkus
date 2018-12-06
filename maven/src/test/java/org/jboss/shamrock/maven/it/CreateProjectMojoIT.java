package org.jboss.shamrock.maven.it;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import org.jboss.shamrock.maven.CreateProjectMojo;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CreateProjectMojoIT extends MojoTestBase {

    private Invoker invoker;
    private File testDir;

    private void init(File root) {
        invoker = new DefaultInvoker();
        invoker.setWorkingDirectory(root);
        String repo = System.getProperty("maven.repo");
        if (repo == null) {
            repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }
        invoker.setLocalRepositoryDirectory(new File(repo));
        installPluginToLocalRepository(invoker.getLocalRepositoryDirectory());
    }

    @Test
    public void testProjectGenerationFromScratch() throws MavenInvocationException, FileNotFoundException {
        testDir = initEmptyProject("projects/project-generation");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        setup(properties);
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
    }

    @Test
    public void testProjectGenerationFromMinimalPom() throws Exception {
        testDir = initProject("projects/simple-pom-it", "projects/project-generation-from-empty-pom");
        assertThat(testDir).isDirectory();
        init(testDir);
        setup(new Properties());
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
            .contains(CreateProjectMojo.PLUGIN_ARTIFACTID, CreateProjectMojo.PLUGIN_VERSION_PROPERTY, CreateProjectMojo.PLUGIN_GROUPID);
        assertThat(new File(testDir, "src/main/java")).isDirectory();
    }

    @Test
    public void testProjectGenerationFromScratchWithResource() throws Exception {
        testDir = initEmptyProject("projects/project-generation-with-resource");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource.java");
        setup(properties);
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).isFile();
    }

    @Test
    public void testProjectGenerationFromMinimalPomWithResource() throws Exception {
        testDir = initProject("projects/simple-pom-it", "projects/project-generation-from-empty-pom-with-resource");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource.java");
        setup(properties);
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
            .contains("shamrock.version");
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).isFile();
    }

    @Test
    public void testProjectGenerationFromScratchWithExtensions() throws Exception {
        testDir = initEmptyProject("projects/project-generation-with-resources-and-extension");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "web,metrics,missing");

        setup(properties);
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
            .contains("shamrock-jaxrs-deployment", "shamrock-metrics-deployment").doesNotContain("missing");
    }

    @Test
    public void testProjectGenerationFromScratchWithCustomDependencies() throws Exception {
        testDir = initEmptyProject("projects/project-generation-with-resource-and-custom-deps");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "commons-io:commons-io:2.5");
        setup(properties);
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("commons-io");
    }

    @Test
    public void testProjectGenerationFromMinimalPomWithDependencies() throws Exception {
        testDir = initProject("projects/simple-pom-it",
            "projects/project-generation-from-minimal-pom-with-extensions");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "commons-io:commons-io:2.5");
        setup(properties);
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("commons-io");
    }

    private void setup(Properties params) throws MavenInvocationException, FileNotFoundException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                CreateProjectMojo.PLUGIN_KEY + ":" + MojoTestBase.VERSION + ":create"
        ));
        request.setProperties(params);
        getEnv().forEach(request::addShellEnvironment);
        File log = new File(testDir.getParentFile(), "build-create-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log)),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        invoker.execute(request);
    }

}
