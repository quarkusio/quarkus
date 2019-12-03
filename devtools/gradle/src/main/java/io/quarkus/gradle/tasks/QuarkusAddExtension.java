package io.quarkus.gradle.tasks;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.cli.commands.AddExtensions;
import io.quarkus.cli.commands.QuarkusCommandInvocation;
import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;

public class QuarkusAddExtension extends QuarkusPlatformTask {

    public QuarkusAddExtension() {
        super("Adds Quarkus extensions specified by the user to the project.");
    }

    private List<String> extensionsToAdd;

    @Option(option = "extensions", description = "Configures the extensions to be added.")
    public void setExtensionsToAdd(List<String> extensionsToAdd) {
        this.extensionsToAdd = extensionsToAdd;
    }

    @Input
    public List<String> getExtensionsToAdd() {
        return extensionsToAdd;
    }

    @TaskAction
    public void addExtension() {
        execute();
    }

    @Override
    protected void doExecute(QuarkusCommandInvocation invocation) {
        Set<String> extensionsSet = getExtensionsToAdd()
                .stream()
                .flatMap(ext -> stream(ext.split(",")))
                .map(String::trim)
                .collect(toSet());
        invocation.setValue(AddExtensions.EXTENSIONS, extensionsSet);
        try {
            new AddExtensions(new GradleBuildFile(new FileProjectWriter(getProject().getProjectDir())))
                    .execute(invocation);
        } catch (Exception e) {
            throw new GradleException("Failed to add extensions " + getExtensionsToAdd(), e);
        }
    }
}
