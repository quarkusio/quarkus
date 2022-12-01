package io.quarkus.gradle.tasks;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.RemoveExtensions;

public class QuarkusRemoveExtension extends QuarkusPlatformTask {

    public QuarkusRemoveExtension() {
        super("Removes Quarkus extensions specified by the user to the project.");
    }

    private List<String> extensionsToRemove;

    @Option(option = "extensions", description = "Configures the extensions to be removed.")
    public void setExtensionsToRemove(List<String> extensionsToRemove) {
        this.extensionsToRemove = extensionsToRemove;
    }

    @Input
    public List<String> getExtensionsToRemove() {
        return extensionsToRemove;
    }

    @TaskAction
    public void removeExtension() {
        Set<String> extensionsSet = getExtensionsToRemove()
                .stream()
                .flatMap(ext -> stream(ext.split(",")))
                .map(String::trim)
                .collect(toSet());
        try {
            new RemoveExtensions(getQuarkusProject(true))
                    .extensions(extensionsSet)
                    .execute();
        } catch (Exception e) {
            throw new GradleException("Failed to remove extensions " + getExtensionsToRemove(), e);
        }
    }
}
