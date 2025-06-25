package io.quarkus.gradle.tooling;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.invocation.Gradle;
import org.gradle.composite.internal.DefaultIncludedBuild;
import org.gradle.internal.composite.IncludedBuildInternal;
import org.gradle.internal.composite.IncludedRootBuild;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.maven.dependency.ArtifactCoords;
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

    public static IncludedBuild includedBuild(final Project project, final String buildPath) {
        Gradle currentGradle = project.getRootProject().getGradle();
        while (null != currentGradle) {
            for (IncludedBuild ib : currentGradle.getIncludedBuilds()) {
                if (ib instanceof IncludedRootBuild) {
                    continue;
                }

                if (((IncludedBuildInternal) ib).getTarget().getBuildIdentifier().getBuildPath().equals(buildPath)) {
                    return ib;
                }
            }

            currentGradle = currentGradle.getParent();
        }

        return null;
    }

    public static Project includedBuildProject(IncludedBuildInternal includedBuild, final String projectPath) {
        return includedBuild.getTarget().getMutableModel().getRootProject().findProject(projectPath);
    }

    public static Project findLocalProject(final Project project, final String projectPath) {
        if (projectPath.startsWith(":")) {
            return project.getRootProject().findProject(projectPath);
        } else {
            Project currentProject = project;
            while (currentProject != null) {
                final Project foundProject = currentProject.findProject(projectPath);
                if (foundProject != null) {
                    return foundProject;
                }

                currentProject = currentProject.getParent();
            }

            return null;
        }
    }

    public static Project findLocalProject(final Project project, final ArtifactCoords artifactCoords) {
        for (Project subproject : project.getRootProject().getSubprojects()) {
            if (subproject.getGroup().equals(artifactCoords.getGroupId()) &&
                    subproject.getName().equals(artifactCoords.getArtifactId()) &&
                    (artifactCoords.getVersion() == null || subproject.getVersion().equals(artifactCoords.getVersion()))) {
                return subproject;
            }
        }

        return null;
    }

    public static Project findIncludedProject(Project project, ExternalModuleDependency dependency) {
        for (IncludedBuild ib : project.getRootProject().getGradle().getIncludedBuilds()) {
            if (ib instanceof IncludedRootBuild) {
                continue;
            }

            final Project includedBuildProject = findIncludedBuildProject(ib, dependency);
            if (includedBuildProject != null) {
                return includedBuildProject;
            }
        }

        try {
            final Gradle parentGradle = project.getRootProject().getGradle().getParent();
            if (parentGradle != null) {
                return findIncludedProject(parentGradle.getRootProject(), dependency);
            } else {
                return null;
            }
        } catch (IllegalStateException ise) {
            // This can happen if the project itself is in an included build, which means that the root-project
            // is not yet known, so `DefaultGradle.getRootProject()` throws an ISE.
            return null;
        }
    }

    private static Project findLocalProject(Project project, ExternalModuleDependency dependency) {
        for (Project p : project.getRootProject().getSubprojects()) {
            if (Objects.equals(p.getGroup(), dependency.getGroup())
                    && Objects.equals(p.getName(), dependency.getName())
                    && (dependency.getVersion() == null || Objects.equals(p.getVersion(), dependency.getVersion()))) {
                return p;
            }
        }

        return null;
    }

    private static Project findIncludedBuildProject(IncludedBuild ib, ExternalModuleDependency dependency) {
        if (!(ib instanceof DefaultIncludedBuild.IncludedBuildImpl)) {
            return null;
        }

        final DefaultIncludedBuild.IncludedBuildImpl dib = (DefaultIncludedBuild.IncludedBuildImpl) ib;
        try {
            final Project rootProject = dib.getTarget().getMutableModel().getRootProject();

            return findLocalProject(rootProject, dependency);
        } catch (IllegalStateException ise) {
            // This can happen if the project itself is in an included build, which means that the root-project
            // is not yet known, so `DefaultGradle.getRootProject()` throws an ISE.
            return null;
        }
    }

    public static Path serializeAppModel(ApplicationModel appModel, Task context, boolean test) throws IOException {
        final Path serializedModel = context.getTemporaryDir().toPath()
                .resolve("quarkus-app" + (test ? "-test" : "") + "-model.dat");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(appModel);
        }
        return serializedModel;
    }

    public static ApplicationModel deserializeAppModel(Path path) throws IOException {
        try (ObjectInputStream out = new ObjectInputStream(Files.newInputStream(path))) {
            return (ApplicationModel) out.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path serializeAppModel(ApplicationModel appModel, Path serializedModelPath) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModelPath))) {
            out.writeObject(appModel);
        }
        return serializedModelPath;
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

    /**
     * This method is meant to figure out the output directory containing class files for a compile task
     * which is not available in the plugin classpath. An example would be KotlinCompile.
     *
     * @param compileTask a compile task
     */
    public static File getClassesOutputDir(Task compileTask) {
        if (compileTask.getOutputs().getHasOutput()) {
            final AtomicReference<File> result = new AtomicReference<>();
            compileTask.getOutputs().getFiles().getAsFileTree().visit(visitor -> {
                // We are looking for the first class file, since a compile task would typically
                // have a single output location for classes.
                // There in fact could be a few output locations, the rest though would typically be some internal caching bits
                if (visitor.getName().endsWith(".class")) {
                    visitor.stopVisiting();
                    var file = visitor.getFile();
                    int relativeSegments = visitor.getRelativePath().getSegments().length;
                    while (file != null && relativeSegments > 0) {
                        relativeSegments--;
                        file = file.getParentFile();
                    }
                    if (file != null) {
                        result.set(file);
                    }
                }
            });
            return result.get();
        }
        return null;
    }
}
