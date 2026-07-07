package io.quarkus.gradle.model.tasks;

import javax.inject.Inject;

import io.quarkus.runtime.LaunchMode;

/**
 * Shared task type that serializes one Quarkus application model for normal, development, or test launch mode.
 * <p>
 * Construction assigns the launch mode and a mode-specific output convention under
 * {@code quarkus/application-model} in the project's build directory. Plugin configurators may replace that convention.
 * The type is shared Gradle plugin infrastructure, not a user-facing DSL model.
 */
public abstract class GenerateApplicationModelTask extends QuarkusApplicationModelTask {

    /**
     * Creates a task for one supported launch mode and applies its conventional output path.
     *
     * @param launchMode normal, development, or test mode
     * @throws IllegalArgumentException if the launch mode cannot generate an application model
     */
    @Inject
    public GenerateApplicationModelTask(LaunchMode launchMode) {
        getLaunchMode().set(launchMode);
        getApplicationModel().convention(getLaunchMode()
                .flatMap(mode -> getProject().getLayout().getBuildDirectory()
                        .file(applicationModelPath(mode, false))));
    }

    /**
     * Returns the conventional singleton task name for a launch mode.
     *
     * @param launchMode normal, development, or test mode
     * @return the conventional task name
     * @throws IllegalArgumentException for modes other than normal, development, and test
     */
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
