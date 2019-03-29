package io.quarkus.maven;

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
            new AddExtensions(model.getPomFile())
                    .addExtensions(ext.stream().map(String::trim).collect(Collectors.toSet()));
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to update the pom.xml file", e);
        }
    }
}
