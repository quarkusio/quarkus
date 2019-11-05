package io.quarkus.gradle.tasks;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.cli.commands.AddExtensions;
import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
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
        setupPlatformDescriptor();

        Set<String> extensionsSet = getExtensionsToAdd()
                .stream()
                .flatMap(ext -> stream(ext.split(",")))
                .map(String::trim)
                .collect(toSet());
        try {
            new AddExtensions(new GradleBuildFile(new FileProjectWriter(getProject().getProjectDir())))
                    .addExtensions(extensionsSet);
        } catch (IOException e) {
            throw new GradleException("Failed to add extensions " + getExtensionsToAdd(), e);
        }
    }
}
