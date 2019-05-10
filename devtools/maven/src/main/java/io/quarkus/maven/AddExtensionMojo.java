package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkus.cli.commands.AddExtensions;
import io.quarkus.cli.commands.writer.FileProjectWriter;

/**
 * Allow adding an extension to an existing pom.xml file.
 * Because you can add one or several extension in one go, there are 2 mojos:
 * {@code add-extensions} and {@code add-extension}. Both supports the {@code extension} and {@code extensions}
 * parameters. Extension must be identified by artifactId with or without the "quarkus-" prefix or by a full or partial GAV.
 */
@Mojo(name = "add-extension")
public class AddExtensionMojo extends AbstractMojo {

    /**
     * The Maven project which will define and configure the quarkus-maven-plugin
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

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
    public void execute() throws MojoExecutionException {
        if ((StringUtils.isBlank(extension) && (extensions == null || extensions.isEmpty())) // None are set
                || (!StringUtils.isBlank(extension) && extensions != null && !extensions.isEmpty())) { // Both are set
            throw new MojoExecutionException("Either the `extension` or `extensions` parameter must be set");
        }

        Set<String> ext = new HashSet<>();
        if (extensions != null && !extensions.isEmpty()) {
            ext.addAll(extensions);
        } else {
            ext.add(extension);
        }

        try {
            Model model = project.getOriginalModel().clone();
            File pomFile = new File(model.getPomFile().getAbsolutePath());
            new AddExtensions(new FileProjectWriter(pomFile.getParentFile()), pomFile.getName())
                    .addExtensions(ext.stream().map(String::trim).collect(Collectors.toSet()));
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to update the pom.xml file", e);
        }
    }
}
