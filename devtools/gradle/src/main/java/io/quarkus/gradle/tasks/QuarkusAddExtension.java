package io.quarkus.gradle.tasks;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.AddExtensions;

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
        Set<String> extensionsSet = getExtensionsToAdd()
                .stream()
                .flatMap(ext -> stream(ext.split(",")))
                .map(String::trim)
                .collect(toSet());

        try {
            AddExtensions addExtensions = new AddExtensions(getQuarkusProject(false))
                    .extensions(extensionsSet);
            addExtensions.execute();
        } catch (Exception e) {
            throw new GradleException("Failed to add extensions " + getExtensionsToAdd(), e);
        }
    }
}
