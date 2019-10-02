package io.quarkus.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkus.cli.commands.ListExtensions;
import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;

/**
 * List the available extensions.
 * You can add one or several extensions in one go, with the 2 following mojos:
 * {@code add-extensions} and {@code add-extension}.
 * You can list all extension or just installable. Choose between 3 output formats: name, concise and full.
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
     * Select the output format among 'name' (display the name only), 'concise' (display name and description) and 'full'
     * (concise format and version related columns).
     */
    @Parameter(property = "quarkus.extension.format", alias = "quarkus.extension.format", defaultValue = "concise")
    protected String format;

    /**
     * Search filter on extension list. The format is based on Java Pattern.
     */
    @Parameter(property = "searchPattern", alias = "quarkus.extension.searchPattern")
    protected String searchPattern;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            FileProjectWriter writer = null;
            BuildFile buildFile = null;
            // Even when we have no pom, the project is not null, but it's set to `org.apache.maven:standalone-pom:1`
            // So we need to also check for the project's file (the pom.xml file).
            if (project != null && project.getFile() != null) {
                writer = new FileProjectWriter(project.getBasedir());
                if (new File(project.getBasedir(), "build.gradle").exists()
                        || new File(project.getBasedir(), "build.gradle.kts").exists()) {
                    buildFile = new GradleBuildFile(writer);
                } else {
                    buildFile = new MavenBuildFile(writer);
                }
            }
            new ListExtensions(buildFile).listExtensions(all, format,
                    searchPattern);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to list extensions", e);
        }
    }
}
