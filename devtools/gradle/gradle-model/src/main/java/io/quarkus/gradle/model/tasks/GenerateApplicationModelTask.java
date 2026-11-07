package io.quarkus.gradle.model.tasks;

import javax.inject.Inject;

import io.quarkus.runtime.LaunchMode;

/**
 * Shared task type for Gradle plugins that need to serialize a Quarkus application model for a specific launch mode.
 */
public abstract class GenerateApplicationModelTask extends QuarkusApplicationModelTask {

    @Inject
    public GenerateApplicationModelTask(LaunchMode launchMode) {
        getLaunchMode().set(launchMode);
        getApplicationModel().convention(getLaunchMode()
                .flatMap(mode -> getProject().getLayout().getBuildDirectory()
                        .file(applicationModelPath(mode, false))));
    }

    public static String taskName(LaunchMode launchMode) {
        return switch (launchMode) {
            case NORMAL -> "quarkusGenerateAppModel";
            case DEVELOPMENT -> "quarkusGenerateDevAppModel";
            case TEST -> "quarkusGenerateTestAppModel";
            default -> throw new IllegalArgumentException("Unsupported launch mode " + launchMode);
        };
    }

    static String applicationModelPath(LaunchMode launchMode, boolean buildModel) {
        return switch (launchMode) {
            case TEST -> {
                if (buildModel) {
                    throw new IllegalArgumentException("BUILD_MODEL mode is not supported for LaunchMode.TEST");
                }
                yield "quarkus/application-model/quarkus-app-test-model.dat";
            }
            case DEVELOPMENT -> {
                if (buildModel) {
                    throw new IllegalArgumentException("BUILD_MODEL mode is not supported for LaunchMode.DEVELOPMENT");
                }
                yield "quarkus/application-model/quarkus-app-dev-model.dat";
            }
            case NORMAL -> {
                yield buildModel ? "quarkus/application-model/quarkus-app-model-build.dat"
                        : "quarkus/application-model/quarkus-app-model.dat";
            }
            case RUN -> {
                throw new IllegalArgumentException("RUN mode is not supported for application model generation");
            }
        };
    }
}
