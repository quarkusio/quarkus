package io.quarkus.bootstrap.utils;

import static io.quarkus.bootstrap.util.QuarkusModelHelper.DEVMODE_REQUIRED_TASKS;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.QuarkusGradleModelFactory;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper class used to expose build tool used by the project
 */
public class BuildToolHelper {

    public enum BuildTool {
        MAVEN("pom.xml"),
        GRADLE("build.gradle", "build.gradle.kts");

        private final String[] buildFiles;

        BuildTool(String... buildFile) {
            this.buildFiles = buildFile;
        }

        public String[] getBuildFiles() {
            return buildFiles;
        }

        public boolean exists(Path root) {
            for (String buildFile : buildFiles) {
                if (Files.exists(root.resolve(buildFile))) {
                    return true;
                }
            }
            return false;
        }
    }

    private BuildToolHelper() {

    }

    public static boolean isMavenProject(Path project) {
        Path currentPath = project;
        while (currentPath != null) {
            if (BuildTool.MAVEN.exists(currentPath)) {
                return true;
            }
            if (BuildTool.GRADLE.exists(currentPath)) {
                return false;
            }
            currentPath = currentPath.getParent();
        }
        return false;
    }

    public static Path getBuildFile(Path project, BuildTool tool) {
        Path currentPath = project;
        while (currentPath != null) {
            if (tool.exists(currentPath)) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }
        return null;
    }

    public static QuarkusModel enableGradleAppModel(Path projectRoot, String mode, String... tasks)
            throws IOException, AppModelResolverException {
        if (isMavenProject(projectRoot)) {
            return null;
        }
        final QuarkusModel model = QuarkusGradleModelFactory.create(getBuildFile(projectRoot, BuildTool.GRADLE).toFile(),
                mode, tasks);
        QuarkusModelHelper.exportModel(model);
        return model;
    }

    public static QuarkusModel enableGradleAppModelForDevMode(Path projectRoot) throws IOException, AppModelResolverException {
        if (isMavenProject(projectRoot)) {
            return null;
        }
        final QuarkusModel model = QuarkusGradleModelFactory
                .createForTasks(getBuildFile(projectRoot, BuildTool.GRADLE).toFile(), DEVMODE_REQUIRED_TASKS);
        QuarkusModelHelper.exportModel(model);
        return model;
    }

}
