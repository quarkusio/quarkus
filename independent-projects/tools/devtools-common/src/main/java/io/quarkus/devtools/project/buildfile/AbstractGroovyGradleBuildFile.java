package io.quarkus.devtools.project.buildfile;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.nio.file.Path;

public abstract class AbstractGroovyGradleBuildFile extends AbstractGradleBuildFile {

    static final String BUILD_GRADLE_PATH = "build.gradle";
    static final String SETTINGS_GRADLE_PATH = "settings.gradle";

    public AbstractGroovyGradleBuildFile(Path projectDirPath, ExtensionCatalog catalog) {
        super(projectDirPath, catalog);
    }

    public AbstractGroovyGradleBuildFile(Path projectDirPath, ExtensionCatalog catalog,
            Path rootProjectPath) {
        super(projectDirPath, catalog, rootProjectPath);
    }

    @Override
    String getSettingsGradlePath() {
        return SETTINGS_GRADLE_PATH;
    }

    @Override
    String getBuildGradlePath() {
        return BUILD_GRADLE_PATH;
    }

    @Override
    protected boolean addDependency(AppArtifactCoords coords, boolean managed) {
        return addDependencyInModel(getModel(), coords, managed);
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.GRADLE;
    }

    static boolean addDependencyInModel(Model model, AppArtifactCoords coords, boolean managed) {
        return addDependencyInModel(model,
                String.format("    implementation %s%n", createDependencyCoordinatesString(coords, managed, '\'')));
    }
}
