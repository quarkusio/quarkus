package org.jboss.shamrock.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.jboss.shamrock.maven.components.dependencies.Extensions.addExtensions;

@Mojo(name = "add-extension", requiresProject = true)
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
        Model model = project.getOriginalModel().clone();
        if (addExtensions(model, extensions, getLog())) {
            File pomFile = project.getFile();
            save(pomFile, model);
        }
    }

    private void save(File pomFile, Model model) throws MojoExecutionException {
        MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
        try (FileWriter pomFileWriter = new FileWriter(pomFile)) {
            xpp3Writer.write(pomFileWriter, model);
            pomFileWriter.flush();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write the pom.xml file", e);
        }
    }

}
