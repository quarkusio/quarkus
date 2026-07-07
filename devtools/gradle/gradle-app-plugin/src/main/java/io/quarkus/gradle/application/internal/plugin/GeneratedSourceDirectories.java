package io.quarkus.gradle.application.internal.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;

/**
 * Resolves the source root contributed by each Quarkus code generation provider.
 */
final class GeneratedSourceDirectories {

    private GeneratedSourceDirectories() {
    }

    static Provider<List<File>> from(Provider<Directory> generatedOutputDirectory) {
        return generatedOutputDirectory.map(directory -> resolve(directory.getAsFile()));
    }

    static Provider<List<File>> fromConfiguredProviders(Provider<Directory> generatedOutputDirectory,
            Provider<List<String>> codegenProviders) {
        return generatedOutputDirectory.flatMap(directory -> codegenProviders.map(providers -> providers.stream()
                .map(provider -> new File(directory.getAsFile(), provider))
                .toList()));
    }

    private static List<File> resolve(File root) {
        File[] providerDirectories = root
                .listFiles(file -> file.isDirectory() && !file.getName().startsWith("."));
        if (providerDirectories == null || providerDirectories.length == 0) {
            return List.of(root);
        }
        Arrays.sort(providerDirectories);
        return Arrays.asList(providerDirectories);
    }
}
