package io.quarkus.bootstrap.resolver;

import java.io.File;
import java.util.List;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.wrapper.GradleUserHomeLookup;

import io.quarkus.bootstrap.model.ApplicationModel;

public class QuarkusGradleModelFactory {

    public static ApplicationModel create(File projectDir, String mode, String... tasks) {
        return create(projectDir, mode, List.of(), tasks);
    }

    public static ApplicationModel create(File projectDir, String mode, List<String> jvmArgs, String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome())
                .connect()) {
            return connection.action(new QuarkusModelBuildAction(mode)).forTasks(tasks).addJvmArguments(jvmArgs).run();
        }
    }

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

}
