package io.quarkus.maven;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Parameter(property = "extensions")
    private Set<String> extensions;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Model model = project.getOriginalModel().clone();

            new AddExtensions(model.getPomFile())
                    .addExtensions(extensions.stream().map(String::trim).collect(Collectors.toSet()));
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to update the pom.xml file", e);
        }
    }
}
