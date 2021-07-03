package io.quarkus.maven.generator;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.QuarkusProjectMojoBase;
import io.quarkus.maven.generator.handlers.ModelHandler;

@Mojo(name = "model")
public class ModelMojo extends QuarkusProjectMojoBase {

    @Parameter(property = "params", alias = "p")
    String params;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject mavenProject;

    @Override
    protected void doExecute(QuarkusProject quarkusProject, MessageWriter log) {
        ModelHandler modelHandler = new ModelHandler(quarkusProject, log, mavenProject);
        modelHandler.execute(params, false);
    }
}
