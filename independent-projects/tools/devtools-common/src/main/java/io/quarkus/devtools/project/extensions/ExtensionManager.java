package io.quarkus.devtools.project.extensions;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 * This interface defines a high level way of managing (read/write) extensions in any QuarkusProject
 */
public interface ExtensionManager {

    /**
     * @return the {@link BuildTool} of this extension manager
     */
    BuildTool getBuildTool();

    /**
     * Returns the list of the imported platforms in the current project.
     *
     * @return current list of imported platforms
     * @throws IOException if a problem occurs while reading the project build file(s)
     */
    Collection<ArtifactCoords> getInstalledPlatforms() throws IOException;

    /**
     * Read the build file(s) to get the list of installed extensions in this Quarkus project.
     *
     * @return The list of {@link ArtifactCoords} installed in the project build file(s).
     * @throws IOException if a problem occurs while reading the project build file(s)
     */
    Collection<ArtifactCoords> getInstalled() throws IOException;

    /**
     * Read build file(s) to check if an extension is installed in this Quarkus project.
     *
     * @param key the {@link ArtifactKey} of the extension to check
     * @return true if it's installed
     * @throws IOException if a problem occurs while reading the project build file(s)
     */
    default boolean isInstalled(ArtifactKey key) throws IOException {
        return getInstalled().stream().anyMatch(i -> Objects.equals(i.getKey(), key));
    }

    /**
     * This is going to install/add all the specified extensions to the project build file(s).
     *
     * <pre>
     *   - Extensions which are already installed will ALWAYS be skipped whatever the specified version
     *   - The provided version will be used if it wasn't already installed
     * </pre>
     *
     * @param coords the list of {@link ArtifactCoords} for the extensions to install
     * @return the {@link InstallResult}
     * @throws IOException if a problem occurs while reading/writing the project build file(s)
     */
    InstallResult install(Collection<ArtifactCoords> coords) throws IOException;

    /**
     * This is going to install/add all the specified extensions to the project build file(s).
     *
     * <pre>
     *   - If the project Quarkus platform bom is not defined, an {@link IllegalStateException} will be thrown
     *   - Extensions which are already installed will ALWAYS be skipped whatever the specified version
     *   - The provided version will be used if wasn't already installed
     * </pre>
     *
     * @param request the list of {@link ArtifactCoords} for the extensions to install
     * @return the {@link InstallResult}
     * @throws IOException if a problem occurs while reading/writing the project build file(s)
     */
    InstallResult install(ExtensionInstallPlan request) throws IOException;

    /**
     * This is going to uninstall/remove all the specified extensions from the project build file(s).
     *
     * This is ignoring the version
     *
     * @param keys the set of {@link ArtifactKey} for the extensions to uninstall
     * @return the {@link InstallResult}
     * @throws IOException if a problem occurs while reading/writing the project build file(s)
     */
    UninstallResult uninstall(Collection<ArtifactKey> keys) throws IOException;

    class InstallResult {
        private final Collection<ArtifactCoords> installedPlatforms;
        private final Collection<ArtifactCoords> installedManagedExtensions;
        private final Collection<ArtifactCoords> installedIndependentExtensions;
        private final Collection<ArtifactKey> alreadyInstalled;

        public InstallResult(Collection<ArtifactCoords> installedPlatforms,
                Collection<ArtifactCoords> installedManagedExtensions,
                Collection<ArtifactCoords> installedIndependentExtensions, Collection<ArtifactKey> alreadyInstalled) {
            this.installedPlatforms = installedPlatforms;
            this.installedManagedExtensions = installedManagedExtensions;
            this.installedIndependentExtensions = installedIndependentExtensions;
            this.alreadyInstalled = alreadyInstalled;
        }

        public Collection<ArtifactCoords> getInstalledManagedExtensions() {
            return installedManagedExtensions;
        }

        public Collection<ArtifactCoords> getInstalledIndependentExtensions() {
            return installedIndependentExtensions;
        }

        public Collection<ArtifactCoords> getInstalledPlatforms() {
            return installedPlatforms;
        }

        public Collection<ArtifactKey> getAlreadyInstalled() {
            return alreadyInstalled;
        }

        public boolean isSourceUpdated() {
            return !installedPlatforms.isEmpty() || !installedManagedExtensions.isEmpty()
                    || !installedIndependentExtensions.isEmpty();
        }
    }

    class UninstallResult {
        private final Collection<ArtifactKey> uninstalled;

        public UninstallResult(Collection<ArtifactKey> uninstalled) {
            this.uninstalled = uninstalled;
        }

        public Collection<ArtifactKey> getUninstalled() {
            return uninstalled;
        }

        public boolean isSourceUpdated() {
            return uninstalled.size() > 0;
        }

    }

}
