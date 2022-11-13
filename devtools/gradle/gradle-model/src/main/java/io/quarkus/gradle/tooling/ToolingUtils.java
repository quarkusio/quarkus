package io.quarkus.gradle.tooling;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.attributes.Category;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.internal.composite.IncludedBuildInternal;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.runtime.LaunchMode;

public class ToolingUtils {

    private static final String DEPLOYMENT_CONFIGURATION_SUFFIX = "Deployment";
    private static final String PLATFORM_CONFIGURATION_SUFFIX = "Platform";
    public static final String DEV_MODE_CONFIGURATION_NAME = "quarkusDev";

    public static String toDeploymentConfigurationName(String baseConfigurationName) {
        return baseConfigurationName + DEPLOYMENT_CONFIGURATION_SUFFIX;
    }

    public static String toPlatformConfigurationName(String baseConfigurationName) {
        return baseConfigurationName + PLATFORM_CONFIGURATION_SUFFIX;
    }

    public static boolean isEnforcedPlatform(ModuleDependency module) {
        final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
        return category != null && (Category.ENFORCED_PLATFORM.equals(category.getName())
                || Category.REGULAR_PLATFORM.equals(category.getName()));
    }

    public static IncludedBuild includedBuild(final Project project,
            final ProjectComponentIdentifier projectComponentIdentifier) {
        try {
            return project.getRootProject().getGradle().includedBuild(projectComponentIdentifier.getBuild().getName());
        } catch (UnknownDomainObjectException e) {
            return null;
        }
    }

    public static Project includedBuildProject(IncludedBuildInternal includedBuild,
            final ProjectComponentIdentifier componentIdentifier) {
        return includedBuild.getTarget().getMutableModel().getRootProject().findProject(
                componentIdentifier.getProjectPath());
    }

    public static Path serializeAppModel(ApplicationModel appModel, Task context, boolean test) throws IOException {
        final Path serializedModel = context.getTemporaryDir().toPath()
                .resolve("quarkus-app" + (test ? "-test" : "") + "-model.dat");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(appModel);
        }
        return serializedModel;
    }

    public static ApplicationModel create(Project project, LaunchMode mode) {
        final ModelParameter params = new ModelParameterImpl();
        params.setMode(mode.toString());
        return create(project, params);
    }

    public static ApplicationModel create(Project project, ModelParameter params) {
        return (ApplicationModel) new GradleApplicationModelBuilder().buildAll(ApplicationModel.class.getName(), params,
                project);
    }

}
