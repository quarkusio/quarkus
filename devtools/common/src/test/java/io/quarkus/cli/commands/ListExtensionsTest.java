package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.getPluginGroupId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;
import static io.quarkus.maven.utilities.MojoUtils.readPom;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.QuarkusDependencyPredicate;

public class ListExtensionsTest {

    @Test
    public void listWithBom() throws IOException {
        final File pom = new File("target/list-extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        final HashMap<String, Object> context = new HashMap<>();

        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId(getPluginGroupId())
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(context);

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(asList("commons-io:commons-io:2.5", "Agroal")));

        Model model = readPom(pom);

        final ListExtensions listExtensions = new ListExtensions(model);

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
    public void listWithBomExtensionWithSpaces() throws IOException {
        final File pom = new File("target/list-extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        final HashMap<String, Object> context = new HashMap<>();

        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId(getPluginGroupId())
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(context);

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(asList("resteasy", " hibernate-validator ")));

        Model model = readPom(pom);

        final ListExtensions listExtensions = new ListExtensions(model);

        final Map<String, Dependency> installed = listExtensions.findInstalled();

        Assertions.assertNotNull(installed.get(getPluginGroupId() + ":quarkus-resteasy"));
        Assertions.assertNotNull(installed.get(getPluginGroupId() + ":quarkus-hibernate-validator"));
    }

    @Test
    public void listWithoutBom() throws IOException {
        final File pom = new File("target/list-extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        final HashMap<String, Object> context = new HashMap<>();

        new CreateProject(new FileProjectWriter(pom.getParentFile()))
                .groupId(getPluginGroupId())
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(context);

        Model model = readPom(pom);

        model.setDependencyManagement(null);
        model.getDependencies().stream()
                .filter(new QuarkusDependencyPredicate())
                .forEach(d -> d.setVersion("0.0.1"));

        MojoUtils.write(model, pom);

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(asList("commons-io:commons-io:2.5", "Agroal")));

        model = readPom(pom);

        final PrintStream out = System.out;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos, false, "UTF-8")) {
            System.setOut(printStream);
            new ListExtensions(model)
                    .listExtensions(true, "full");
        } finally {
            System.setOut(out);
        }
        boolean agroal = false;
        boolean resteasy = false;
        boolean hibernateValidator = false;
        final String output = baos.toString();
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
            } else if (line.contains(" Hibernate Validator  ")) {
                assertTrue(line.startsWith("   "), "Hibernate Validator should not list as anything: " + line);
                hibernateValidator = true;
            } else if (checkGuideInLineAfter) {
                checkGuideInLineAfter = false;
                assertTrue(
                        line.endsWith(
                                String.format("%s", "https://quarkus.io/guides/rest-json-guide")),
                        "RESTEasy should list as having an guide: " + line);
            }
        }

        assertTrue(agroal && resteasy && hibernateValidator);
    }
}
