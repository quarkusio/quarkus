package org.jboss.shamrock.cli.commands;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jboss.shamrock.maven.utilities.MojoUtils;
import org.jboss.shamrock.maven.utilities.ShamrockDependencyPredicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.jboss.shamrock.maven.utilities.MojoUtils.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListExtensionsTest {

    @Test
    public void listWithBom() throws IOException {
        final File pom = new File("target/list-extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        final HashMap<String, Object> context = new HashMap<>();

        new CreateProject(pom.getParentFile())
            .groupId(getPluginGroupId())
            .artifactId("add-extension-test")
            .version("0.0.1-SNAPSHOT")
            .doCreateProject(context);

        new AddExtensions(pom)
            .addExtensions(asList("commons-io:commons-io:2.5", "Agroal"));

        Model model = readPom(pom);

        final ListExtensions listExtensions = new ListExtensions(model);

        final Map<String, Dependency> installed = listExtensions.findInstalled();

        Assertions.assertNotNull(installed.get(getPluginGroupId() + ":shamrock-agroal-deployment"));
    }

    @Test
    public void listWithOutBom() throws IOException {
        final File pom = new File("target/list-extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        final HashMap<String, Object> context = new HashMap<>();

        new CreateProject(pom.getParentFile())
            .groupId(getPluginGroupId())
            .artifactId("add-extension-test")
            .version("0.0.1-SNAPSHOT")
            .doCreateProject(context);

        Model model = readPom(pom);

        model.setDependencyManagement(null);
        model.getDependencies().stream()
             .filter(new ShamrockDependencyPredicate())
             .forEach(d -> d.setVersion("0.0.1"));

        MojoUtils.write(model, pom);

        new AddExtensions(pom)
            .addExtensions(asList("commons-io:commons-io:2.5", "Agroal"));

        model = readPom(pom);

        final PrintStream out = System.out;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final PrintStream printStream = new PrintStream(baos)) {
            System.setOut(printStream);
            new ListExtensions(model)
                .listExtensions();
        } finally {
            System.setOut(out);
        }
        boolean jaxrs = false;
        boolean arc = false;
        boolean agroal = false;
        boolean bean = false;
        final String output = baos.toString();
        for (String line : output.split("\n")) {
            if (line.contains(" Agroal ")) {
                assertTrue(line.startsWith("current"), "Agroal should list as current: " + line);
                agroal = true;
            } else if (line.contains(" Arc ")) {
                assertTrue(line.startsWith("update"), "Arc should list as having an update: " + line);
                assertTrue(line.endsWith(getPluginVersion()), "Arc should list as having an update: " + line);
                arc = true;
            } else if (line.contains(" JaxRS ")) {
                assertTrue(line.startsWith("update"), "JaxRS should list as having an update: " + line);
                assertTrue(line.endsWith(getPluginVersion()), "Arc should list as having an update: " + line);
                jaxrs = true;
            } else if (line.contains(" Bean Validation ")) {
                assertTrue(line.startsWith("   "), "Bean Validation should not list as anything: " + line);
                bean = true;
            }
        }

        assertTrue(agroal && arc && jaxrs && bean);
    }
}
