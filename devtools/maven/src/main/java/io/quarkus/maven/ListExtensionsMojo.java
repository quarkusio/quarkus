package io.quarkus.maven;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.cli.commands.ListExtensions;
import io.quarkus.cli.commands.file.BuildFile;

/**
 * List the available extensions.
 * You can add one or several extensions in one go, with the 2 following mojos:
 * {@code add-extensions} and {@code add-extension}.
 * You can list all extension or just installable. Choose between 3 output formats: name, concise and full.
 */
@Mojo(name = "list-extensions", requiresProject = false)
public class ListExtensionsMojo extends BuildFileMojoBase {

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
    public void doExecute(BuildFile buildFile) throws MojoExecutionException {
        try {
            new ListExtensions(buildFile).listExtensions(all, format, searchPattern);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to list extensions", e);
        }
    }
}
