package org.jboss.shamrock.maven;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jboss.shamrock.cli.commands.AddExtensions;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.io.IOException;
import java.util.List;

@Mojo(name = "add-extension")
public class AddExtensionMojo extends AbstractMojo {

    /**
     * The Maven project which will define and configure the shamrock-maven-plugin
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "extensions")
    private List<String> extensions;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Model model = project.getOriginalModel().clone();

            new AddExtensions(model.getPomFile())
                .addExtensions(extensions);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to update the pom.xml file", e);
        }
    }
}
