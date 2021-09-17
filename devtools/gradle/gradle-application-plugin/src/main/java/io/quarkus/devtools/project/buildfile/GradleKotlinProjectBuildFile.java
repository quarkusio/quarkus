package io.quarkus.devtools.project.buildfile;

import org.gradle.api.Project;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class GradleKotlinProjectBuildFile extends GradleProjectBuildFile {

    static final String BUILD_GRADLE_PATH = "build.gradle.kts";
    static final String SETTINGS_GRADLE_PATH = "settings.gradle.kts";

    public GradleKotlinProjectBuildFile(Project project, ExtensionCatalog catalog) {
        super(project, catalog);
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
    protected boolean importBom(ArtifactCoords coords) {
        return importBomInModel(getModel(), toBomImportCoords(coords));
    }

    @Override
    protected boolean addDependency(ArtifactCoords coords, boolean managed) {
        return addDependencyInModel(getModel(), coords, managed);
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.GRADLE_KOTLIN_DSL;
    }

    static boolean importBomInModel(Model model, ArtifactCoords coords) {
        return addDependencyInModel(model,
                String.format("    implementation(%s)%n",
                        createDependencyCoordinatesString(coords, false, '"')));
    }

    static boolean addDependencyInModel(Model model, ArtifactCoords coords, boolean managed) {
        return addDependencyInModel(model,
                String.format("    implementation(%s)%n", createDependencyCoordinatesString(coords, managed, '"')));
    }
}
