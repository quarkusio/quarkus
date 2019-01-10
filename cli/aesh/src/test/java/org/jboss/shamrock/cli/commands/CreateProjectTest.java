package org.jboss.shamrock.cli.commands;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.Assert.assertFalse;

public class CreateProjectTest {
    @Test
    public void create() throws IOException {
        final File file = new File("target/basic-rest");
        delete(file);
        final CreateProject createProject = new CreateProject(file, "org.jboss.shamrock",
            "basic-rest", "1.0.0-SNAPSHOT");

        Assert.assertTrue(createProject.doCreateProject());
    }

    private void delete(final File file) throws IOException {

        if (file.exists()) {
            Files.walk(file.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }

        assertFalse("Directory still exists",
            Files.exists(file.toPath()));
    }
}