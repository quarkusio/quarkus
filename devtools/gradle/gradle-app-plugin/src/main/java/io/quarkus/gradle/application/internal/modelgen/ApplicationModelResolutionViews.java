package io.quarkus.gradle.application.internal.modelgen;

import java.util.EnumMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.VerificationType;

import io.quarkus.gradle.model.config.ExtensionVariantConstants;
import io.quarkus.gradle.model.config.LocalComponentOutputViews;
import io.quarkus.runtime.LaunchMode;

/**
 * Resolution configurations and artifact views created during plugin
 * application and shared by tasks and Tooling API model builders.
 */
public final class ApplicationModelResolutionViews {

    private final Map<LaunchMode, ModeViews> modes;
    private final ModeViews continuousTest;
    private final Configuration platformConfiguration;

    private ApplicationModelResolutionViews(Map<LaunchMode, ModeViews> modes, ModeViews continuousTest,
            Configuration platformConfiguration) {
        this.modes = Map.copyOf(modes);
        this.continuousTest = continuousTest;
        this.platformConfiguration = platformConfiguration;
    }

    public static ApplicationModelResolutionViews create(Project project, ClasspathBuilder classpath) {
        Map<LaunchMode, ModeViews> modes = new EnumMap<>(LaunchMode.class);
        modes.put(LaunchMode.NORMAL, modeViews(project, classpath.getRuntimeConfiguration(),
                classpath.getDeploymentConfiguration(), classpath.getCompileOnlyConfiguration()));
        modes.put(LaunchMode.DEVELOPMENT, modeViews(project, classpath.getDevRuntimeConfiguration(),
                classpath.getDevDeploymentConfiguration(), classpath.getCompileOnlyConfiguration()));
        modes.put(LaunchMode.TEST, modeViews(project, classpath.getTestRuntimeConfiguration(),
                classpath.getTestDeploymentConfiguration(), classpath.getTestCompileOnlyConfiguration()));
        ModeViews continuousTest = modeViews(project, classpath.getContinuousTestRuntimeConfiguration(),
                classpath.getContinuousTestDeploymentConfiguration(), classpath.getTestCompileOnlyConfiguration());
        return new ApplicationModelResolutionViews(modes, continuousTest, classpath.getPlatformPropertiesConfiguration());
    }

    public ModeViews forMode(LaunchMode mode) {
        ModeViews result = modes.get(mode);
        if (result == null) {
            throw new IllegalArgumentException("Unsupported Quarkus launch mode " + mode);
        }
        return result;
    }

    public Configuration platformConfiguration() {
        return platformConfiguration;
    }

    public ModeViews forContinuousTest() {
        return continuousTest;
    }

    private static ModeViews modeViews(Project project, Configuration runtime, Configuration deployment,
            Configuration compileOnly) {
        LocalComponentOutputViews localOutputs = LocalComponentOutputViews.of(project.getObjects(), runtime);
        ArtifactView mainSources = runtime.getIncoming().artifactView(view -> {
            view.withVariantReselection();
            view.lenient(true);
            view.componentFilter(ProjectComponentIdentifier.class::isInstance);
            view.attributes(attributes -> {
                attributes.attribute(Category.CATEGORY_ATTRIBUTE,
                        project.getObjects().named(Category.class, Category.VERIFICATION));
                attributes.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
                        project.getObjects().named(VerificationType.class, VerificationType.MAIN_SOURCES));
                attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                        ArtifactTypeDefinition.DIRECTORY_TYPE);
            });
        });
        ArtifactView deploymentMarkers = deployment.getIncoming().artifactView(view -> {
            view.withVariantReselection();
            view.lenient(true);
            view.componentFilter(ProjectComponentIdentifier.class::isInstance);
            view.attributes(attributes -> {
                attributes.attribute(Category.CATEGORY_ATTRIBUTE,
                        project.getObjects().named(Category.class,
                                ExtensionVariantConstants.EXTENSION_DEPLOYMENT_MARKER_CATEGORY));
                attributes.attribute(ExtensionVariantConstants.EXTENSION_DEPLOYMENT_ATTRIBUTE, true);
            });
        });
        return new ModeViews(runtime, deployment, compileOnly, localOutputs, mainSources, deploymentMarkers);
    }

    public record ModeViews(
            Configuration runtime,
            Configuration deployment,
            Configuration compileOnly,
            LocalComponentOutputViews localOutputs,
            ArtifactView mainSources,
            ArtifactView deploymentMarkers) {
    }
}
