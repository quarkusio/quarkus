package io.quarkus.devtools.project.extensions;

import io.quarkus.maven.ArtifactCoords;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExtensionInstallPlan {

    public static final ExtensionInstallPlan EMPTY = new ExtensionInstallPlan(
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet());

    private final Set<ArtifactCoords> platforms;
    private final Set<ArtifactCoords> managedExtensions;
    private final Set<ArtifactCoords> independentExtensions;
    private final Collection<String> unmatchedKeywords;

    private ExtensionInstallPlan(Set<ArtifactCoords> platforms,
            Set<ArtifactCoords> managedExtensions,
            Set<ArtifactCoords> independentExtensions,
            Collection<String> unmatchedKeywords) {
        this.platforms = platforms;
        this.managedExtensions = managedExtensions;
        this.independentExtensions = independentExtensions;
        this.unmatchedKeywords = unmatchedKeywords;
    }

    public boolean isNotEmpty() {
        return !this.platforms.isEmpty() || !this.managedExtensions.isEmpty()
                || !this.independentExtensions.isEmpty();
    }

    public boolean isInstallable() {
        return isNotEmpty() && unmatchedKeywords.isEmpty();
    }

    /**
     * @return a {@link Collection} of all extensions contained in this object
     */
    public Collection<ArtifactCoords> toCollection() {
        Set<ArtifactCoords> result = new LinkedHashSet<>();
        result.addAll(getPlatforms());
        result.addAll(getManagedExtensions());
        result.addAll(getIndependentExtensions());
        return result;
    }

    /**
     * @return Platforms (BOMs) to be added to the build descriptor
     */
    public Collection<ArtifactCoords> getPlatforms() {
        return platforms;
    }

    /**
     * @return Extensions that are included in the platforms returned in {@link #getPlatforms()},
     *         therefore setting the version is not required.
     */
    public Collection<ArtifactCoords> getManagedExtensions() {
        return managedExtensions;
    }

    /**
     * @return Extensions that do not exist in any platform, the version MUST be set in the build descriptor
     */
    public Collection<ArtifactCoords> getIndependentExtensions() {
        return independentExtensions;
    }

    public Collection<String> getUnmatchedKeywords() {
        return unmatchedKeywords;
    }

    @Override
    public String toString() {
        return "InstallRequest{" +
                "platforms=" + platforms +
                ", managedExtensions=" + managedExtensions +
                ", independentExtensions=" + independentExtensions +
                ", unmatchedKeywords=" + unmatchedKeywords +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Set<ArtifactCoords> platforms = new LinkedHashSet<>();
        private final Set<ArtifactCoords> extensionsInPlatforms = new LinkedHashSet<>();
        private final Set<ArtifactCoords> independentExtensions = new LinkedHashSet<>();
        private final Collection<String> unmatchedKeywords = new ArrayList<>();

        public ExtensionInstallPlan build() {
            return new ExtensionInstallPlan(platforms, extensionsInPlatforms, independentExtensions, unmatchedKeywords);
        }

        public Builder addIndependentExtension(ArtifactCoords artifactCoords) {
            this.independentExtensions.add(artifactCoords);
            return this;
        }

        public Builder addManagedExtension(ArtifactCoords artifactCoords) {
            this.extensionsInPlatforms.add(artifactCoords);
            return this;
        }

        public Builder addPlatform(ArtifactCoords artifactCoords) {
            this.platforms.add(artifactCoords);
            return this;
        }

        public Builder addUnmatchedKeyword(String unmatchedKeyword) {
            this.unmatchedKeywords.add(unmatchedKeyword);
            return this;
        }

        public boolean hasExtensionInPlatform() {
            return !this.extensionsInPlatforms.isEmpty();
        }
    }
}
