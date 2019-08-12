package io.quarkus.cli.commands;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

class AddExtensionsTest {

    @Test
    void addSomeValidExtensions() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(asList("jdbc-postgre", "agroal", "quarkus-arc", " hibernate-validator",
                        "commons-io:commons-io:2.6")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        hasDependency(model, "quarkus-arc");
        hasDependency(model, "quarkus-hibernate-validator");
        hasDependency(model, "commons-io", "commons-io", "2.6");
        hasDependency(model, "quarkus-jdbc-postgresql");
    }

    @Test
    void testPartialMatches() throws IOException {
        File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(asList("orm-pana", "jdbc-postgre", "arc")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-arc");
        hasDependency(model, "quarkus-hibernate-orm-panache");
        hasDependency(model, "quarkus-jdbc-postgresql");
    }

    @Test
    void addMissingExtension() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(Collections.singletonList("missing")));

        Model model = MojoUtils.readPom(pom);
        doesNotHaveDependency(model, "quarkus-missing");
        Assertions.assertFalse(result.succeeded());
        Assertions.assertFalse(result.isUpdated());
    }

    @Test
    void addExtensionTwiceInOneBatch() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(asList("agroal", "agroal")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        Assertions.assertEquals(1,
                model.getDependencies().stream().filter(d -> d.getArtifactId().equals("quarkus-agroal")).count());
        Assertions.assertTrue(result.isUpdated());
        Assertions.assertTrue(result.succeeded());
    }

    @Test
    void addExtensionTwiceInTwoBatches() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(Collections.singletonList("agroal")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        Assertions.assertEquals(1,
                model.getDependencies().stream().filter(d -> d.getArtifactId().equals("quarkus-agroal")).count());
        Assertions.assertTrue(result.isUpdated());
        Assertions.assertTrue(result.succeeded());

        AddExtensionResult result2 = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(Collections.singletonList("agroal")));
        model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        Assertions.assertEquals(1,
                model.getDependencies().stream().filter(d -> d.getArtifactId().equals("quarkus-agroal")).count());
        Assertions.assertFalse(result2.isUpdated());
        Assertions.assertTrue(result2.succeeded());
    }

    /**
     * This test reproduce the issue we had using the first selection algorithm.
     * The `arc` query was matching ArC but also hibernate-search-elasticsearch.
     */
    @Test
    void testPartialMatchConflict() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(Collections.singletonList("arc")));

        Assertions.assertTrue(result.isUpdated());
        Assertions.assertTrue(result.succeeded());
        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-arc");

        result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(Collections.singletonList("elasticsearch")));

        Assertions.assertTrue(result.isUpdated());
        Assertions.assertTrue(result.succeeded());
        model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-hibernate-search-elasticsearch");
    }

    @Test
    void addExistingAndMissingExtensions() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(asList("missing", "agroal")));

        Model model = MojoUtils.readPom(pom);
        doesNotHaveDependency(model, "quarkus-missing");
        hasDependency(model, "quarkus-agroal");
        Assertions.assertFalse(result.succeeded());
        Assertions.assertTrue(result.isUpdated());
    }

    @Test
    void testMultiMatchByLabels() {
        Extension e1 = new Extension("org.acme", "e1", "1.0")
                .setName("some extension 1")
                .setLabels(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "e2", "1.0")
                .setName("some extension 2")
                .setLabels(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "e3", "1.0")
                .setName("unrelated")
                .setLabels(new String[] { "bar" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = AddExtensions.select("foo", extensions, false);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(0, matches.getExtensions().size());
    }

    @Test
    void testThatSingleLabelMatchIsNotAMatch() {
        Extension e1 = new Extension("org.acme", "e1", "1.0")
                .setName("e1")
                .setLabels(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "e2", "1.0")
                .setName("e2")
                .setLabels(new String[] { "bar", "baz" });

        List<Extension> extensions = asList(e1, e2);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(1, matches.getExtensions().size());
    }

    @Test
    void testMultiMatchByArtifactIdsAndNames() {
        Extension e1 = new Extension("org.acme", "e1", "1.0")
                .setName("foo")
                .setLabels(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "quarkus-foo", "1.0")
                .setName("some foo bar")
                .setLabels(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "e3", "1.0")
                .setName("unrelated")
                .setLabels(new String[] { "foo" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, false);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = AddExtensions.select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(3, matches.getExtensions().size());

    }

    @Test
    void testShortNameSelection() {
        Extension e1 = new Extension("org.acme", "some-complex-seo-unaware-artifactid", "1.0")
                .setName("some complex seo unaware name")
                .setShortName("foo")
                .setLabels(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "some-foo-bar", "1.0")
                .setName("some foo bar")
                .setLabels(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "unrelated", "1.0")
                .setName("unrelated")
                .setLabels(new String[] { "foo" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, false);
        Assertions.assertTrue(matches.matches());
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertNotNull(matches.getMatch());
        Assertions.assertTrue(matches.getMatch().getArtifactId().equalsIgnoreCase("some-complex-seo-unaware-artifactid"));
    }

    @Test
    void testArtifactIdSelectionWithQuarkusPrefix() {
        Extension e1 = new Extension("org.acme", "quarkus-foo", "1.0")
                .setName("some complex seo unaware name")
                .setShortName("foo")
                .setLabels(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "quarkus-foo-bar", "1.0")
                .setName("some foo bar")
                .setLabels(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "quarkus-unrelated", "1.0")
                .setName("unrelated")
                .setLabels(new String[] { "foo" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, false);
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertNotNull(matches.getMatch());
        Assertions.assertTrue(matches.getMatch().getArtifactId().equalsIgnoreCase("quarkus-foo"));
    }

    @Test
    void addDuplicatedExtension() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(asList("agroal", "jdbc", "non-exist-ent")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        doesNotHaveDependency(model, "quarkus-jdbc-postgresql");
        doesNotHaveDependency(model, "quarkus-jdbc-h2");
    }

    @Test
    void addDuplicatedExtensionUsingGAV() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(asList("org.acme:acme:1", "org.acme:acme:1")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "org.acme", "acme", "1");
        Assertions.assertEquals(1,
                model.getDependencies().stream().filter(d -> d.getArtifactId().equalsIgnoreCase("acme")).count());
    }

    @Test
    void testVertxWithAndWithoutDot() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        // Test with vertx
        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(Collections.singletonList("vertx")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-vertx");

        // Test with vert.x (the official writing)
        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()))
                .addExtensions(new HashSet<>(Collections.singletonList("vert.x")));

        model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-vertx");
    }

    private void hasDependency(final Model model, final String artifactId) {
        Assertions.assertTrue(model.getDependencies()
                .stream()
                .anyMatch(d -> d.getGroupId().equals(MojoUtils.getPluginGroupId()) &&
                        d.getArtifactId().equals(artifactId)));
    }

    private void hasDependency(final Model model, String groupId, String artifactId, String version) {
        Assertions.assertTrue(model.getDependencies()
                .stream()
                .anyMatch(d -> d.getGroupId().equals(groupId) &&
                        d.getArtifactId().equals(artifactId) &&
                        d.getVersion().equals(version)));
    }

    private void doesNotHaveDependency(final Model model, final String artifactId) {
        Assertions.assertFalse(model.getDependencies()
                .stream()
                .anyMatch(d -> d.getGroupId().equals(MojoUtils.getPluginGroupId()) &&
                        d.getArtifactId().equals(artifactId)));
    }
}
