package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.commands.ListPlatforms;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.Constants;

/**
 * List imported and optionally other platforms available for the project.
 */
@Mojo(name = "list-platforms", requiresProject = false)
public class ListPlatformsMojo extends QuarkusProjectMojoBase {

    /**
     * List the already installed extensions
     */
    @Parameter(property = "installed", defaultValue = "false")
    protected boolean installed;

    @Override
    public void doExecute(final QuarkusProject quarkusProject, final MessageWriter log) throws MojoExecutionException {
        if (installed) {
            getImportedPlatforms().forEach(coords -> {
                final StringBuilder buf = new StringBuilder();
                buf.append(coords.getGroupId()).append(":")
                        .append(coords.getArtifactId().substring(0,
                                coords.getArtifactId().length() - Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX.length()))
                        .append("::pom:").append(coords.getVersion());
                log.info(buf.toString());
            });
            return;
        }
        try {
            new ListPlatforms(quarkusProject).execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to list platforms", e);
        }
    }
}
