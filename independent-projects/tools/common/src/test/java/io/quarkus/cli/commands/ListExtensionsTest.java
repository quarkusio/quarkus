package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.readPom;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.QuarkusDependencyPredicate;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListExtensionsTest extends PlatformAwareTestBase {

    @Test
    public void listWithBom() throws Exception {
        final FileProjectWriter writer = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(writer, "commons-io:commons-io:2.5", "Agroal");

        final ListExtensions listExtensions = new ListExtensions(new MavenBuildFile(writer), getPlatformDescriptor());

        final Map<String, Dependency> installed = listExtensions.findInstalled();

        Assertions.assertNotNull(installed.get(getPluginGroupId() + ":quarkus-agroal"));
    }

    /**
     * When creating a project with Maven, you could have -Dextensions="resteasy, hibernate-validator".
     * <p>
     * Having a space is not automatically handled by the Maven converter injecting the properties
     * so we added code for that and we need to test it.
     */
    @Test
    public void listWithBomExtensionWithSpaces() throws Exception {
        final FileProjectWriter writer = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(writer, "resteasy", " hibernate-validator ");

        final ListExtensions listExtensions = new ListExtensions(new MavenBuildFile(writer), getPlatformDescriptor());

        final Map<String, Dependency> installed = listExtensions.findInstalled();

        Assertions.assertNotNull(installed.get(getPluginGroupId() + ":quarkus-resteasy"));
        Assertions.assertNotNull(installed.get(getPluginGroupId() + ":quarkus-hibernate-validator"));
    }

    @Test
    public void listWithoutBom() throws Exception {
        final File pom = new File("target/list-extensions-test", "pom.xml");
        final FileProjectWriter writer = createNewProject(pom);

        Model model = readPom(pom);

        model.setDependencyManagement(null);
        model.getDependencies().stream()
                .filter(new QuarkusDependencyPredicate())
                .forEach(d -> d.setVersion("0.0.1"));

        MojoUtils.write(model, pom);

        addExtensions(writer, "commons-io:commons-io:2.5", "Agroal");

        model = readPom(pom);

        final PrintStream out = System.out;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            System.setOut(printStream);
            new ListExtensions(new MavenBuildFile(writer), getPlatformDescriptor())
                    .all(true)
                    .format("full")
                    .execute();
        } finally {
            System.setOut(out);
        }
        boolean agroal = false;
        boolean resteasy = false;
        boolean hibernateValidator = false;
        final String output = baos.toString("UTF-8");
        boolean checkGuideInLineAfter = false;
        for (String line : output.split("\r?\n")) {
            if (line.contains(" Agroal ")) {
                assertTrue(line.startsWith("current"), "Agroal should list as current: " + line);
                agroal = true;
            } else if (line.contains(" RESTEasy  ")) {
                assertTrue(line.startsWith("update"), "RESTEasy should list as having an update: " + line);
                assertTrue(
                        line.endsWith(
                                String.format("%-16s", getPluginVersion())),
                        "RESTEasy should list as having an update: " + line);
                resteasy = true;
                checkGuideInLineAfter = true;
            } else if (line.contains(" Hibernate Validator  ")) {
                assertTrue(line.startsWith("   "), "Hibernate Validator should not list as anything: " + line);
                hibernateValidator = true;
            } else if (checkGuideInLineAfter) {
                checkGuideInLineAfter = false;
                assertTrue(
                        line.endsWith(
                                String.format("%s", "https://quarkus.io/guides/rest-json")),
                        "RESTEasy should list as having an guide: " + line);
            }
        }

        assertTrue(agroal && resteasy && hibernateValidator);
    }

    @Test
    public void searchUnexpected() throws Exception {
        final FileProjectWriter writer = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(writer, "commons-io:commons-io:2.5", "Agroal");

        final PrintStream out = System.out;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            System.setOut(printStream);
            new ListExtensions(new MavenBuildFile(writer), getPlatformDescriptor())
                    .all(true)
                    .format("full")
                    .search("unexpectedSearch")
                    .execute();
        } finally {
            System.setOut(out);
        }
        final String output = baos.toString("UTF-8");
        Assertions.assertEquals(String.format("No extension found with this pattern%n"), output,
                "search to unexpected extension must return a message");
    }

    @Test
    public void searchRest() throws Exception {
        final FileProjectWriter writer = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(writer, "commons-io:commons-io:2.5", "Agroal");

        final PrintStream out = System.out;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            System.setOut(printStream);
            new ListExtensions(new MavenBuildFile(writer), getPlatformDescriptor())
                    .all(true)
                    .format("full")
                    .search("Rest")
                    .execute();
        } finally {
            System.setOut(out);
        }
        final String output = baos.toString("UTF-8");
        Assertions.assertTrue(output.split("\r?\n").length > 7, "search to unexpected extension must return a message");
    }

    @Test
    void testListExtensionsWithoutAPomFile() throws IOException {
        ListExtensions listExtensions = new ListExtensions(null, getPlatformDescriptor());
        assertThat(listExtensions.findInstalled()).isEmpty();
    }

    private void addExtensions(FileProjectWriter writer, String... extensions) throws Exception {
        new AddExtensions(writer, getPlatformDescriptor())
                .extensions(new HashSet<>(asList(extensions)))
                .execute();
    }

    private FileProjectWriter createNewProject(final File pom) throws IOException, QuarkusCommandException {
        CreateProjectTest.delete(pom.getParentFile());
        final FileProjectWriter writer = new FileProjectWriter(pom.getParentFile());
        new CreateProject(writer, getPlatformDescriptor())
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .execute();
        return writer;
    }
}
