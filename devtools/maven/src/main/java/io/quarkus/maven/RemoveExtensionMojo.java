package io.quarkus.maven;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.commands.RemoveExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

/**
 * Allow removing an extension from an existing pom.xml file.
 * Because you can remove one or several extension in one go, there are 2 mojos:
 * {@code remove-extensions} and {@code remove-extension}. Both supports the {@code extension} and {@code extensions}
 * parameters.
 */
@Mojo(name = "remove-extension")
public class RemoveExtensionMojo extends QuarkusProjectMojoBase {

    /**
     * The list of extensions to be removed.
     */
    @Parameter(property = "extensions")
    Set<String> extensions;

    /**
     * For usability reason, this parameter allow removing a single extension.
     */
    @Parameter(property = "extension")
    String extension;

    @Override
    protected void validateParameters() throws MojoExecutionException {
        if ((StringUtils.isBlank(extension) && (extensions == null || extensions.isEmpty())) // None are set
                || (!StringUtils.isBlank(extension) && extensions != null && !extensions.isEmpty())) { // Both are set
            throw new MojoExecutionException("Either the `extension` or `extensions` parameter must be set");
        }
    }

    @Override
    public void doExecute(final QuarkusProject quarkusProject, final MessageWriter log) throws MojoExecutionException {

        Set<String> ext = new HashSet<>();
        if (extensions != null && !extensions.isEmpty()) {
            ext.addAll(extensions);
        } else {
            // Parse the "extension" just in case it contains several comma-separated values
            // https://github.com/quarkusio/quarkus/issues/2393
            ext.addAll(Arrays.stream(extension.split(",")).map(s -> s.trim()).collect(Collectors.toSet()));
        }

        try {
            final QuarkusCommandOutcome outcome = new RemoveExtensions(quarkusProject)
                    .extensions(ext.stream().map(String::trim).collect(Collectors.toSet()))
                    .execute();
            if (!outcome.isSuccess()) {
                throw new MojoExecutionException("Unable to remove extensions");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to update the pom.xml file", e);
        }
    }
}
