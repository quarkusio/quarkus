package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.jupiter.api.Test;

@DisableForNative
class AddExtensionIT extends QuarkusPlatformAwareMojoTestBase {

    private static final String QUARKUS_GROUPID = "io.quarkus";
    private static final String BOM_ARTIFACT_ID = "quarkus-bom";
    private static final String VERTX_ARTIFACT_ID = "quarkus-vertx";
    private static final String COMMONS_CODEC = "commons-codec";
    private static final String PROJECT_SOURCE_DIR = "projects/classic";
    private File testDir;
    private Invoker invoker;

    @Test
    void testAddExtensionWithASingleExtension() throws MavenInvocationException, IOException {
        testDir = initProject(PROJECT_SOURCE_DIR, "projects/testAddExtensionWithASingleExtension");
        invoker = initInvoker(testDir);
        addExtension(false, VERTX_ARTIFACT_ID);

        Model model = loadPom(testDir);
        Dependency expected = new Dependency();
        expected.setGroupId(QUARKUS_GROUPID);
        expected.setArtifactId(VERTX_ARTIFACT_ID);
        assertThat(contains(model.getDependencies(), expected)).isTrue();
    }

    @Test
    void testAddExtensionWithASingleExtensionToSubmodule() throws MavenInvocationException, IOException {
        testDir = initProject("projects/multimodule", "projects/testAddExtensionWithASingleExtensionToSubmodule");
        testDir = new File(testDir, "runner");
        invoker = initInvoker(testDir);
        addExtension(false, VERTX_ARTIFACT_ID);

        Model model = loadPom(testDir);
        Dependency expected = new Dependency();
        expected.setGroupId(QUARKUS_GROUPID);
        expected.setArtifactId(VERTX_ARTIFACT_ID);
        assertThat(contains(model.getDependencies(), expected)).isTrue();

        assertThat(Optional.ofNullable(model.getDependencyManagement())
                .map(DependencyManagement::getDependencies).orElse(Collections.emptyList())).isEmpty();
    }

    @Test
    void testAddExtensionWithMultipleExtension() throws MavenInvocationException, IOException {
        testDir = initProject(PROJECT_SOURCE_DIR, "projects/testAddExtensionWithMultipleExtension");
        invoker = initInvoker(testDir);
        addExtension(false, "quarkus-vertx, commons-codec:commons-codec:1.15");

        Model model = loadPom(testDir);
        Dependency expected1 = new Dependency();
        expected1.setGroupId(QUARKUS_GROUPID);
        expected1.setArtifactId(VERTX_ARTIFACT_ID);
        Dependency expected2 = new Dependency();
        expected2.setGroupId(COMMONS_CODEC);
        expected2.setArtifactId(COMMONS_CODEC);
        expected2.setVersion("1.15");
        assertThat(contains(model.getDependencies(), expected1)).isTrue();
        assertThat(contains(model.getDependencies(), expected2)).isTrue();
    }

    @Test
    void testAddExtensionWithASingleExtensionWithPluralForm() throws MavenInvocationException, IOException {
        testDir = initProject(PROJECT_SOURCE_DIR,
                "projects/testAddExtensionWithASingleExtensionWithPluralForm");
        invoker = initInvoker(testDir);
        addExtension(true, VERTX_ARTIFACT_ID);

        Model model = loadPom(testDir);
        Dependency expected = new Dependency();
        expected.setGroupId(QUARKUS_GROUPID);
        expected.setArtifactId(VERTX_ARTIFACT_ID);
        assertThat(contains(model.getDependencies(), expected)).isTrue();
    }

    @Test
    void testAddExtensionWithMultipleExtensionsAndPluralForm() throws MavenInvocationException, IOException {
        testDir = initProject(PROJECT_SOURCE_DIR,
                "projects/testAddExtensionWithMultipleExtensionAndPluralForm");
        invoker = initInvoker(testDir);
        addExtension(true, "quarkus-vertx, commons-codec:commons-codec:1.15");

        Model model = loadPom(testDir);
        Dependency expected1 = new Dependency();
        expected1.setGroupId(QUARKUS_GROUPID);
        expected1.setArtifactId(VERTX_ARTIFACT_ID);
        Dependency expected2 = new Dependency();
        expected2.setGroupId(COMMONS_CODEC);
        expected2.setArtifactId(COMMONS_CODEC);
        expected2.setVersion("1.15");
        assertThat(contains(model.getDependencies(), expected1)).isTrue();
        assertThat(contains(model.getDependencies(), expected2)).isTrue();
    }

    private boolean contains(List<Dependency> dependencies, Dependency expected) {
        return dependencies.stream().anyMatch(dep -> dep.getGroupId().equals(expected.getGroupId())
                && dep.getArtifactId().equals(expected.getArtifactId())
                && (expected.getVersion() == null ? dep.getVersion() == null : expected.getVersion().equals(dep.getVersion()))
                && (dep.getScope() == null || dep.getScope().equals(expected.getScope()))
                && dep.isOptional() == expected.isOptional()
                && dep.getType().equals(expected.getType()));
    }

    private void addExtension(boolean plural, String ext)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList(
                getMavenPluginGroupId() + ":" + getMavenPluginArtifactId() + ":" + getMavenPluginVersion() + ":add-extension"));
        Properties properties = new Properties();
        properties.setProperty("platformGroupId", QUARKUS_GROUPID);
        properties.setProperty("platformArtifactId", BOM_ARTIFACT_ID);
        properties.setProperty("platformVersion", getQuarkusCoreVersion());
        if (plural) {
            properties.setProperty("extensions", ext);
        } else {
            properties.setProperty("extension", ext);
        }
        request.setProperties(properties);

        File log = new File(testDir, "build-add-extension-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        invoker.execute(request);
    }
}
