package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import java.io.File;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

public class QuarkusGradleModelFactory {

    public static QuarkusModel create(File projectDir, String mode) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            return connection.action(new QuarkusModelBuildAction(mode)).run();
        }
    }

    public static QuarkusModel createForTasks(File projectDir, String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            final ModelBuilder<QuarkusModel> modelBuilder = connection.model(QuarkusModel.class);
            modelBuilder.forTasks(tasks);
            return modelBuilder.get();
        }
    }

}
