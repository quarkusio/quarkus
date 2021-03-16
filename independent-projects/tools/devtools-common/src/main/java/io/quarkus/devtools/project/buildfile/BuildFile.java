package io.quarkus.devtools.project.buildfile;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.project.extensions.ExtensionInstallPlan;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BuildFile implements ExtensionManager {

    private final Path projectDirPath;
    private final ExtensionCatalog catalog;

    public BuildFile(final Path projectDirPath, ExtensionCatalog catalog) {
        this.projectDirPath = requireNonNull(projectDirPath, "projectPath is required");
        this.catalog = requireNonNull(catalog, "catalog is required");
    }

    @Override
    public final InstallResult install(Collection<AppArtifactCoords> coords) throws IOException {
        this.refreshData();
        final Collection<AppArtifactCoords> installed = withoutAlreadyInstalled(coords);
        installed.forEach(e -> addDependency(e, e.getVersion() == null));
        this.writeToDisk();
        return new InstallResult(installed);
    }

    @Override
    public InstallResult install(ExtensionInstallPlan plan) throws IOException {
        List<AppArtifactCoords> installed = new ArrayList<>();
        for (AppArtifactCoords platform : withoutAlreadyInstalled(plan.getPlatforms())) {
            if (addDependency(platform, false)) {
                installed.add(platform);
            }
        }
        for (AppArtifactCoords managedExtension : withoutAlreadyInstalled(plan.getManagedExtensions())) {
            if (addDependency(managedExtension, true)) {
                installed.add(managedExtension);
            }
        }
        for (AppArtifactCoords independentExtension : withoutAlreadyInstalled(plan.getIndependentExtensions())) {
            if (addDependency(independentExtension, false)) {
                installed.add(independentExtension);
            }
        }
        writeToDisk();
        return new InstallResult(installed);
    }

    @Override
    public final Collection<AppArtifactCoords> getInstalled() throws IOException {
        this.refreshData();
        return this.getDependencies().stream()
                .filter(d -> this.isQuarkusExtension(d.getKey()))
                .collect(toList());
    }

    @Override
    public final UninstallResult uninstall(Collection<AppArtifactKey> keys) throws IOException {
        this.refreshData();
        final Set<AppArtifactKey> existingKeys = getDependenciesKeys();
        final List<AppArtifactKey> uninstalled = keys.stream()
                .distinct()
                .filter(existingKeys::contains)
                .filter(k -> {
                    try {
                        removeDependency(k);
                        return true;
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }).collect(toList());
        this.writeToDisk();
        return new UninstallResult(uninstalled);
    }

    private Collection<AppArtifactCoords> withoutAlreadyInstalled(Collection<AppArtifactCoords> extensions) throws IOException {
        final Set<AppArtifactKey> existingKeys = getDependenciesKeys();
        return extensions.stream()
                .distinct()
                .filter(a -> !existingKeys.contains(a.getKey()))
                .collect(toList());
    }

    protected abstract boolean addDependency(AppArtifactCoords coords, boolean managed);

    protected abstract void removeDependency(AppArtifactKey key) throws IOException;

    protected abstract List<AppArtifactCoords> getDependencies() throws IOException;

    protected abstract void writeToDisk() throws IOException;

    protected abstract String getProperty(String propertyName) throws IOException;

    protected abstract void refreshData();

    protected Path getProjectDirPath() {
        return projectDirPath;
    }

    protected boolean hasProjectFile(final String fileName) throws IOException {
        final Path filePath = projectDirPath.resolve(fileName);
        return Files.exists(filePath);
    }

    protected byte[] readProjectFile(final String fileName) throws IOException {
        final Path filePath = projectDirPath.resolve(fileName);
        return Files.readAllBytes(filePath);
    }

    protected void writeToProjectFile(final String fileName, final byte[] content) throws IOException {
        Files.write(projectDirPath.resolve(fileName), content);
    }

    private boolean isQuarkusExtension(final AppArtifactKey key) {
        if (catalog != null) {
            return findInList(catalog.getExtensions(), key).isPresent();
        }
        return isDefinedInRegistry(catalog.getExtensions(), key);
    }

    private Set<AppArtifactKey> getDependenciesKeys() throws IOException {
        return getDependencies().stream().map(AppArtifactCoords::getKey).collect(Collectors.toSet());
    }

    public static boolean isDefinedInRegistry(Collection<Extension> registry, final AppArtifactKey key) {
        return Extensions.findInList(registry, key).isPresent();
    }

    private static Optional<io.quarkus.registry.catalog.Extension> findInList(
            Collection<io.quarkus.registry.catalog.Extension> list, final AppArtifactKey key) {
        ArtifactKey k = new ArtifactKey(key.getGroupId(), key.getArtifactId(), key.getClassifier(), key.getType());
        return list.stream().filter(e -> Objects.equals(e.getArtifact().getKey(), k)).findFirst();
    }

}
