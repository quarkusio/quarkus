package io.quarkus.cli.commands;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.commands.writer.FileWriter;
import io.quarkus.maven.utilities.MojoUtils;

public class AddExtensionsTest {
    @Test
    public void addExtension() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(new FileWriter(pom.getParentFile()))
                .groupId("org.acme")
                .artifactId("add-extension-test")
                .version("0.0.1-SNAPSHOT")
                .doCreateProject(new HashMap<>());

        File pomFile = new File(pom.getAbsolutePath());
        new AddExtensions(new FileWriter(pomFile.getParentFile()), pomFile.getName())
                .addExtensions(new HashSet<>(asList("agroal", "arc", " hibernate-validator")));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "quarkus-agroal");
        hasDependency(model, "quarkus-arc");
        hasDependency(model, "quarkus-hibernate-validator");
    }

    private void hasDependency(final Model model, final String artifactId) {
        Assertions.assertTrue(model.getDependencies()
                .stream()
                .anyMatch(d -> d.getGroupId().equals(MojoUtils.getPluginGroupId()) &&
                        d.getArtifactId().equals(artifactId)));
    }
}
