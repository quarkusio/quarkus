package io.quarkus.cli.commands;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(asList("jdbc-postgre", "agroal", "quarkus-arc", " hibernate-validator",
                        "commons-io:commons-io:2.6")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        hasDependency(model, "quarkus-arc");
        hasDependency(model, "quarkus-hibernate-validator");
        hasDependency(model, "commons-io", "commons-io", "2.6");
        doesNotHaveDependency(model, "quarkus-jdbc-postgresql");
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
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(asList("missing")));

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
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
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
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(Collections.singletonList("agroal")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        Assertions.assertEquals(1,
                model.getDependencies().stream().filter(d -> d.getArtifactId().equals("quarkus-agroal")).count());
        Assertions.assertTrue(result.isUpdated());
        Assertions.assertTrue(result.succeeded());

        AddExtensionResult result2 = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(Collections.singletonList("agroal")));
        model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        Assertions.assertEquals(1,
                model.getDependencies().stream().filter(d -> d.getArtifactId().equals("quarkus-agroal")).count());
        Assertions.assertFalse(result2.isUpdated());
        Assertions.assertTrue(result2.succeeded());
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
        AddExtensionResult result = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
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
                .setName("some complex seo unaware name")
                .setLabels(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "e2", "1.0")
                .setName("some foo bar")
                .setLabels(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "e3", "1.0")
                .setName("unrelated")
                .setLabels(new String[] { "bar" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(2, matches.getExtensions().size());
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
        SelectionResult matches = AddExtensions.select("foo", extensions);
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
        SelectionResult matches = AddExtensions.select("foo", extensions);
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
        SelectionResult matches = AddExtensions.select("foo", extensions);
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
        SelectionResult matches = AddExtensions.select("foo", extensions);
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertNotNull(matches.getMatch());
        Assertions.assertTrue(matches.getMatch().getArtifactId().equalsIgnoreCase("quarkus-foo"));
    }

    @Test
    void testArtifactIdSelectionWithQuarkusSmallRyePrefix() {
        Extension e1 = new Extension("org.acme", "quarkus-smallrye-foo", "1.0")
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
        SelectionResult matches = AddExtensions.select("foo", extensions);
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertNotNull(matches.getMatch());
        Assertions.assertTrue(matches.getMatch().getArtifactId().equalsIgnoreCase("quarkus-smallrye-foo"));
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
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(asList("agroal", "jdbc", "non-exist-ent")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        doesNotHaveDependency(model, "quarkus-jdbc-postgresql");
        doesNotHaveDependency(model, "quarkus-jdbc-h2");
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
