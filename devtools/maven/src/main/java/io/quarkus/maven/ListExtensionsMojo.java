package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

/**
 * List the available extensions.
 * You can add one or several extensions in one go, with the 2 following mojos:
 * {@code add-extensions} and {@code add-extension}.
 * You can list all extension or just installable. Choose between 3 output formats: name, concise and full.
 */
@Mojo(name = "list-extensions", requiresProject = false)
public class ListExtensionsMojo extends QuarkusProjectMojoBase {

    private static final String DEFAULT_FORMAT = "concise";

    /**
     * List all extensions or just the installable.
     */
    @Parameter(property = "all", defaultValue = "true")
    protected boolean all;

    /**
     * Select the output format among 'id' (display the artifactId only), 'concise' (display name and artifactId) and 'full'
     * (concise format and version related columns).
     */
    @Parameter(property = "format", defaultValue = DEFAULT_FORMAT)
    protected String format;

    /**
     * Search filter on extension list. The format is based on Java Pattern.
     */
    @Parameter(property = "searchPattern")
    protected String searchPattern;

    /**
     * Only list extensions from given category.
     */
    @Parameter(property = "category")
    protected String category;

    /**
     * List the already installed extensions
     */
    @Parameter(property = "installed", defaultValue = "false")
    protected boolean installed;

    @Override
    public void doExecute(final QuarkusProject quarkusProject, final MessageWriter log) throws MojoExecutionException {
        try {
            ListExtensions listExtensions = new ListExtensions(quarkusProject)
                    .all(all)
                    .format(format)
                    .search(searchPattern)
                    .category(category)
                    .installed(installed);
            listExtensions.execute();

            if (DEFAULT_FORMAT.equalsIgnoreCase(format)) {
                log.info("");
                log.info(ListExtensions.MORE_INFO_HINT, "-Dformat=full");
            }
            if (!installed && (category == null || category.isBlank())) {
                log.info("");
                log.info(ListExtensions.FILTER_HINT, "-Dcategory=\"categoryId\"");
            }
            log.info("");
            log.info(ListExtensions.ADD_EXTENSION_HINT,
                    "pom.xml", "./mvnw quarkus:add-extension -Dextensions=\"artifactId\"");

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to list extensions", e);
        }
    }
}
