package io.quarkus.devtools.project.buildfile;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import io.quarkus.devtools.project.extensions.ExtensionInstallPlan;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.ArtifactCoords;
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
    public final InstallResult install(Collection<ArtifactCoords> coords) throws IOException {
        final ExtensionInstallPlan.Builder builder = ExtensionInstallPlan.builder();
        for (ArtifactCoords coord : coords) {
            if ("pom".equals(coord.getType())) {
                builder.addPlatform(coord);
            } else if (coord.getVersion() == null) {
                builder.addManagedExtension(coord);
            } else {
                builder.addIndependentExtension(coord);
            }
        }
        return install(builder.build());
    }

    @Override
    public InstallResult install(ExtensionInstallPlan plan) throws IOException {
        this.refreshData();
        List<ArtifactCoords> installedManagedExtensions = new ArrayList<>();
        List<ArtifactCoords> installedIndependentExtensions = new ArrayList<>();
        List<ArtifactCoords> installedPlatforms = new ArrayList<>();
        final Set<ArtifactKey> alreadyInstalled = alreadyInstalled(plan.toCollection());
        for (ArtifactCoords platform : withoutAlreadyInstalled(alreadyInstalled, plan.getPlatforms())) {
            if (importBom(platform)) {
                installedPlatforms.add(platform);
            }
        }
        for (ArtifactCoords managedExtension : withoutAlreadyInstalled(alreadyInstalled, plan.getManagedExtensions())) {
            if (addDependency(managedExtension, true)) {
                installedManagedExtensions.add(managedExtension);
            }
        }
        for (ArtifactCoords independentExtension : withoutAlreadyInstalled(alreadyInstalled, plan.getIndependentExtensions())) {
            if (addDependency(independentExtension, false)) {
                installedIndependentExtensions.add(independentExtension);
            }
        }
        writeToDisk();
        return new InstallResult(installedPlatforms, installedManagedExtensions, installedIndependentExtensions,
                alreadyInstalled);
    }

    @Override
    public final Collection<ArtifactCoords> getInstalled() throws IOException {
        this.refreshData();
        return this.getDependencies().stream()
                .filter(d -> this.isQuarkusExtension(d.getKey()))
                .collect(toList());
    }

    @Override
    public final UninstallResult uninstall(Collection<ArtifactKey> keys) throws IOException {
        this.refreshData();
        final Set<ArtifactKey> existingKeys = getDependenciesKeys();
        final List<ArtifactKey> uninstalled = keys.stream()
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

    private Set<ArtifactKey> alreadyInstalled(Collection<ArtifactCoords> extensions) throws IOException {
        final Set<ArtifactKey> existingKeys = getDependenciesKeys();
        return extensions.stream()
                .distinct()
                .filter(a -> existingKeys.contains(a.getKey()))
                .map(ArtifactCoords::getKey)
                .collect(Collectors.toSet());
    }

    private Collection<ArtifactCoords> withoutAlreadyInstalled(Set<ArtifactKey> existingKeys,
            Collection<ArtifactCoords> extensions) {
        return extensions.stream()
                .distinct()
                .filter(a -> !existingKeys.contains(a.getKey()))
                .collect(toList());
    }

    protected abstract boolean importBom(ArtifactCoords coords);

    protected abstract boolean addDependency(ArtifactCoords coords, boolean managed);

    protected abstract void removeDependency(ArtifactKey key) throws IOException;

    protected abstract List<ArtifactCoords> getDependencies() throws IOException;

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

    private boolean isQuarkusExtension(final ArtifactKey key) {
        return catalog != null ? isDefinedInRegistry(catalog.getExtensions(), key) : false;
    }

    private Set<ArtifactKey> getDependenciesKeys() throws IOException {
        return getDependencies().stream().map(ArtifactCoords::getKey).collect(Collectors.toSet());
    }

    public static boolean isDefinedInRegistry(Collection<Extension> registry, final ArtifactKey key) {
        return Extensions.findInList(registry, key).isPresent();
    }
}
