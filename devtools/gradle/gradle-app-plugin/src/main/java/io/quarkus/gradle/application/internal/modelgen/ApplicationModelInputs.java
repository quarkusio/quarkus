package io.quarkus.gradle.application.internal.modelgen;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, Gradle-free inputs for application-model assembly.
 */
record ApplicationModelInputs(
        WorkspaceSnapshot workspace,
        ClasspathSnapshot applicationClasspath,
        ClasspathSnapshot deploymentClasspath,
        ClasspathSnapshot compileOnlyClasspath,
        List<PlatformArtifactSnapshot> platformArtifacts,
        LocalOutputSnapshot localOutputs,
        boolean reloadableWorkspaceDependencies) {

    ApplicationModelInputs {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(applicationClasspath, "applicationClasspath");
        Objects.requireNonNull(deploymentClasspath, "deploymentClasspath");
        Objects.requireNonNull(compileOnlyClasspath, "compileOnlyClasspath");
        platformArtifacts = List.copyOf(platformArtifacts);
        Objects.requireNonNull(localOutputs, "localOutputs");
    }

    record WorkspaceSnapshot(
            Path moduleDirectory,
            Path buildDirectory,
            Path buildFile,
            List<Path> applicationClassesDirectories,
            List<Path> applicationResourcesDirectories,
            List<Path> applicationSourceDirectories,
            List<Path> applicationResourceSourceDirectories,
            List<Path> testClassesDirectories,
            List<Path> testResourcesDirectories,
            List<Path> testSourceDirectories,
            List<Path> testResourceSourceDirectories) {

        WorkspaceSnapshot {
            Objects.requireNonNull(moduleDirectory, "moduleDirectory");
            Objects.requireNonNull(buildDirectory, "buildDirectory");
            Objects.requireNonNull(buildFile, "buildFile");
            applicationClassesDirectories = List.copyOf(applicationClassesDirectories);
            applicationResourcesDirectories = List.copyOf(applicationResourcesDirectories);
            applicationSourceDirectories = List.copyOf(applicationSourceDirectories);
            applicationResourceSourceDirectories = List.copyOf(applicationResourceSourceDirectories);
            testClassesDirectories = List.copyOf(testClassesDirectories);
            testResourcesDirectories = List.copyOf(testResourcesDirectories);
            testSourceDirectories = List.copyOf(testSourceDirectories);
            testResourceSourceDirectories = List.copyOf(testResourceSourceDirectories);
        }
    }

    record ClasspathSnapshot(
            ModuleCoordinates rootCoordinates,
            List<ComponentKey> rootDependencies,
            Map<ComponentKey, ComponentSnapshot> components,
            List<Path> allResolvedFiles) {

        ClasspathSnapshot {
            Objects.requireNonNull(rootCoordinates, "rootCoordinates");
            rootDependencies = List.copyOf(rootDependencies);
            components = immutableMap(components);
            allResolvedFiles = List.copyOf(allResolvedFiles);
        }

        ComponentSnapshot component(ComponentKey key) {
            return Objects.requireNonNull(components.get(key), () -> "No component snapshot for " + key);
        }
    }

    record ComponentKey(int value) {
    }

    record ComponentSnapshot(
            ComponentKey key,
            ModuleCoordinates coordinates,
            List<ComponentKey> dependencies,
            List<ArtifactSnapshot> artifacts) {

        ComponentSnapshot {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(coordinates, "coordinates");
            dependencies = List.copyOf(dependencies);
            artifacts = List.copyOf(artifacts);
        }
    }

    record ModuleCoordinates(String group, String name, String version) {

        ModuleCoordinates {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(version, "version");
        }
    }

    record ArtifactSnapshot(Path path, String type) {

        ArtifactSnapshot {
            Objects.requireNonNull(path, "path");
        }
    }

    record PlatformArtifactSnapshot(String group, String name, String version, Path path) {

        PlatformArtifactSnapshot {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(path, "path");
        }
    }

    record LocalOutputKey(ComponentKey component, String classifier) {

        LocalOutputKey {
            Objects.requireNonNull(component, "component");
            Objects.requireNonNull(classifier, "classifier");
        }
    }

    record LocalOutputSnapshot(Map<LocalOutputKey, List<Path>> pathsByVariant) {

        LocalOutputSnapshot {
            var immutablePaths = new LinkedHashMap<LocalOutputKey, List<Path>>(pathsByVariant.size());
            pathsByVariant.forEach((key, value) -> immutablePaths.put(key, List.copyOf(value)));
            pathsByVariant = Collections.unmodifiableMap(immutablePaths);
        }

        List<Path> get(ComponentKey component, String classifier) {
            return pathsByVariant.get(new LocalOutputKey(component, classifier));
        }

        boolean hasComponent(ComponentKey component) {
            for (LocalOutputKey key : pathsByVariant.keySet()) {
                if (key.component().equals(component)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
