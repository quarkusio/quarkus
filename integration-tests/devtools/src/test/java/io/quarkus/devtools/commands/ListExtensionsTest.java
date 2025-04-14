package io.quarkus.devtools.commands;

import static io.quarkus.maven.utilities.MojoUtils.readPom;
import static io.quarkus.platform.tools.ToolsConstants.IO_QUARKUS;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.QuarkusDependencyPredicate;

public class ListExtensionsTest extends PlatformAwareTestBase {

    @Test
    public void listWithBom() throws Exception {
        final QuarkusProject project = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(project, "commons-io:commons-io:2.5", "Agroal");

        final ListExtensions listExtensions = new ListExtensions(project);

        final Map<ArtifactKey, ArtifactCoords> installed = readByKey(project);
        Assertions.assertNotNull(installed.get(ArtifactKey.fromString(IO_QUARKUS + ":quarkus-agroal")));
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

        final Map<ArtifactKey, ArtifactCoords> installed = readByKey(quarkusProject);

        Assertions.assertNotNull(installed.get(ArtifactKey.fromString(IO_QUARKUS + ":quarkus-resteasy")));
        Assertions.assertNotNull(
                installed.get(ArtifactKey.fromString(IO_QUARKUS + ":quarkus-hibernate-validator")));
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
                "io.quarkus:quarkus-hibernate-orm-panache:" + getMavenPluginVersion());

        model = readPom(pom);

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final PrintStream printStream = new PrintStream(baos, true, "UTF-8")) {
            new ListExtensions(quarkusProject, MessageWriter.info(printStream))
                    .all(true)
                    .format("full")
                    .execute();
            final String output = baos.toString("UTF-8");

            boolean agroal = false;
            boolean resteasy = false;
            boolean panache = false;
            boolean hibernateValidator = false;
            for (String line : output.split("\r?\n")) {
                if (line.contains("agroal")) {
                    assertTrue(line.startsWith("✬"), "Agroal is a platform extension: " + line);
                    agroal = true;
                } else if (line.contains("quarkus-resteasy ")) {
                    assertTrue(line.startsWith("✬"), "Resteasy is a platform extension: " + line);
                    resteasy = true;
                    assertTrue(
                            line.endsWith(
                                    String.format("%s", "https://quarkus.io/guides/resteasy")),
                            "RESTEasy should list as having an guide: " + line);
                } else if (line.contains("quarkus-hibernate-orm-panache ")) {
                    assertTrue(line.startsWith("✬"), "Panache is a platform extension: " + line);
                    panache = true;
                } else if (line.contains("hibernate-validator")) {
                    assertTrue(line.startsWith("✬"), "Hibernate validator is a platform extension: " + line);
                    hibernateValidator = true;
                }
            }

            assertTrue(agroal);
            assertTrue(resteasy);
            assertTrue(hibernateValidator);
            assertTrue(panache);
        }
    }

    @Test
    public void searchUnexpected() throws Exception {
        final QuarkusProject quarkusProject = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(quarkusProject, "commons-io:commons-io:2.5", "Agroal");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            new ListExtensions(quarkusProject, MessageWriter.info(printStream))
                    .all(true)
                    .format("full")
                    .search("unexpectedSearch")
                    .execute();
        }
        final String output = baos.toString("UTF-8");
        Assertions.assertEquals(String.format("No extension found with pattern 'unexpectedSearch'%n"), output,
                "search to unexpected extension must return a message");
    }

    @Test
    public void searchHibernate() throws Exception {
        final QuarkusProject quarkusProject = createNewProject(new File("target/list-extensions-test", "pom.xml"));
        addExtensions(quarkusProject, "commons-io:commons-io:2.5", "Agroal");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            new ListExtensions(quarkusProject, MessageWriter.info(printStream))
                    .all(true)
                    .format("full")
                    .search("Hibernate")
                    .execute();
        }
        final String output = baos.toString("UTF-8");
        Assertions.assertTrue(output.split("\r?\n").length > 7, "search to unexpected extension must return a message");
    }

    @Test
    void testListExtensionsWithoutAPomFile() throws Exception {
        final Path tempDirectory = Files.createTempDirectory("proj");
        final QuarkusProject project = QuarkusProjectHelper.getProject(tempDirectory, BuildTool.MAVEN);
        final Map<ArtifactKey, ArtifactCoords> installed = readByKey(project);
        assertTrue(installed.isEmpty());
        assertFalse(project.getExtensionsCatalog().getExtensions().isEmpty());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            new ListExtensions(project, MessageWriter.info(printStream)).execute();
        }
        final String output = baos.toString("UTF-8");
        Assertions.assertTrue(output.contains("Current Quarkus extensions available:"));
    }

    private void addExtensions(QuarkusProject quarkusProject, String... extensions) throws Exception {
        new AddExtensions(quarkusProject)
                .extensions(new HashSet<>(asList(extensions)))
                .execute();
    }

    private QuarkusProject createNewProject(final File pom) throws IOException, QuarkusCommandException {
        SnapshotTesting.deleteTestDirectory(pom.getParentFile());
        final Path projectDirPath = pom.getParentFile().toPath();
        final QuarkusProject project = QuarkusProjectHelper.getProject(projectDirPath, BuildTool.MAVEN);
        new CreateProject(project)
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .execute();
        return project;
    }

    private static Map<ArtifactKey, ArtifactCoords> readByKey(QuarkusProject project) throws IOException {
        // re-create the QuarkusProject to re-read the POM Model from the disk
        project = QuarkusProjectHelper.getProject(project.getProjectDirPath(), project.getExtensionsCatalog(),
                project.getBuildTool(), project.getJavaVersion(), project.log());
        return project.getExtensionManager().getInstalled().stream()
                .collect(toMap(ArtifactCoords::getKey, Function.identity()));
    }
}
