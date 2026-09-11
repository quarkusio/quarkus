package io.quarkus.bootstrap.resolver;

import java.io.File;
import java.util.List;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.wrapper.GradleUserHomeLookup;

import io.quarkus.bootstrap.model.ApplicationModel;

/**
 * Loads Quarkus application models through the Gradle Tooling API.
 * <p>
 * Every operation creates and closes its own {@link ProjectConnection} and uses the configured Gradle user home.
 */
public class QuarkusGradleModelFactory {

    /**
     * Loads an application model after executing the requested Gradle tasks with the default build JVM arguments.
     *
     * @param projectDir Gradle project directory; must not be {@code null}
     * @param mode mode passed to the Quarkus model provider
     * @param tasks tasks to execute before loading the model; empty executes the default tasks and {@code null}
     *        executes no tasks
     * @return the application model returned by the Quarkus Tooling API action
     */
    public static ApplicationModel create(File projectDir, String mode, String... tasks) {
        return create(projectDir, mode, List.of(), tasks);
    }

    /**
     * Loads an application model after executing the requested Gradle tasks.
     *
     * @param projectDir Gradle project directory; must not be {@code null}
     * @param mode mode passed to the Quarkus model provider
     * @param jvmArgs JVM arguments for the Gradle build process; must not be {@code null}
     * @param tasks tasks to execute before loading the model; empty executes the default tasks and {@code null}
     *        executes no tasks
     * @return the application model returned by the Quarkus Tooling API action
     */
    public static ApplicationModel create(File projectDir, String mode, List<String> jvmArgs, String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome())
                .connect()) {
            return connection.action(new QuarkusModelBuildAction(mode)).forTasks(tasks).addJvmArguments(jvmArgs).run();
        }
    }

    /**
     * Loads Gradle's directly exposed Quarkus application model after executing the requested tasks.
     * <p>
     * Unlike {@link #create(File, String, String...)}, this method does not use a Quarkus build action or carry an
     * explicit launch mode.
     *
     * @param projectDir Gradle project directory; must not be {@code null}
     * @param tasks tasks to execute before loading the model; must not be {@code null}; an empty array does not
     *        configure task execution
     * @return the application model exposed by the Gradle project
     * @throws NullPointerException if {@code tasks} is {@code null}
     */
    public static ApplicationModel createForTasks(File projectDir, String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome())
                .connect()) {
            final ModelBuilder<ApplicationModel> modelBuilder = connection.model(ApplicationModel.class);
            if (tasks.length != 0) {
                modelBuilder.forTasks(tasks);
            }
            return modelBuilder.get();
        }
    }

    /**
     * Loads the development-mode application model and provider classification for a Gradle project.
     * <p>
     * The Tooling API invocation executes the supplied tasks before loading the models and uses the default Gradle JVM
     * arguments. An empty task array asks Gradle to execute the project's default tasks; a {@code null} array executes
     * no tasks.
     *
     * @param projectDir Gradle project directory; must not be {@code null}
     * @param tasks tasks to execute before loading the models; empty executes the default tasks and {@code null}
     *        executes no tasks
     * @return paired application model and provider classification
     */
    public static QuarkusToolingModelResult createPairedForTasks(File projectDir, String... tasks) {
        return createPaired(projectDir, "DEVELOPMENT", List.of(), tasks);
    }

    /**
     * Loads an application model and provider classification in one Gradle Tooling API invocation.
     * <p>
     * The connection uses the configured Gradle user home and is closed before this method returns. A standalone
     * provider returns a validated sidecar; a provider without the standalone marker returns an unmarked compatibility
     * result.
     *
     * Tasks execute before the action that loads the models. An empty task array asks Gradle to execute the project's
     * default tasks; a {@code null} array executes no tasks.
     *
     * @param projectDir Gradle project directory; must not be {@code null}
     * @param mode mode passed to the application-model and sidecar providers; a standalone sidecar provider requires
     *        {@code NORMAL}, {@code DEVELOPMENT}, or {@code TEST}
     * @param jvmArgs JVM arguments for the Gradle build process; must not be {@code null}
     * @param tasks tasks to execute before loading the models; empty executes the default tasks and {@code null}
     *        executes no tasks
     * @return paired application model and provider classification
     */
    public static QuarkusToolingModelResult createPaired(File projectDir, String mode, List<String> jvmArgs,
            String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome())
                .connect()) {
            return connection.action(new QuarkusToolingModelBuildAction(mode))
                    .forTasks(tasks)
                    .addJvmArguments(jvmArgs)
                    .run();
        }
    }

}
