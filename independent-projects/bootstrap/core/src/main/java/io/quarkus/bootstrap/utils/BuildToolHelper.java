package io.quarkus.bootstrap.utils;

import static io.quarkus.bootstrap.util.QuarkusModelHelper.DEVMODE_REQUIRED_TASKS;
import static io.quarkus.bootstrap.util.QuarkusModelHelper.ENABLE_JAR_PACKAGING;
import static io.quarkus.bootstrap.util.QuarkusModelHelper.TEST_REQUIRED_TASKS;

import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.QuarkusGradleModelFactory;
import io.quarkus.bootstrap.util.QuarkusModelHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Helper class used to expose build tool used by the project
 */
public class BuildToolHelper {

    private static final Logger log = Logger.getLogger(BuildToolHelper.class);

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

    public static Path getProjectDir(Path p) {
        Path currentPath = p;
        while (currentPath != null) {
            if (BuildTool.MAVEN.exists(currentPath) || BuildTool.GRADLE.exists(currentPath)) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }
        log.warnv("Unable to find a project directory for {0}.", p.toString());
        return null;
    }

    public static BuildTool findBuildTool(Path project) {
        Path currentPath = project;
        while (currentPath != null) {
            if (BuildTool.MAVEN.exists(currentPath)) {
                return BuildTool.MAVEN;
            }
            if (BuildTool.GRADLE.exists(currentPath)) {
                return BuildTool.GRADLE;
            }
            currentPath = currentPath.getParent();
        }
        log.warnv("Unable to find a build tool in {0} or in any parent.", project.toString());
        return null;
    }

    public static boolean isMavenProject(Path project) {
        return findBuildTool(project) == BuildTool.MAVEN;
    }

    public static boolean isGradleProject(Path project) {
        return findBuildTool(project) == BuildTool.GRADLE;
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

    public static QuarkusModel enableGradleAppModelForTest(Path projectRoot)
            throws IOException, AppModelResolverException {
        // We enable jar packaging since we want test-fixtures as jars
        return enableGradleAppModel(projectRoot, "TEST", ENABLE_JAR_PACKAGING, TEST_REQUIRED_TASKS);
    }

    public static QuarkusModel enableGradleAppModel(Path projectRoot, String mode, List<String> jvmArgs, String... tasks)
            throws IOException, AppModelResolverException {
        if (isGradleProject(projectRoot)) {
            final QuarkusModel model = QuarkusGradleModelFactory.create(getBuildFile(projectRoot, BuildTool.GRADLE).toFile(),
                    mode, jvmArgs, tasks);
            QuarkusModelHelper.exportModel(model, "TEST".equalsIgnoreCase(mode));
            return model;
        }
        return null;
    }

    public static QuarkusModel enableGradleAppModelForDevMode(Path projectRoot)
            throws IOException, AppModelResolverException {
        if (isGradleProject(projectRoot)) {
            final QuarkusModel model = QuarkusGradleModelFactory
                    .createForTasks(getBuildFile(projectRoot, BuildTool.GRADLE).toFile(), DEVMODE_REQUIRED_TASKS);
            QuarkusModelHelper.exportModel(model, false);
            return model;
        }
        return null;
    }

}
