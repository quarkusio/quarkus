package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ExtensionCapabilities;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Derives build-cache inputs from a serialized {@link ApplicationModel} that do not depend on where
 * the project happens to be checked out.
 * <p>
 * The serialized model ({@code quarkus-app-model*.dat}) embeds absolute filesystem paths: the module
 * directories of the project itself and the resolved locations of every dependency inside the Gradle
 * dependency cache. Fingerprinting that file directly as an {@code @InputFile} therefore makes the
 * cache key a function of the checkout directory and of {@code GRADLE_USER_HOME}, so entries produced
 * in one working directory can never be reused from another. {@code @PathSensitive(RELATIVE)} does not
 * help, because it normalises the location of the input file, not its contents.
 * <p>
 * Instead the file is carried as an {@code @Internal} property and the semantically relevant content is
 * declared as properly normalised inputs: dependency jars as a {@code @Classpath} (content-hashed,
 * order-sensitive, location-independent) and the remaining coordinates, flags and capability metadata
 * as plain {@code @Input} values.
 * <p>
 * Both are returned as {@link Provider}s derived from the model file property: the model is only read
 * when Gradle actually computes the inputs, and the dependency on the task that produces the file is
 * carried along by the property itself.
 */
final class ApplicationModelCacheKey {

    private ApplicationModelCacheKey() {
    }

    /**
     * The resolved artifact files of every dependency in the model, to be declared as a
     * {@code @Classpath} input. Gradle hashes these by content, so they contribute the identity of the
     * dependencies without contributing the absolute locations they were resolved to.
     */
    static Provider<List<File>> resolvedDependencyFiles(RegularFileProperty modelFile) {
        return modelFile.map(file -> {
            var files = new ArrayList<File>();
            for (ResolvedDependency dependency : deserialize(file.getAsFile().toPath()).getDependencies()) {
                for (Path path : dependency.getResolvedPaths()) {
                    files.add(path.toFile());
                }
            }
            return files;
        });
    }

    /**
     * Everything else about the model that influences code generation and the build, expressed as
     * location-independent strings: dependency coordinates with their flags, the application artifact,
     * platform properties, extension capabilities and the artifact-key sets.
     * <p>
     * Workspace module directories are deliberately reduced to their module ids, since the directories
     * themselves are exactly the checkout-dependent part.
     */
    static Provider<Map<String, String>> relocatableProperties(RegularFileProperty modelFile) {
        return modelFile.map(file -> relocatableProperties(deserialize(file.getAsFile().toPath())));
    }

    private static Map<String, String> relocatableProperties(ApplicationModel model) {
        var props = new TreeMap<String, String>();

        props.put("app-artifact", coordinates(model.getAppArtifact()));

        var dependencies = new TreeSet<String>();
        for (ResolvedDependency dependency : model.getDependencies()) {
            // the flags carry deployment/runtime/reloadable classification, which changes behaviour
            dependencies.add(coordinates(dependency) + "/flags:" + dependency.getFlags());
        }
        props.put("dependencies", String.join(",", dependencies));

        var modules = new TreeSet<String>();
        for (WorkspaceModule module : model.getWorkspaceModules()) {
            // spelled out rather than WorkspaceModuleId.toString(): the interface does not define it
            var id = module.getId();
            modules.add(id.getGroupId() + ":" + id.getArtifactId() + ":" + id.getVersion());
        }
        props.put("workspace-modules", String.join(",", modules));

        props.put("platform-properties", new TreeMap<>(model.getPlatformProperties()).toString());

        // ExtensionCapabilities does not override toString(), so the contract has to be spelled out
        // explicitly here - relying on toString() would hash identity hash codes, which differ on
        // every JVM run and would make the key unstable even within a single working directory.
        var capabilities = new TreeSet<String>();
        for (ExtensionCapabilities capability : model.getExtensionCapabilities()) {
            capabilities.add(capability.getExtension()
                    + "/provides:" + new TreeSet<>(capability.getProvidesCapabilities())
                    + "/requires:" + new TreeSet<>(capability.getRequiresCapabilities()));
        }
        props.put("extension-capabilities", String.join(",", capabilities));

        props.put("parent-first", artifactKeys(model.getParentFirst()));
        props.put("runner-parent-first", artifactKeys(model.getRunnerParentFirst()));
        props.put("lower-priority", artifactKeys(model.getLowerPriorityArtifacts()));
        props.put("reloadable-workspace", artifactKeys(model.getReloadableWorkspaceDependencies()));

        var removedResources = new TreeMap<String, String>();
        model.getRemovedResources()
                .forEach((key, resources) -> removedResources.put(key.toString(), new TreeSet<>(resources).toString()));
        props.put("removed-resources", removedResources.toString());

        return props;
    }

    private static ApplicationModel deserialize(Path path) {
        try {
            return ToolingUtils.deserializeAppModel(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize the Quarkus application model from " + path, e);
        }
    }

    private static String artifactKeys(Set<ArtifactKey> keys) {
        var sorted = new TreeSet<String>();
        keys.forEach(key -> sorted.add(key.toString()));
        return String.join(",", sorted);
    }

    private static String coordinates(ResolvedDependency dependency) {
        return dependency.getGroupId()
                + ":" + dependency.getArtifactId()
                + ":" + dependency.getClassifier()
                + ":" + dependency.getType()
                + ":" + dependency.getVersion();
    }
}
