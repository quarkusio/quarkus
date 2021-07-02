package io.quarkus.maven.generator.handlers;

import org.apache.maven.project.MavenProject;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.generators.file.FileGenerator;
import io.quarkus.devtools.generators.kinds.service.ServiceGenerator;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

public class ServiceHandler {

    private final QuarkusProject quarkusProject;
    private final MessageWriter log;
    private final MavenProject mavenProject;

    public ServiceHandler(QuarkusProject quarkusProject, MessageWriter log, MavenProject mavenProject) {
        this.quarkusProject = quarkusProject;
        this.log = log;
        this.mavenProject = mavenProject;
    }

    public QuarkusCommandOutcome execute(String params) {
        if (validateParams(params)) {
            return QuarkusCommandOutcome.failure();
        }
        QuarkusCommandInvocation quarkusCommandInvocation = new QuarkusCommandInvocation(quarkusProject);
        FileGenerator fileGenerator = new FileGenerator(quarkusCommandInvocation, mavenProject);

        ServiceGenerator serviceGenerator = new ServiceGenerator("service.mustache", fileGenerator, mavenProject);
        serviceGenerator.generate(params);

        return QuarkusCommandOutcome.success();
    }

    private boolean validateParams(String params) {
        return params.isEmpty();
    }

}
