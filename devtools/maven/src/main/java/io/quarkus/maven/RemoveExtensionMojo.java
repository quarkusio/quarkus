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

import io.quarkus.cli.commands.QuarkusCommandOutcome;
import io.quarkus.cli.commands.RemoveExtensions;
import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.MessageWriter;

/**
 * Allow removing an extension from an existing pom.xml file.
 * Because you can remove one or several extension in one go, there are 2 mojos:
 * {@code remove-extensions} and {@code remove-extension}. Both supports the {@code extension} and {@code extensions}
 * parameters.
 */
@Mojo(name = "remove-extension")
public class RemoveExtensionMojo extends BuildFileMojoBase {

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
    public void doExecute(BuildFile buildFile, QuarkusPlatformDescriptor platformDescr, MessageWriter log)
            throws MojoExecutionException {

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
            final QuarkusCommandOutcome outcome = new RemoveExtensions(buildFile, platformDescr)
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
