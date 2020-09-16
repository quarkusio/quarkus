package io.quarkus.maven;

import static java.util.stream.Collectors.toSet;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.DefaultExtensionRegistry;

/**
 * Allow adding an extension to an existing pom.xml file.
 * Because you can add one or several extension in one go, there are 2 mojos:
 * {@code add-extensions} and {@code add-extension}. Both supports the {@code extension} and {@code extensions}
 * parameters.
 */
@Mojo(name = "add-extension")
public class AddExtensionMojo extends QuarkusProjectMojoBase {

    /**
     * The list of extensions to be added.
     */
    @Parameter(property = "extensions")
    Set<String> extensions;

    /**
     * For usability reason, this parameter allow adding a single extension.
     */
    @Parameter(property = "extension")
    String extension;

    /**
     * The URL where the registry is.
     */
    @Parameter(property = "registry", alias = "quarkus.extension.registry")
    List<URL> registries;

    @Override
    protected void validateParameters() throws MojoExecutionException {
        if ((StringUtils.isBlank(extension) && (extensions == null || extensions.isEmpty())) // None are set
                || (!StringUtils.isBlank(extension) && extensions != null && !extensions.isEmpty())) { // Both are set
            throw new MojoExecutionException("Either the `extension` or `extensions` parameter must be set");
        }
    }

    @Override
    public void doExecute(final QuarkusProject quarkusProject, final MessageWriter log)
            throws MojoExecutionException {
        Set<String> ext = new HashSet<>();
        if (extensions != null && !extensions.isEmpty()) {
            ext.addAll(extensions);
        } else {
            // Parse the "extension" just in case it contains several comma-separated values
            // https://github.com/quarkusio/quarkus/issues/2393
            ext.addAll(Arrays.stream(extension.split(",")).map(String::trim).collect(toSet()));
        }

        try {
            AddExtensions addExtensions = new AddExtensions(quarkusProject)
                    .extensions(ext.stream().map(String::trim).collect(toSet()));
            if (registries != null && !registries.isEmpty()) {
                addExtensions.extensionRegistry(DefaultExtensionRegistry.fromURLs(registries));
            }
            final QuarkusCommandOutcome outcome = addExtensions.execute();
            if (!outcome.isSuccess()) {
                throw new MojoExecutionException("Unable to add extensions");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to update the pom.xml file", e);
        }
    }
}
