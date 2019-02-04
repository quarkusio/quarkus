package org.jboss.shamrock.cli.commands;

import org.apache.maven.model.Model;
import org.jboss.shamrock.maven.utilities.MojoUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static java.util.Arrays.asList;

public class AddExtensionsTest {
    @Test
    public void addExtension() throws IOException {
        final File pom = new File("target/extensions-test", "pom.xml");

        CreateProjectTest.delete(pom.getParentFile());
        new CreateProject(pom.getParentFile())
            .groupId("org.acme")
            .artifactId("add-extension-test")
            .version("0.0.1-SNAPSHOT")
            .doCreateProject(new HashMap<>());

        new AddExtensions(pom)
            .addExtensions(asList("agroal", "arc", "hibernate-validator"));

        Model model = MojoUtils.readPom(pom);
        hasDependency(model, "shamrock-agroal-deployment");
        hasDependency(model, "shamrock-arc-deployment");
        hasDependency(model, "shamrock-hibernate-validator-deployment");
    }

    private void hasDependency(final Model model, final String artifactId) {
        Assertions.assertTrue(model.getDependencies()
                               .stream()
                               .anyMatch(d -> d.getGroupId().equals(MojoUtils.getPluginGroupId()) &&
                                              d.getArtifactId().equals(artifactId)));
    }
}