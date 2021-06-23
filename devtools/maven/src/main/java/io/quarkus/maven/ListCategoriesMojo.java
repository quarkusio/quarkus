package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.commands.ListCategories;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

/**
 * List extension categories, which a user can use to filter extensions.
 */
@Mojo(name = "list-categories", requiresProject = false)
public class ListCategoriesMojo extends QuarkusProjectMojoBase {

    private static final String DEFAULT_FORMAT = "concise";

    /**
     * Select the output format among 'name' (display the name only) and 'full'
     * (includes a verbose name and a description).
     */
    @Parameter(property = "format", defaultValue = DEFAULT_FORMAT)
    protected String format;

    @Override
    public void doExecute(final QuarkusProject quarkusProject, final MessageWriter log) throws MojoExecutionException {
        try {
            ListCategories listExtensions = new ListCategories(quarkusProject)
                    .format(format);
            listExtensions.execute();

            if (DEFAULT_FORMAT.equalsIgnoreCase(format)) {
                log.info("");
                log.info(ListCategories.MORE_INFO_HINT, "-Dformat=full");
            }
            log.info("");
            log.info(ListCategories.LIST_EXTENSIONS_HINT,
                    "`./mvnw quarkus:list-extensions -Dcategory=\"categoryId\"`");

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to list extension categories", e);
        }
    }
}
