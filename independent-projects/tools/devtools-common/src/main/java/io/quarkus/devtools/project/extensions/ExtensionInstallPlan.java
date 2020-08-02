package io.quarkus.devtools.project.extensions;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExtensionInstallPlan {

    public static final ExtensionInstallPlan EMPTY = new ExtensionInstallPlan(
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet());

    private final Set<AppArtifactCoords> platforms;
    private final Set<AppArtifactCoords> managedExtensions;
    private final Set<AppArtifactCoords> independentExtensions;

    private ExtensionInstallPlan(Set<AppArtifactCoords> platforms,
            Set<AppArtifactCoords> managedExtensions,
            Set<AppArtifactCoords> independentExtensions) {
        this.platforms = platforms;
        this.managedExtensions = managedExtensions;
        this.independentExtensions = independentExtensions;
    }

    public boolean isNotEmpty() {
        return !this.platforms.isEmpty() || !this.managedExtensions.isEmpty()
                || !this.independentExtensions.isEmpty();
    }

    /**
     * @return a {@link Collection} of all extensions contained in this object
     */
    public Collection<AppArtifactCoords> toCollection() {
        Set<AppArtifactCoords> result = new LinkedHashSet<>();
        result.addAll(getPlatforms());
        result.addAll(getManagedExtensions());
        result.addAll(getIndependentExtensions());
        return result;
    }

    /**
     * @return Platforms (BOMs) to be added to the build descriptor
     */
    public Collection<AppArtifactCoords> getPlatforms() {
        return platforms;
    }

    /**
     * @return Extensions that are included in the platforms returned in {@link #getPlatforms()},
     *         therefore setting the version is not required.
     */
    public Collection<AppArtifactCoords> getManagedExtensions() {
        return managedExtensions;
    }

    /**
     * @return Extensions that do not exist in any platform, the version MUST be set in the build descriptor
     */
    public Collection<AppArtifactCoords> getIndependentExtensions() {
        return independentExtensions;
    }

    @Override
    public String toString() {
        return "InstallRequest{" +
                "platforms=" + platforms +
                ", managedExtensions=" + managedExtensions +
                ", independentExtensions=" + independentExtensions +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Set<AppArtifactCoords> platforms = new LinkedHashSet<>();
        private final Set<AppArtifactCoords> extensionsInPlatforms = new LinkedHashSet<>();
        private final Set<AppArtifactCoords> independentExtensions = new LinkedHashSet<>();

        public ExtensionInstallPlan build() {
            return new ExtensionInstallPlan(platforms, extensionsInPlatforms, independentExtensions);
        }

        public Builder addIndependentExtension(AppArtifactCoords appArtifactCoords) {
            this.independentExtensions.add(appArtifactCoords);
            return this;
        }

        public Builder addManagedExtension(AppArtifactCoords appArtifactCoords) {
            this.extensionsInPlatforms.add(appArtifactCoords);
            return this;
        }

        public Builder addPlatform(AppArtifactCoords appArtifactCoords) {
            this.platforms.add(appArtifactCoords);
            return this;
        }

        public boolean hasExtensionInPlatform() {
            return !this.extensionsInPlatforms.isEmpty();
        }
    }
}
