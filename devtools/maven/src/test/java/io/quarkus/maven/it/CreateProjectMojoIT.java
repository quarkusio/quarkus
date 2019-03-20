package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import io.quarkus.maven.CreateProjectMojo;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CreateProjectMojoIT extends MojoTestBase {

    private Invoker invoker;
    private RunningInvoker running;
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
    public void testProjectGenerationFromScratch() throws MavenInvocationException, IOException {
        testDir = initEmptyProject("projects/project-generation");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("projectVersion", "1.0-SNAPSHOT");
        setup(properties);

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/resources/application.properties")).isFile();

        String config = Files
                .asCharSource(new File(testDir, "src/main/resources/application.properties"), Charsets.UTF_8)
                .read();
        assertThat(config).contains("key = value");

        assertThat(new File(testDir, "src/main/docker/Dockerfile.native")).isFile();
        assertThat(new File(testDir, "src/main/docker/Dockerfile.jvm")).isFile();

        Model model = load(testDir);
        final DependencyManagement dependencyManagement = model.getDependencyManagement();
        final List<Dependency> dependencies = dependencyManagement.getDependencies();
        assertThat(dependencies.stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase(MojoUtils.getBomArtifactId())
                && d.getVersion().equalsIgnoreCase("${quarkus.version}")
                && d.getScope().equalsIgnoreCase("import")
                && d.getType().equalsIgnoreCase("pom"))).isTrue();

        assertThat(
                model.getDependencies().stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase("quarkus-resteasy")
                        && d.getVersion() == null)).isTrue();

        assertThat(model.getProfiles()).hasSize(1);
        assertThat(model.getProfiles().get(0).getId()).isEqualTo("native");
    }

    private Model load(File directory) {
        File pom = new File(directory, "pom.xml");
        assertThat(pom).isFile();
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(pom), StandardCharsets.UTF_8);) {
            return new MavenXpp3Reader().read(isr);
        } catch (IOException | XmlPullParserException e) {
            throw new IllegalArgumentException("Cannot read the pom.xml file", e);
        }
    }

    @Test
    public void testProjectGenerationFromEmptyPom() throws Exception {
        testDir = initProject("projects/simple-pom-it", "projects/project-generation-from-empty-pom");
        assertThat(testDir).isDirectory();
        init(testDir);
        setup(new Properties());

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains(MojoUtils.getPluginGroupId(), MojoUtils.QUARKUS_VERSION_PROPERTY, MojoUtils.getPluginGroupId());
        assertThat(new File(testDir, "src/main/java")).isDirectory();

        assertThat(new File(testDir, "src/main/resources/application.properties")).exists();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).exists();

        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .containsIgnoringCase(MojoUtils.getBomArtifactId());

        Model model = load(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream()
                .anyMatch(d -> d.getArtifactId().equalsIgnoreCase(MojoUtils.getBomArtifactId())
                        && d.getVersion().equalsIgnoreCase("${quarkus.version}")
                        && d.getScope().equalsIgnoreCase("import")
                        && d.getType().equalsIgnoreCase("pom"))).isTrue();

        assertThat(model.getDependencies()).isEmpty();

        assertThat(model.getProfiles()).hasSize(1);
        assertThat(model.getProfiles().get(0).getId()).isEqualTo("native");
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

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();

        check(new File(testDir, "src/main/java/org/acme/MyResource.java"), "package org.acme;");
    }

    @Test
    public void testProjectGenerationFromMinimalPomWithResource() throws Exception {
        testDir = initProject("projects/simple-pom-it", "projects/project-generation-from-empty-pom-with-resource");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("className", "org.acme.MyResource.java");
        setup(properties);

        check(new File(testDir, "pom.xml"), "quarkus.version");

        assertThat(new File(testDir, "src/main/java")).isDirectory();

        check(new File(testDir, "src/main/java/org/acme/MyResource.java"), "package org.acme;");
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
        properties.put("extensions", "resteasy,smallrye-metrics,missing");
        setup(properties);

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();

        check(new File(testDir, "src/main/java/org/acme/MyResource.java"), "package org.acme;");

        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("quarkus-resteasy", "quarkus-smallrye-metrics").doesNotContain("missing");

        Model model = load(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream()
                .anyMatch(d -> d.getArtifactId().equalsIgnoreCase(MojoUtils.getBomArtifactId())
                        && d.getVersion().equalsIgnoreCase("${quarkus.version}")
                        && d.getScope().equalsIgnoreCase("import")
                        && d.getType().equalsIgnoreCase("pom"))).isTrue();

        assertThat(
                model.getDependencies().stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase("quarkus-resteasy")
                        && d.getVersion() == null)).isTrue();

        assertThat(model.getDependencies().stream()
                .anyMatch(d -> d.getArtifactId().equalsIgnoreCase("quarkus-smallrye-metrics")
                        && d.getVersion() == null)).isTrue();
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

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("commons-io");

        Model model = load(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream()
                .anyMatch(d -> d.getArtifactId().equalsIgnoreCase(MojoUtils.getBomArtifactId())
                        && d.getVersion().equalsIgnoreCase("${quarkus.version}")
                        && d.getScope().equalsIgnoreCase("import")
                        && d.getType().equalsIgnoreCase("pom"))).isTrue();

        assertThat(
                model.getDependencies().stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase("quarkus-resteasy")
                        && d.getVersion() == null)).isTrue();

        assertThat(model.getDependencies().stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase("commons-io")
                && d.getVersion().equalsIgnoreCase("2.5"))).isTrue();
    }

    @Test
    public void testProjectGenerationFromMinimalPomWithDependencies() throws Exception {
        testDir = initProject("projects/simple-pom-it",
                "projects/project-generation-from-minimal-pom-with-extensions");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "commons-io:commons-io:2.5");
        setup(properties);
        check(new File(testDir, "src/main/java/org/acme/MyResource.java"), "package org.acme;");
        check(new File(testDir, "pom.xml"), "commons-io");
    }

    /**
     * Reproducer for https://github.com/jbossas/quarkus/issues/671
     */
    @Test
    public void testThatDefaultPackageAreReplaced() throws Exception {
        testDir = initEmptyProject("projects/default-package-test");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("className", "MyGreatResource");
        setup(properties);
        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "my-quarkus-project");
        check(new File(testDir, "src/main/java/org/acme/quarkus/sample/MyGreatResource.java"),
                "package org.acme.quarkus.sample;");
    }

    /**
     * Reproducer for https://github.com/jbossas/quarkus/issues/673
     */
    @Test
    public void testThatGenerationFailedWhenTheUserPassGAVonExistingPom() throws Exception {
        testDir = initProject("projects/simple-pom-it", "projects/fail-on-gav-and-existing-pom");
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("className", "MyResource");
        InvocationResult result = setup(properties);
        assertThat(result.getExitCode()).isNotZero();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).doesNotExist();
    }

    private void check(final File resource, final String contentsToFind) throws IOException {
        assertThat(resource).isFile();
        assertThat(FileUtils.readFileToString(resource, "UTF-8")).contains(contentsToFind);
    }

    @AfterEach
    public void cleanup() {
        if (running != null) {
            running.stop();
        }
    }

    @Test
    public void generateNewProjectAndRun() throws Exception {
        testDir = initEmptyProject("projects/project-generation-and-run");

        // Scaffold the new project
        assertThat(testDir).isDirectory();
        init(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.HelloResource");
        setup(properties);

        // Run
        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "quarkus:dev"), Collections.emptyMap());

        String resp = getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    private InvocationResult setup(Properties params)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                CreateProjectMojo.PLUGIN_KEY + ":" + MojoUtils.getPluginVersion() + ":create"));
        request.setProperties(params);
        getEnv().forEach(request::addShellEnvironment);
        File log = new File(testDir, "build-create-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        return invoker.execute(request);
    }

}
