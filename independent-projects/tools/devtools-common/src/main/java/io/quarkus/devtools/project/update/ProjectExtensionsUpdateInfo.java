package io.quarkus.devtools.project.update;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.platform.catalog.processor.ExtensionProcessor;

public class ProjectExtensionsUpdateInfo {
    private final Map<String, List<ExtensionUpdateInfo>> managedExtensions;
    final Map<String, List<ExtensionUpdateInfo>> versionedManagedExtensions;
    final Map<String, List<ExtensionUpdateInfo>> removedExtensions;
    final Map<String, List<ExtensionUpdateInfo>> addedExtensions;
    final Map<String, List<ExtensionUpdateInfo>> nonPlatformExtensions;

    public ProjectExtensionsUpdateInfo(Map<String, List<ExtensionUpdateInfo>> managedExtensions,
            Map<String, List<ExtensionUpdateInfo>> versionedManagedExtensions,
            Map<String, List<ExtensionUpdateInfo>> removedExtensions,
            Map<String, List<ExtensionUpdateInfo>> addedExtensions,
            Map<String, List<ExtensionUpdateInfo>> nonPlatformExtensionUpdate) {
        this.managedExtensions = managedExtensions;
        this.versionedManagedExtensions = versionedManagedExtensions;
        this.removedExtensions = removedExtensions;
        this.addedExtensions = addedExtensions;
        this.nonPlatformExtensions = nonPlatformExtensionUpdate;
    }

    public Map<String, List<ExtensionUpdateInfo>> getManagedExtensions() {
        return managedExtensions;
    }

    public Map<String, List<ExtensionUpdateInfo>> getVersionedManagedExtensions() {
        return versionedManagedExtensions;
    }

    public Map<String, List<ExtensionUpdateInfo>> getRemovedExtensions() {
        return removedExtensions;
    }

    public Map<String, List<ExtensionUpdateInfo>> getAddedExtensions() {
        return addedExtensions;
    }

    public Map<String, List<ExtensionUpdateInfo>> getNonPlatformExtensions() {
        return nonPlatformExtensions;
    }

    public OptionalInt getMinJavaVersion() {
        return Stream.of(getManagedExtensions().values(),
                getVersionedManagedExtensions().values(),
                getNonPlatformExtensions().values(),
                getAddedExtensions().values())
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .mapToInt(e -> Optional.ofNullable(ExtensionProcessor.getMinimumJavaVersion(e.getRecommendedMetadata()))
                        .orElse(JavaVersion.DEFAULT_JAVA_VERSION))
                .max();
    }

    public boolean isUpToDate() {
        return versionedManagedExtensions.isEmpty()
                && removedExtensions.isEmpty()
                && addedExtensions.isEmpty()
                && nonPlatformExtensions.isEmpty();
    }
}
