package io.quarkus.gradle.tasks;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.registry.DefaultExtensionRegistry;

public class QuarkusAddExtension extends QuarkusPlatformTask {

    public QuarkusAddExtension() {
        super("Adds Quarkus extensions specified by the user to the project.");
    }

    private List<String> extensionsToAdd;
    private List<String> registries;

    @Option(option = "extensions", description = "Configures the extensions to be added.")
    public void setExtensionsToAdd(List<String> extensionsToAdd) {
        this.extensionsToAdd = extensionsToAdd;
    }

    @Input
    public List<String> getExtensionsToAdd() {
        return extensionsToAdd;
    }

    @Optional
    @Input
    public List<String> getRegistries() {
        return registries;
    }

    @Option(description = "The extension registry URLs to be used", option = "registry")
    public void setRegistries(List<String> registry) {
        this.registries = registry;
    }

    @TaskAction
    public void addExtension() {
        Set<String> extensionsSet = getExtensionsToAdd()
                .stream()
                .flatMap(ext -> stream(ext.split(",")))
                .map(String::trim)
                .collect(toSet());

        try {
            AddExtensions addExtensions = new AddExtensions(getQuarkusProject())
                    .extensions(extensionsSet);
            if (registries != null && !registries.isEmpty()) {
                List<URL> urls = registries.stream()
                        .map(QuarkusAddExtension::toURL)
                        .collect(toList());
                addExtensions.extensionRegistry(DefaultExtensionRegistry.fromURLs(urls));
            }
            addExtensions.execute();
        } catch (Exception e) {
            throw new GradleException("Failed to add extensions " + getExtensionsToAdd(), e);
        }
    }
}
