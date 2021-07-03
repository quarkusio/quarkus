package io.quarkus.maven.generator;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.QuarkusProjectMojoBase;
import io.quarkus.maven.generator.handlers.*;

@Mojo(name = "scaffold")
public class ScaffoldMojo extends QuarkusProjectMojoBase {

    @Parameter(property = "params", alias = "p")
    String params;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject mavenProject;

    protected void doExecute(QuarkusProject quarkusProject, MessageWriter log) {
        if (validateParams(log))
            return;

        QuarkusCommandInvocation quarkusCommandInvocation = new QuarkusCommandInvocation(quarkusProject);
        addDependencies(quarkusProject, quarkusCommandInvocation);

        new ModelHandler(quarkusProject, log, mavenProject).execute(params, true);
        new RepositoryHandler(quarkusProject, log, mavenProject).execute(params.split(" ")[0]);
        new ServiceHandler(quarkusProject, log, mavenProject).execute(params.split(" ")[0]);
        new ResourceHandler(quarkusProject, log, mavenProject).execute(params.split(" ")[0]);
        new ExceptionHandler(quarkusProject, log, mavenProject).execute("");
    }

    private void addDependencies(QuarkusProject quarkusProject, QuarkusCommandInvocation quarkusCommandInvocation) {
        quarkusCommandInvocation.log().info(MessageIcons.NOOP_ICON + " Analyzing dependencies");

        String extension = "hibernate-orm-panache, hibernate-validator, resteasy, resteasy-jsonb, agroal, jdbc-postgresql, smallrye-health, smallrye-openapi";

        Set<String> ext = new HashSet<>(Arrays.stream(extension.split(",")).map(String::trim).collect(toSet()));
        AddExtensions addExtensions = new AddExtensions(quarkusProject)
                .extensions(ext.stream().map(String::trim).collect(toSet()));
        try {
            addExtensions.execute();
        } catch (QuarkusCommandException e) {
            quarkusCommandInvocation.log().error("Error: {}", e.getMessage());
        }
    }

    private boolean validateParams(MessageWriter log) {
        if (params.isEmpty()) {
            log.error("params can not be null. ex: scaffold -p");
            return true;
        }
        return false;
    }

}
