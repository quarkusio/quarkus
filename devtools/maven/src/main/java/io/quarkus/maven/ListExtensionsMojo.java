package io.quarkus.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkus.cli.commands.ListExtensions;

/**
 * List the available extensions.
 * You can add one or several extensions in one go, with the 2 following mojos:
 * {@code add-extensions} and {@code add-extension}.
 * You can list all extension or just installable and choose simple or full format.
 */
@Mojo(name = "list-extensions", requiresProject = false)
public class ListExtensionsMojo extends AbstractMojo {

    /**
     * The Maven project which will define and configure the quarkus-maven-plugin
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    /**
     * List all extensions or just the installable.
     */
    @Parameter(property = "quarkus.extension.all", alias = "quarkus.extension.all", defaultValue = "true")
    protected boolean all;

    /**
     * Display in simplified format.
     */
    @Parameter(property = "quarkus.extension.format", alias = "quarkus.extension.format", defaultValue = "full")
    protected String format;

    /**
     * Search filter on extension list. The format is based on Java Pattern.
     */
    @Parameter(property = "searchPattern", alias = "quarkus.extension.searchPattern")
    protected String searchPattern;

    @Override
    public void execute() {
        new ListExtensions(project.getModel()).listExtensions(all, format, searchPattern);
    }
}
