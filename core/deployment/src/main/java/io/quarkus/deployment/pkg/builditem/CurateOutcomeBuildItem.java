package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.builder.item.SimpleBuildItem;

public final class CurateOutcomeBuildItem extends SimpleBuildItem {

    private final ApplicationModel appModel;
    private Path appModuleDir;

    public CurateOutcomeBuildItem(ApplicationModel appModel) {
        this.appModel = appModel;
    }

    public ApplicationModel getApplicationModel() {
        return appModel;
    }

    /**
     * Returns the application module directory, if the application is built from a source project.
     * For a single module project it will the project directory. For a multimodule project,
     * it will the directory of the application module.
     * <p>
     * During re-augmentation of applications packaged as {@code mutable-jar} this method will return the current directory,
     * since the source project might not be available anymore.
     *
     * @return application module directory, never null
     */
    public Path getApplicationModuleDirectory() {
        if (appModuleDir == null) {
            // modules are by default available in dev and test modes, and in prod mode if
            // quarkus.bootstrap.workspace-discovery system or project property is true,
            // otherwise it could be null
            final WorkspaceModule module = appModel.getApplicationModule();
            appModuleDir = module == null ? deriveModuleDirectoryFromArtifact() : module.getModuleDir().toPath();
        }
        return appModuleDir;
    }

    private Path deriveModuleDirectoryFromArtifact() {
        var paths = appModel.getAppArtifact().getResolvedPaths();
        for (var path : paths) {
            if (Files.isDirectory(path)) {
                var moduleDir = BuildToolHelper.getProjectDir(path);
                if (moduleDir != null) {
                    return moduleDir;
                }
            }
        }
        // the module isn't available, return the current directory
        return Path.of("").toAbsolutePath();
    }
}
