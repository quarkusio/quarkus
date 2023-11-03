package io.quarkus.gradle.extension;

import static io.quarkus.gradle.QuarkusPlugin.EXTENSION_NAME;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

public class QuarkusExtensionTest {
    @Test
    public void extensionInstantiates() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("java");

        QuarkusPluginExtension extension = project.getExtensions().create(EXTENSION_NAME, QuarkusPluginExtension.class,
                project);
    }
}
