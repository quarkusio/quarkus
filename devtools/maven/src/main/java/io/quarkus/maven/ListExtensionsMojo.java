package io.quarkus.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;

import io.quarkus.cli.commands.AddExtensions;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * List of the available extensions.
 * You can add one or several extension in one go, with the 2 following mojos:
 * {@code add-extensions} and {@code add-extension}.
 */
@Mojo(name = "list-extensions", requiresProject = false)
public class ListExtensionsMojo extends AbstractMojo {

    @Override
    public void execute() {
        getLog().info("Available extensions:");
        MojoUtils.loadExtensions().stream()
                .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                .forEach(ext -> getLog()
                        .info("\t * " + ext.getName() + " (" + ext.getGroupId() + ":" + ext.getArtifactId() + ")"));

        getLog().info("\nAdd an extension to your project by adding the dependency to your " +
                "project or use `mvn quarkus:add-extension -Dextensions=\"name\"`");
    }
}
