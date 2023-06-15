package io.quarkus.devtools.project.update;

import java.util.List;
import java.util.Map;

import io.quarkus.maven.dependency.ArtifactCoords;

public class ProjectExtensionsUpdateInfo {
    final Map<String, List<ExtensionUpdateInfo>> versionedManagedExtensions;
    final Map<String, List<ArtifactCoords>> removedExtensions;
    final Map<String, List<ArtifactCoords>> addedExtensions;
    final Map<String, List<ExtensionUpdateInfo>> nonPlatformExtensions;

    public ProjectExtensionsUpdateInfo(Map<String, List<ExtensionUpdateInfo>> versionedManagedExtensions,
            Map<String, List<ArtifactCoords>> removedExtensions, Map<String, List<ArtifactCoords>> addedExtensions,
            Map<String, List<ExtensionUpdateInfo>> nonPlatformExtensionUpdate) {
        this.versionedManagedExtensions = versionedManagedExtensions;
        this.removedExtensions = removedExtensions;
        this.addedExtensions = addedExtensions;
        this.nonPlatformExtensions = nonPlatformExtensionUpdate;
    }

    public Map<String, List<ExtensionUpdateInfo>> getVersionedManagedExtensions() {
        return versionedManagedExtensions;
    }

    public Map<String, List<ArtifactCoords>> getRemovedExtensions() {
        return removedExtensions;
    }

    public Map<String, List<ArtifactCoords>> getAddedExtensions() {
        return addedExtensions;
    }

    public Map<String, List<ExtensionUpdateInfo>> getNonPlatformExtensions() {
        return nonPlatformExtensions;
    }

    public boolean isEmpty() {
        return versionedManagedExtensions.isEmpty()
                && removedExtensions.isEmpty()
                && addedExtensions.isEmpty()
                && nonPlatformExtensions.isEmpty();
    }
}
