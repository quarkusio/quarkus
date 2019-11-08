package io.quarkus.maven;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.cli.commands.AddExtensionResult;
import io.quarkus.cli.commands.AddExtensions;
import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.generators.BuildTool;

/**
 * Allow adding an extension to an existing pom.xml file.
 * Because you can add one or several extension in one go, there are 2 mojos:
 * {@code add-extensions} and {@code add-extension}. Both supports the {@code extension} and {@code extensions}
 * parameters.
 */
@Mojo(name = "add-extension")
public class AddExtensionMojo extends BuildFileMojoBase {

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

    @Override
    protected void validateParameters() throws MojoExecutionException {
        if ((StringUtils.isBlank(extension) && (extensions == null || extensions.isEmpty())) // None are set
                || (!StringUtils.isBlank(extension) && extensions != null && !extensions.isEmpty())) { // Both are set
            throw new MojoExecutionException("Either the `extension` or `extensions` parameter must be set");
        }
    }

    @Override
    public void doExecute(BuildFile buildFile) throws MojoExecutionException {

        if (buildFile == null) {
            try {
                buildFile = BuildTool.MAVEN.createBuildFile(new FileProjectWriter(project.getBasedir()));
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to initialize the project's build descriptor", e);
            }
        }
        Set<String> ext = new HashSet<>();
        if (extensions != null && !extensions.isEmpty()) {
            ext.addAll(extensions);
        } else {
            // Parse the "extension" just in case it contains several comma-separated values
            // https://github.com/quarkusio/quarkus/issues/2393
            ext.addAll(Arrays.stream(extension.split(",")).map(s -> s.trim()).collect(Collectors.toSet()));
        }

        try {
            final AddExtensionResult result = new AddExtensions(buildFile)
                    .addExtensions(ext.stream().map(String::trim).collect(Collectors.toSet()));
            if (!result.succeeded()) {
                throw new MojoExecutionException("Unable to add extensions");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to update the pom.xml file", e);
        }
    }
}
