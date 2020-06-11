package io.quarkus.devtools.project.buildfile;

import static io.quarkus.devtools.project.extensions.Extensions.findInList;
import static io.quarkus.devtools.project.extensions.Extensions.toCoords;
import static io.quarkus.devtools.project.extensions.Extensions.toKey;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;

public abstract class BuildFile implements ExtensionManager {

    private final Path projectFolderPath;
    private final QuarkusPlatformDescriptor platformDescriptor;

    public BuildFile(final Path projectFolderPath, final QuarkusPlatformDescriptor platformDescriptor) {
        this.projectFolderPath = requireNonNull(projectFolderPath, "projectPath is required");
        this.platformDescriptor = requireNonNull(platformDescriptor, "platformDescriptor is required");
    }

    @Override
    public final boolean hasQuarkusPlatformBom() throws IOException {
        return containsBOM(platformDescriptor.getBomGroupId(), platformDescriptor.getBomArtifactId());
    }

    @Override
    public final InstallResult install(Collection<AppArtifactCoords> coords) throws IOException {
        if (!hasQuarkusPlatformBom()) {
            throw new IllegalStateException("The Quarkus BOM is required to add a Quarkus extension");
        }
        this.refreshData();
        final Set<AppArtifactKey> existingKeys = getDependenciesKeys();
        final List<AppArtifactCoords> installed = coords.stream()
                .distinct()
                .filter(a -> !existingKeys.contains(a.getKey()))
                .filter(e -> {
                    try {
                        addDependencyInBuildFile(e);
                        return true;
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }).collect(toList());
        this.writeToDisk();
        return new InstallResult(installed);
    }

    @Override
    public final Collection<AppArtifactCoords> getInstalled() throws IOException {
        this.refreshData();
        return this.getDependencies().stream()
                .filter(d -> this.isQuarkusExtension(toKey(d)))
                .map(d -> toCoords(d, extractVersion(d)))
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
                        removeDependencyFromBuildFile(k);
                        return true;
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }).collect(toList());
        this.writeToDisk();
        return new UninstallResult(uninstalled);
    }

    protected abstract void addDependencyInBuildFile(AppArtifactCoords coords) throws IOException;

    protected abstract void removeDependencyFromBuildFile(AppArtifactKey key) throws IOException;

    protected abstract List<Dependency> getDependencies() throws IOException;

    protected abstract void writeToDisk() throws IOException;

    protected abstract String getProperty(String propertyName) throws IOException;

    protected abstract boolean containsBOM(String groupId, String artifactId) throws IOException;

    protected abstract void refreshData();

    protected Path getProjectFolderPath() {
        return projectFolderPath;
    }

    protected boolean hasProjectFile(final String fileName) throws IOException {
        final Path filePath = projectFolderPath.resolve(fileName);
        return Files.exists(filePath);
    }

    protected byte[] readProjectFile(final String fileName) throws IOException {
        final Path filePath = projectFolderPath.resolve(fileName);
        return Files.readAllBytes(filePath);
    }

    protected void writeToProjectFile(final String fileName, final byte[] content) throws IOException {
        Files.write(projectFolderPath.resolve(fileName), content);
    }

    private boolean isQuarkusExtension(final AppArtifactKey key) {
        // This will not always be true as the platform descriptor does not contain the list of all available extensions
        return isDefinedInRegistry(platformDescriptor.getExtensions(), key);
    }

    private Set<AppArtifactKey> getDependenciesKeys() throws IOException {
        return getDependencies().stream().map(Extensions::toKey).collect(Collectors.toSet());
    }

    private String extractVersion(final Dependency d) {
        String version = d != null ? d.getVersion() : null;
        if (version != null && version.startsWith("$")) {
            String value = null;
            try {
                value = (String) this.getProperty(propertyName(version));
                if (value != null) {
                    return value;
                }
            } catch (IOException e) {
                // ignore this error.
            }
        }
        if (version != null) {
            return version;
        }
        return null;
    }

    private String propertyName(final String variable) {
        return variable.substring(2, variable.length() - 1);
    }

    public static boolean isDefinedInRegistry(List<Extension> registry, final AppArtifactKey key) {
        return findInList(registry, key).isPresent();
    }

}
