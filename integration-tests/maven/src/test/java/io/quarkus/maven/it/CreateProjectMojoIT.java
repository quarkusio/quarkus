package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jboss.logmanager.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.test.devmode.util.DevModeTestUtils;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@DisableForNative
public class CreateProjectMojoIT extends QuarkusPlatformAwareMojoTestBase {

    private Invoker invoker;
    private RunningInvoker running;
    private File testDir;

    @Test
    public void testProjectGenerationFromScratch() throws MavenInvocationException, IOException {
        testDir = initEmptyProject("projects/project-generation");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("projectVersion", "1.0.0-SNAPSHOT");

        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();
        assertThat(new File(testDir, "src/main/resources/application.properties")).isFile();

        String config = Files
                .asCharSource(new File(testDir, "src/main/resources/application.properties"), Charsets.UTF_8)
                .read();
        assertThat(config).isEmpty();

        assertThat(new File(testDir, "src/main/docker/Dockerfile.native")).isFile();
        assertThat(new File(testDir, "src/main/docker/Dockerfile.jvm")).isFile();

        Model model = loadPom(testDir);
        final DependencyManagement dependencyManagement = model.getDependencyManagement();
        final List<Dependency> dependencies = dependencyManagement.getDependencies();
        assertThat(dependencies.stream()
                .anyMatch(d -> d.getArtifactId().equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_VALUE)
                        && d.getVersion().equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_VALUE)
                        && d.getScope().equals("import")
                        && d.getType().equals("pom"))).isTrue();

        assertThat(
                model.getDependencies().stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase("quarkus-resteasy")
                        && d.getVersion() == null)).isTrue();

        assertThat(model.getProfiles()).hasSize(1);
        assertThat(model.getProfiles().get(0).getId()).isEqualTo("native");

        Xpp3Dom surefireSystemProperties = Optional.ofNullable(model.getBuild())
                .map(Build::getPlugins)
                .flatMap(plugins -> plugins.stream().filter(p -> p.getArtifactId().equals("maven-surefire-plugin")).findFirst())
                .map(Plugin::getConfiguration)
                .map(Xpp3Dom.class::cast)
                .map(cfg -> cfg.getChild("systemPropertyVariables"))
                .orElse(null);
        assertThat(surefireSystemProperties).isNotNull();
        assertThat(surefireSystemProperties.getChild("java.util.logging.manager"))
                .returns(LogManager.class.getName(), from(Xpp3Dom::getValue));
        assertThat(surefireSystemProperties.getChild("maven.home"))
                .returns("${maven.home}", from(Xpp3Dom::getValue));
    }

    @Test
    public void testProjectGenerationWithExistingPomFileWithPackagingJarShouldFail() throws Exception {
        testDir = initProject("projects/simple-pom-it", "projects/project-generation-from-empty-pom");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);
        InvocationResult result = setup(new Properties());

        assertThat(result.getExitCode()).isOne();
    }

    @Test
    public void testProjectGenerationWithExistingGradleFileShouldFail() throws Exception {
        testDir = initProject("projects/parent-gradle-it", "projects/project-generation-from-parent-gradle");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);
        InvocationResult result = setup(new Properties());

        assertThat(result.getExitCode()).isOne();
    }

    @Test
    public void testGradleProjectGenerationWithExistingGradleFileShouldFail() throws Exception {
        testDir = initProject("projects/parent-gradle-it", "projects/gradle-project-generation-from-parent-gradle");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("buildTool", "gradle");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isOne();
    }

    @Test
    public void testGradleProjectGenerationWithExistingPomFileShouldFail() throws Exception {
        testDir = initProject("projects/parent-pom-it", "projects/gradle-project-generation-from-parent-pom");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);
        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("buildTool", "gradle");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isOne();
    }

    @Test
    public void testProjectGenerationAsModuleWithExistingPomFileWithPackagingPom() throws Exception {
        testDir = initProject("projects/parent-pom-it", "projects/project-generation-from-parent-pom");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        String projectArtifactId = "acme";
        Properties properties = new Properties();
        properties.put("projectGroupId", "io.acme.it");
        properties.put("projectArtifactId", projectArtifactId);
        properties.put("projectVersion", "1.0-SNAPSHOT");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        Model parentPomModel = loadPom(testDir);
        assertThat(parentPomModel.getModules()).isNotEmpty();
        assertThat(parentPomModel.getModules()).contains(projectArtifactId);

        Model modulePomModel = loadPom(new File(testDir, projectArtifactId));
        assertThat(modulePomModel.getParent()).isNotNull();
        assertThat(modulePomModel.getParent().getGroupId()).isEqualTo("io.acme.it");
        assertThat(modulePomModel.getParent().getArtifactId()).isEqualTo("acme-parent-pom");
        assertThat(modulePomModel.getParent().getVersion()).isEqualTo("0.0.1.BUILD-SNAPSHOT");
    }

    @Test
    public void testProjectGenerationFromScratchWithResource() throws Exception {
        testDir = initEmptyProject("projects/project-generation-with-resource");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource.java");
        properties.put("extensions", "resteasy");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();

        check(new File(testDir, "src/main/java/org/acme/MyResource.java"), "package org.acme;");
    }

    @Test
    public void testProjectGenerationWithInvalidPackage() throws Exception {
        testDir = initEmptyProject("projects/project-generation-invalid-package");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.invalid-package-name.MyResource");

        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isNotZero();
        assertThat(new File(testDir, "src/main/java/org/acme")).doesNotExist();
    }

    @Test
    public void testProjectGenerationFromScratchWithMissingExtensionShouldFail() throws Exception {
        testDir = initEmptyProject("projects/project-generation-with-missing-extension");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "resteasy,smallrye-metrics,missing");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isOne();
    }

    @Test
    public void testProjectGenerationFromScratchWithExtensions() throws Exception {
        testDir = initEmptyProject("projects/project-generation-with-resources-and-extension");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "resteasy,smallrye-metrics");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();

        check(new File(testDir, "src/main/java/org/acme/MyResource.java"), "package org.acme;");

        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("quarkus-resteasy", "quarkus-smallrye-metrics").doesNotContain("missing");

        Model model = loadPom(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream()
                .anyMatch(d -> d.getArtifactId().equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_VALUE)
                        && d.getVersion().equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_VALUE)
                        && d.getScope().equals("import")
                        && d.getType().equals("pom"))).isTrue();

        assertThat(
                model.getDependencies().stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase("quarkus-resteasy")
                        && d.getVersion() == null)).isTrue();

        assertThat(model.getDependencies().stream()
                .anyMatch(d -> d.getArtifactId().equalsIgnoreCase("quarkus-smallrye-metrics")
                        && d.getVersion() == null)).isTrue();
    }

    @Test
    public void testGradleProjectGenerationFromScratchWithExtensions() throws Exception {
        testDir = initEmptyProject("projects/gradle-project-generation-with-extensions");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "kotlin,resteasy,jackson");
        properties.put("buildTool", "gradle");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "build.gradle")).isFile();
        assertThat(new File(testDir, "gradlew.bat")).isFile();
        assertThat(new File(testDir, "gradlew")).isFile();
        assertThat(new File(testDir, "gradle/wrapper")).isDirectory();
        assertThat(new File(testDir, "src/main/kotlin")).isDirectory();

        check(new File(testDir, "src/main/kotlin/org/acme/MyResource.kt"), "package org.acme");

        assertThat(FileUtils.readFileToString(new File(testDir, "build.gradle"), "UTF-8"))
                .contains("quarkus-kotlin", "quarkus-jackson").doesNotContain("missing");
    }

    @Test
    public void testProjectGenerationFromScratchWithCustomDependencies() throws Exception {
        testDir = initEmptyProject("projects/project-generation-with-resource-and-custom-deps");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("className", "org.acme.MyResource");
        properties.put("extensions", "resteasy,commons-io:commons-io:2.5");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java/org/acme/MyResource.java")).isFile();
        assertThat(FileUtils.readFileToString(new File(testDir, "pom.xml"), "UTF-8"))
                .contains("commons-io");

        Model model = loadPom(testDir);
        assertThat(model.getDependencyManagement().getDependencies().stream()
                .anyMatch(d -> d.getArtifactId().equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_VALUE)
                        && d.getVersion().equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_VALUE)
                        && d.getScope().equals("import")
                        && d.getType().equals("pom"))).isTrue();

        assertThat(
                model.getDependencies().stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase("quarkus-resteasy")
                        && d.getVersion() == null)).isTrue();

        assertThat(model.getDependencies().stream().anyMatch(d -> d.getArtifactId().equalsIgnoreCase("commons-io")
                && d.getVersion().equalsIgnoreCase("2.5"))).isTrue();
    }

    @Test
    public void testProjectGenerationFromScratchWithAppConfigParameter() throws MavenInvocationException, IOException {
        testDir = initEmptyProject("projects/project-generation-with-config-param");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("projectVersion", "1.0.0-SNAPSHOT");

        List<String> configs = Arrays.asList("custom.app.config1=val1",
                "custom.app.config2=val2", "lib.config=val3");
        properties.put("appConfig", StringUtils.join(configs, ", "));

        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");

        assertThat(new File(testDir, "pom.xml")).isFile();
        assertThat(new File(testDir, "src/main/java")).isDirectory();

        String file = Files
                .asCharSource(new File(testDir, "src/main/resources/application.properties"), Charsets.UTF_8)
                .read();
        configs.forEach(conf -> Assertions.assertTrue(file.contains(conf)));

    }

    /**
     * Reproducer for https://github.com/quarkusio/quarkus/issues/671
     */
    @Test
    public void testThatDefaultPackageAreReplaced() throws Exception {
        testDir = initEmptyProject("projects/default-package-test");
        assertThat(testDir).isDirectory();
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("className", "MyGreatResource");
        properties.put("extensions", "resteasy");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();
        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "code-with-quarkus");
        check(new File(testDir, "src/main/java/org/acme/MyGreatResource.java"),
                "package org.acme;");
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
        invoker = initInvoker(testDir);

        Properties properties = new Properties();
        properties.put("projectGroupId", "org.acme");
        properties.put("projectArtifactId", "acme");
        properties.put("extensions", "resteasy");
        properties.put("className", "org.acme.HelloResource");
        InvocationResult result = setup(properties);

        assertThat(result.getExitCode()).isZero();

        // Run
        // As the directory is not empty (log) navigate to the artifactID directory
        testDir = new File(testDir, "acme");
        running = new RunningInvoker(testDir, false);
        final Properties mvnRunProps = new Properties();
        mvnRunProps.setProperty("debug", "false");
        running.execute(Arrays.asList("compile", "quarkus:dev"), Collections.emptyMap(), mvnRunProps);

        String resp = DevModeTestUtils.getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application").containsIgnoringCase("org.acme")
                .containsIgnoringCase("1.0.0-SNAPSHOT");

        String greeting = DevModeTestUtils.getHttpResponse("/hello");
        assertThat(greeting).containsIgnoringCase("hello");
    }

    private InvocationResult setup(Properties params)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {

        params.setProperty("platformGroupId", ToolsConstants.IO_QUARKUS);
        params.setProperty("platformArtifactId", "quarkus-bom");
        params.setProperty("platformVersion", getQuarkusCoreVersion());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                getMavenPluginGroupId() + ":" + getMavenPluginArtifactId() + ":" + getMavenPluginVersion() + ":create"));
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
