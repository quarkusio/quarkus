package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

public class QuarkusGradleModelFactory {

    public static QuarkusModel create(File projectDir, String mode, String... tasks) {
        return create(projectDir, mode, Collections.emptyList(), tasks);
    }

    public static QuarkusModel create(File projectDir, String mode, List<String> jvmArgs, String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            connection.newBuild().forTasks(tasks).addJvmArguments(jvmArgs).run();

            return connection.action(new QuarkusModelBuildAction(mode)).run();
        }
    }

    public static QuarkusModel createForTasks(File projectDir, String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            final ModelBuilder<QuarkusModel> modelBuilder = connection.model(QuarkusModel.class);
            if (tasks.length != 0) {
                modelBuilder.forTasks(tasks);
            }
            return modelBuilder.get();
        }
    }

}
