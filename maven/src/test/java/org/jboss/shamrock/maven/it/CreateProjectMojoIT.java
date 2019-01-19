package org.jboss.shamrock.maven.it;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.shamrock.maven.MavenConstants;
import org.jboss.shamrock.maven.CreateProjectMojo;
import org.jboss.shamrock.maven.it.verifier.RunningInvoker;
import org.jboss.shamrock.maven.utilities.MojoUtils;
import org.junit.After;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
        setup(properties);
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/resources/META-INF/microprofile-config.properties")).isFile();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).isFile();

        String config = Files.asCharSource(new File(testDir, "src/main/resources/META-INF/microprofile-config.properties"), Charsets.UTF_8)
                .read();
        assertThat(config).contains("key = value");

        String index = Files.asCharSource(new File(testDir, "src/main/resources/META-INF/resources/index.html"), Charsets.UTF_8)
                .read();
        assertThat(index).contains("org.acme");
        assertThat(index).contains("1.0-SNAPSHOT");
        assertThat(index).contains(VERSION);

        assertThat(new File(testDir, "src/main/docker/Dockerfile")).isFile();

        Model model = load(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase(MojoUtils.get("bom-artifactId"))
                        && d.getVersion().equalsIgnoreCase("${shamrock.version}")
                        && d.getScope().equalsIgnoreCase("import")
                        && d.getType().equalsIgnoreCase("pom"))).isTrue();

        assertThat(model.getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase("shamrock-jaxrs-deployment")
                        && d.getVersion() == null)).isTrue();
    }

    private Model load(File directory) {
        File pom = new File(directory, "pom.xml");
        assertThat(pom).isFile();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileReader fr = new FileReader(pom)) {
            return reader.read(fr);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot find the pom.xml file", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read the pom.xml file", e);
        } catch (XmlPullParserException e) {
            throw new IllegalArgumentException("Malformed pom.xml file", e);
        }
    }

    @Test
    public void testProjectGenerationFromMinimalPom() throws Exception {
        testDir = initProject("projects/simple-pom-it", "projects/project-generation-from-empty-pom");
        assertThat(testDir).isDirectory();
        init(testDir);
        setup(new Properties());
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains(MavenConstants.PLUGIN_ARTIFACTID, CreateProjectMojo.PLUGIN_VERSION_PROPERTY, MavenConstants.PLUGIN_GROUPID);
        assertThat(new File(testDir, "src/main/java")).isDirectory();

        assertThat(new File(testDir, "src/main/resources/META-INF/microprofile-config.properties")).doesNotExist();
        assertThat(new File(testDir, "src/main/resources/META-INF/resources/index.html")).doesNotExist();

        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8")).containsIgnoringCase(MojoUtils.get("bom-artifactId"));

        Model model = load(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase(MojoUtils.get("bom-artifactId"))
                        && d.getVersion().equalsIgnoreCase("${shamrock.version}")
                        && d.getScope().equalsIgnoreCase("import")
                        && d.getType().equalsIgnoreCase("pom"))).isTrue();

        assertThat(model.getDependencies()).isEmpty();
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
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).doesNotExist();
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
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).doesNotExist();
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
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).doesNotExist();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("shamrock-jaxrs-deployment", "shamrock-metrics-deployment").doesNotContain("missing");

        Model model = load(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase(MojoUtils.get("bom-artifactId"))
                        && d.getVersion().equalsIgnoreCase("${shamrock.version}")
                        && d.getScope().equalsIgnoreCase("import")
                        && d.getType().equalsIgnoreCase("pom"))).isTrue();

        System.out.println(model.getDependencies().stream().map(d -> d.getManagementKey() + "[" + d.getVersion() + "]").collect(Collectors.toList()));

        assertThat(model.getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase("shamrock-jaxrs-deployment")
                        && d.getVersion() == null)).isTrue();

        assertThat(model.getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase("shamrock-metrics-deployment")
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
        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).doesNotExist();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("commons-io");

        Model model = load(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase(MojoUtils.get("bom-artifactId"))
                        && d.getVersion().equalsIgnoreCase("${shamrock.version}")
                        && d.getScope().equalsIgnoreCase("import")
                        && d.getType().equalsIgnoreCase("pom"))).isTrue();

        assertThat(model.getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase("shamrock-jaxrs-deployment")
                        && d.getVersion() == null)).isTrue();

        assertThat(model.getDependencies().stream().anyMatch(d ->
                d.getArtifactId().equalsIgnoreCase("commons-io")
                        && d.getVersion().equalsIgnoreCase("2.5"))).isTrue();
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
        assertThat(new File(testDir, "src/main/java/org/acme/ShamrockApplication.java")).doesNotExist();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("commons-io");
    }

    @After
    public void cleanup() {
        if (running != null) {
            running.stop();
        }
    }

    @Test
    public void generateNewProjectAndRun() throws MavenInvocationException, FileNotFoundException {
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
        running = new RunningInvoker(testDir, false);
        running.execute(Arrays.asList("compile", "shamrock:dev"), Collections.emptyMap());

        String resp = getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0-SNAPSHOT");

        String greeting = getHttpResponse("/hello");
        assertThat(greeting).containsIgnoringCase("hello");
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
