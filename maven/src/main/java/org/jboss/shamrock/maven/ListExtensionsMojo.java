package org.jboss.shamrock.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.jboss.shamrock.maven.components.dependencies.Extensions;

@Mojo(name = "list-extensions", requiresProject = false)
public class ListExtensionsMojo extends AbstractMojo {

    @Component
    Extensions extensions;

    @Override
    public void execute() {
        getLog().info("Available extensions:");
        extensions.get().stream()
                .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                .forEach(ext -> getLog().info("\t * " + ext.getName() + " (" + ext.getGroupId() + ":" + ext.getArtifactId() + ")"));

        getLog().info("\nAdd an extension to your project by adding the dependency to your " +
                "project or use `mvn shamrock:add-extension -Dextensions=\"name\"`");
    }
}
