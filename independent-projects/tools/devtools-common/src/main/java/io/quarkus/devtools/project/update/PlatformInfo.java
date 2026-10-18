package io.quarkus.devtools.project.update;

import io.quarkus.devtools.project.state.ExtensionProvider;
import io.quarkus.maven.dependency.ArtifactCoords;

public class PlatformInfo {
    private final ArtifactCoords imported;
    private final ArtifactCoords recommended;

    public PlatformInfo(ArtifactCoords imported, ArtifactCoords recommended) {
        this.imported = imported;
        this.recommended = recommended;
    }

    public ArtifactCoords getImported() {
        return imported;
    }

    public ArtifactCoords getRecommended() {
        return recommended;
    }

    public boolean isVersionUpdateRecommended() {
        return imported != null && recommended != null && !imported.getVersion().equals(recommended.getVersion());
    }

    public String getRecommendedVersion() {
        return recommended == null ? null : recommended.getVersion();
    }

    public boolean isImported() {
        return imported != null;
    }

    public boolean isToBeImported() {
        return imported == null && recommended != null;
    }

    public ArtifactCoords getRecommendedCoords() {
        return recommended == null ? imported : recommended;
    }

    public String getRecommendedProviderKey() {
        if (recommended != null) {
            return ExtensionProvider.key(recommended, true);
        }
        return ExtensionProvider.key(imported, true);
    }
}
