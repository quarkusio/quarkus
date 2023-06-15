package io.quarkus.devtools.project.update;

import java.util.List;
import java.util.Map;

import io.quarkus.maven.dependency.ArtifactKey;

public class ProjectPlatformUpdateInfo {
    private final Map<ArtifactKey, PlatformInfo> platformImports;
    private final List<PlatformInfo> importVersionUpdates;
    private final List<PlatformInfo> newImports;
    private final boolean importsToBeRemoved;
    private final boolean platformUpdatesAvailable;

    public ProjectPlatformUpdateInfo(Map<ArtifactKey, PlatformInfo> platformImports, List<PlatformInfo> importVersionUpdates,
            List<PlatformInfo> newImports) {
        this.platformImports = platformImports;
        this.importVersionUpdates = importVersionUpdates;
        this.newImports = newImports;
        this.importsToBeRemoved = platformImports.values().stream().anyMatch(p -> p.getRecommended() == null);
        this.platformUpdatesAvailable = !importVersionUpdates.isEmpty() || !newImports.isEmpty() || importsToBeRemoved;
    }

    public boolean isImportsToBeRemoved() {
        return importsToBeRemoved;
    }

    public boolean isPlatformUpdatesAvailable() {
        return platformUpdatesAvailable;
    }

    public Map<ArtifactKey, PlatformInfo> getPlatformImports() {
        return platformImports;
    }

    public List<PlatformInfo> getImportVersionUpdates() {
        return importVersionUpdates;
    }

    public List<PlatformInfo> getNewImports() {
        return newImports;
    }
}
