package io.quarkus.bootstrap.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.QuarkusGradleModelFactory;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Helper class used to expose build tool used by the project
 */
public class BuildToolHelper {

    private static final Logger log = Logger.getLogger(BuildToolHelper.class);

    private final static String[] DEVMODE_REQUIRED_TASKS = new String[] { "classes" };
    private final static String[] TEST_REQUIRED_TASKS = new String[] { "classes", "testClasses", "integrationTestClasses" };
    private final static List<String> ENABLE_JAR_PACKAGING = List.of("-Dorg.gradle.java.compile-classpath-packaging=true");

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

    /**
     * Returns the application module directory, if the application is built from a source project.
     * For a single module project it will the project directory. For a multimodule project,
     * it will be the directory of the application module.
     * <p>
     * During re-augmentation of applications packaged as {@code mutable-jar} this method will return the current directory,
     * since the source project might not be available anymore.
     *
     * @return application module directory, never null
     */
    public static Path getApplicationModuleOrCurrentDirectory(ApplicationModel appModel) {
        return getModuleOrCurrentDirectory(appModel.getAppArtifact());
    }

    private static Path getModuleOrCurrentDirectory(ResolvedDependency resolvedDep) {
        final WorkspaceModule module = resolvedDep.getWorkspaceModule();
        if (module != null) {
            return module.getModuleDir().toPath();
        }
        var paths = resolvedDep.getResolvedPaths();
        for (var path : paths) {
            if (Files.isDirectory(path)) {
                var moduleDir = BuildToolHelper.getProjectDir(path);
                if (moduleDir != null) {
                    return moduleDir;
                }
            }
        }
        // the module isn't available, return the current directory
        return Path.of("").toAbsolutePath();
    }

    public static Path getProjectDir(Path p) {
        Path currentPath = p;
        while (currentPath != null) {
            if (BuildTool.MAVEN.exists(currentPath) || BuildTool.GRADLE.exists(currentPath)) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }
        log.warnv("Unable to find the project directory for {0}.", p);
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
        log.warnv("Unable to find a build tool in {0} or in any parent.", project);
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

    public static ApplicationModel enableGradleAppModelForTest(Path projectRoot)
            throws IOException, AppModelResolverException {
        // We enable jar packaging since we want test-fixtures as jars
        return enableGradleAppModel(projectRoot, "TEST", ENABLE_JAR_PACKAGING, TEST_REQUIRED_TASKS);
    }

    public static ApplicationModel enableGradleAppModelForProdMode(Path projectRoot)
            throws IOException, AppModelResolverException {
        return enableGradleAppModel(projectRoot, "NORMAL", List.of());
    }

    public static ApplicationModel enableGradleAppModel(Path projectRoot, String mode, List<String> jvmArgs, String... tasks)
            throws IOException, AppModelResolverException {
        if (isGradleProject(projectRoot)) {
            log.infof("Loading Quarkus Gradle application model for %s", projectRoot);
            final ApplicationModel model = QuarkusGradleModelFactory.create(
                    getBuildFile(projectRoot, BuildTool.GRADLE).toFile(),
                    mode, jvmArgs, tasks);
            ApplicationModelSerializer.exportGradleModel(model, "TEST".equalsIgnoreCase(mode));
            return model;
        }
        return null;
    }

    public static ApplicationModel enableGradleAppModelForDevMode(Path projectRoot)
            throws IOException, AppModelResolverException {
        if (isGradleProject(projectRoot)) {
            final ApplicationModel model = QuarkusGradleModelFactory
                    .createForTasks(getBuildFile(projectRoot, BuildTool.GRADLE).toFile(), DEVMODE_REQUIRED_TASKS);
            ApplicationModelSerializer.exportGradleModel(model, false);
            return model;
        }
        return null;
    }

}
