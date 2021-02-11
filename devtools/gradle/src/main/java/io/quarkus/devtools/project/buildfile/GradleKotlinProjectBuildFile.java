package io.quarkus.devtools.project.buildfile;

import org.gradle.api.Project;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public class GradleKotlinProjectBuildFile extends GradleProjectBuildFile {

    static final String BUILD_GRADLE_PATH = "build.gradle.kts";
    static final String SETTINGS_GRADLE_PATH = "settings.gradle.kts";

    public GradleKotlinProjectBuildFile(Project project, QuarkusPlatformDescriptor platformDescriptor) {
        super(project, platformDescriptor);
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
        return BuildTool.GRADLE_KOTLIN_DSL;
    }

    static boolean addDependencyInModel(Model model, AppArtifactCoords coords, boolean managed) {
        return addDependencyInModel(model,
                String.format("    implementation(%s)%n", createDependencyCoordinatesString(coords, managed, '"')));
    }
}
