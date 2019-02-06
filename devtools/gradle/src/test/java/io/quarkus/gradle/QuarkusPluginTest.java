package io.quarkus.gradle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

public class QuarkusPluginTest {

    @Test
    public void quarkusTest() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("io.quarkus.gradle.plugin");

        assertTrue(project.getPluginManager().hasPlugin("io.quarkus.gradle.plugin"));

        assertNotNull(project.getTasks().getByName("quarkus-build"));
    }

}
