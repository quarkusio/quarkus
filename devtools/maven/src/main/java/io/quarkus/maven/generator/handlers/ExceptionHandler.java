package io.quarkus.maven.generator.handlers;

import org.apache.maven.project.MavenProject;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.generators.file.FileGenerator;
import io.quarkus.devtools.generators.kinds.exception.AppExceptionGenerator;
import io.quarkus.devtools.generators.kinds.exception.ExceptionHandlerGenerator;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

public class ExceptionHandler {

    private final QuarkusProject quarkusProject;
    private final MessageWriter log;
    private final MavenProject mavenProject;

    public ExceptionHandler(QuarkusProject quarkusProject, MessageWriter log, MavenProject mavenProject) {
        this.quarkusProject = quarkusProject;
        this.log = log;
        this.mavenProject = mavenProject;
    }

    public QuarkusCommandOutcome execute(String params) {
        QuarkusCommandInvocation quarkusCommandInvocation = new QuarkusCommandInvocation(quarkusProject);
        FileGenerator fileGenerator = new FileGenerator(quarkusCommandInvocation, mavenProject);

        AppExceptionGenerator appExceptionGenerator = new AppExceptionGenerator("app-exception.mustache", fileGenerator,
                mavenProject);
        appExceptionGenerator.generate(params);

        ExceptionHandlerGenerator exceptionHandlerGenerator = new ExceptionHandlerGenerator("exception-handler.mustache",
                fileGenerator, mavenProject);
        exceptionHandlerGenerator.generate(params);

        return QuarkusCommandOutcome.success();
    }
}
