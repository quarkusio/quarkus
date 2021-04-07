package io.quarkus.devtools.project.buildfile;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.project.extensions.ExtensionInstallPlan;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import java.io.IOException;
import java.util.Collection;

/**
 * TODO We need to find a way to use the gradle api outside of a gradle plugin
 */
abstract class GenericGradleBuildFile implements ExtensionManager {

    @Override
    public Collection<AppArtifactCoords> getInstalled() throws IOException {
        throw new IllegalStateException("This feature is not yet implemented outside of the Gradle Plugin.");
    }

    @Override
    public Collection<AppArtifactCoords> getInstalledPlatforms() throws IOException {
        throw new IllegalStateException("This feature is not yet implemented outside of the Gradle Plugin.");
    }

    @Override
    public InstallResult install(Collection<AppArtifactCoords> coords) throws IOException {
        throw new IllegalStateException("This feature is not yet implemented outside of the Gradle Plugin.");
    }

    @Override
    public InstallResult install(ExtensionInstallPlan request) throws IOException {
        throw new IllegalStateException("This feature is not yet implemented outside of the Gradle Plugin.");
    }

    @Override
    public UninstallResult uninstall(Collection<AppArtifactKey> keys) throws IOException {
        throw new IllegalStateException("This feature is not yet implemented outside of the Gradle Plugin.");
    }

}
