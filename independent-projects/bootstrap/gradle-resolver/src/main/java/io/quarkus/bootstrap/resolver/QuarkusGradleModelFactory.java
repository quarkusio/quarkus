package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.model.ApplicationModel;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

public class QuarkusGradleModelFactory {

    public static ApplicationModel create(File projectDir, String mode, String... tasks) {
        return create(projectDir, mode, Collections.emptyList(), tasks);
    }

    public static ApplicationModel create(File projectDir, String mode, List<String> jvmArgs, String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            return connection.action(new QuarkusModelBuildAction(mode)).forTasks(tasks).addJvmArguments(jvmArgs).run();
        }
    }

    public static ApplicationModel createForTasks(File projectDir, String... tasks) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            final ModelBuilder<ApplicationModel> modelBuilder = connection.model(ApplicationModel.class);
            if (tasks.length != 0) {
                modelBuilder.forTasks(tasks);
            }
            return modelBuilder.get();
        }
    }

}
