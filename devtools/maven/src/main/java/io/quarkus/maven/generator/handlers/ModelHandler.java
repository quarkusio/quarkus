package io.quarkus.maven.generator.handlers;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;

import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.generators.kinds.model.ModelGenerator;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

public class ModelHandler {

    private final QuarkusProject quarkusProject;
    private final MessageWriter log;
    private final MavenProject mavenProject;

    public ModelHandler(QuarkusProject quarkusProject, MessageWriter log, MavenProject mavenProject) {
        this.quarkusProject = quarkusProject;
        this.log = log;
        this.mavenProject = mavenProject;
    }

    public QuarkusCommandOutcome execute(String params, Boolean skipDeps) {
        if (validateParams(params)) {
            return QuarkusCommandOutcome.failure();
        }

        QuarkusCommandInvocation quarkusCommandInvocation = new QuarkusCommandInvocation(quarkusProject);
        if (skipDeps) {
            addDependencies(quarkusProject, quarkusCommandInvocation);
        }

        ModelGenerator modelGenerator = new ModelGenerator("model.mustache", quarkusCommandInvocation, mavenProject);
        modelGenerator.generate(params);

        return QuarkusCommandOutcome.success();
    }

    private void addDependencies(QuarkusProject quarkusProject, QuarkusCommandInvocation quarkusCommandInvocation) {
        quarkusCommandInvocation.log().info(MessageIcons.NOOP_ICON + " Analyzing dependencies");

        String extension = "hibernate-orm-panache, hibernate-validator";

        Set<String> ext = new HashSet<>(Arrays.stream(extension.split(",")).map(String::trim).collect(toSet()));
        AddExtensions addExtensions = new AddExtensions(quarkusProject)
                .extensions(ext.stream().map(String::trim).collect(toSet()));
        try {
            addExtensions.execute();
        } catch (QuarkusCommandException e) {
            quarkusCommandInvocation.log().error("Error: {}", e.getMessage());
        }
    }

    private boolean validateParams(String params) {
        if (params.isEmpty()) {
            log.error("params can not be null. ex: scaffold -p");
            return true;
        }
        return false;
    }

}
