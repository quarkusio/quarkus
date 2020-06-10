package io.quarkus.devtools.commands;

import static io.quarkus.maven.utilities.MojoUtils.readPom;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.QuarkusDependencyPredicate;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListExtensionsTest extends PlatformAwareTestBase {

    @Test
    public void listWithBom() throws Exception {
        final QuarkusProject project = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(project, "commons-io:commons-io:2.5", "Agroal");

        final ListExtensions listExtensions = new ListExtensions(project);

        final Map<AppArtifactKey, AppArtifactCoords> installed = readByKey(project);

        Assertions.assertNotNull(installed.get(AppArtifactKey.fromString(getPluginGroupId() + ":quarkus-agroal")));
    }

    /**
     * When creating a project with Maven, you could have -Dextensions="resteasy, hibernate-validator".
     * <p>
     * Having a space is not automatically handled by the Maven converter injecting the properties
     * so we added code for that and we need to test it.
     */
    @Test
    public void listWithBomExtensionWithSpaces() throws Exception {
        final QuarkusProject quarkusProject = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(quarkusProject, "resteasy", " hibernate-validator ");

        final ListExtensions listExtensions = new ListExtensions(quarkusProject);

        final Map<AppArtifactKey, AppArtifactCoords> installed = readByKey(quarkusProject);

        Assertions.assertNotNull(installed.get(AppArtifactKey.fromString(getPluginGroupId() + ":quarkus-resteasy")));
        Assertions.assertNotNull(installed.get(AppArtifactKey.fromString(getPluginGroupId() + ":quarkus-hibernate-validator")));
    }

    @Test
    public void listWithoutBom() throws Exception {
        final File pom = new File("target/list-extensions-test", "pom.xml");
        final QuarkusProject quarkusProject = createNewProject(pom);
        Model model = readPom(pom);

        model.getDependencies().stream()
                .filter(new QuarkusDependencyPredicate())
                .forEach(d -> d.setVersion("0.0.1"));

        MojoUtils.write(model, pom);

        addExtensions(quarkusProject, "commons-io:commons-io:2.5", "Agroal",
                "io.quarkus:quarkus-hibernate-orm-panache:" + getPluginVersion());

        model = readPom(pom);

        final PrintStream out = System.out;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            System.setOut(printStream);
            new ListExtensions(quarkusProject)
                    .all(true)
                    .format("full")
                    .execute();
        } finally {
            System.setOut(out);
        }
        boolean agroal = false;
        boolean resteasy = false;
        boolean panache = false;
        boolean hibernateValidator = false;
        final String output = baos.toString("UTF-8");
        boolean checkGuideInLineAfter = false;
        for (String line : output.split("\r?\n")) {
            if (line.contains(" Agroal ")) {
                assertTrue(line.startsWith("default"), "Agroal should list as being default: " + line);
                agroal = true;
            } else if (line.contains(" RESTEasy  ")) {
                assertTrue(line.startsWith("custom*"), "RESTEasy should list as being custom*: " + line);
                assertTrue(
                        line.endsWith(
                                String.format("%-15s", getPluginVersion())),
                        "RESTEasy should list as being custom*: " + line);
                resteasy = true;
                checkGuideInLineAfter = true;
            } else if (line.contains("quarkus-hibernate-orm-panache")) {
                assertTrue(line.startsWith("custom"), "Panache should list as being custom: " + line);
                assertTrue(
                        line.endsWith(String.format("%-25s", getPluginVersion())),
                        "Panache should list as being custom*: " + line);
                panache = true;
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

        assertTrue(agroal && resteasy && hibernateValidator && panache);
    }

    @Test
    public void searchUnexpected() throws Exception {
        final QuarkusProject quarkusProject = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(quarkusProject, "commons-io:commons-io:2.5", "Agroal");

        final PrintStream out = System.out;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            System.setOut(printStream);
            new ListExtensions(quarkusProject)
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
        final QuarkusProject quarkusProject = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(quarkusProject, "commons-io:commons-io:2.5", "Agroal");

        final PrintStream out = System.out;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            System.setOut(printStream);
            new ListExtensions(quarkusProject)
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
        final Path tempDirectory = Files.createTempDirectory("proj");
        final QuarkusProject project = QuarkusProject.maven(tempDirectory, getPlatformDescriptor());
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> readByKey(project))
                .withRootCauseInstanceOf(NoSuchFileException.class);
    }

    private void addExtensions(QuarkusProject quarkusProject, String... extensions) throws Exception {
        new AddExtensions(quarkusProject)
                .extensions(new HashSet<>(asList(extensions)))
                .execute();
    }

    private QuarkusProject createNewProject(final File pom) throws IOException, QuarkusCommandException {
        CreateProjectTest.delete(pom.getParentFile());
        final Path projectFolderPath = pom.getParentFile().toPath();
        new CreateProject(projectFolderPath, getPlatformDescriptor())
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .execute();
        return QuarkusProject.maven(projectFolderPath, getPlatformDescriptor());
    }

    private static Map<AppArtifactKey, AppArtifactCoords> readByKey(QuarkusProject project) throws IOException {
        return project.getExtensionManager().getInstalled().stream()
                .collect(toMap(AppArtifactCoords::getKey, Function.identity()));
    }
}
