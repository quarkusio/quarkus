package io.quarkus.deployment.pkg.jar;

import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.maven.dependency.ResolvedDependency;

public class FastJarFormat {

    public static final String QUARKUS_RUN_JAR = "quarkus-run.jar";
    public static final String DEFAULT_FAST_JAR_DIRECTORY_NAME = "quarkus-app";
    public static final String BOOT_LIB = "boot";
    public static final String LIB = "lib";
    public static final String MAIN = "main";
    public static final String APP = "app";
    public static final String DEPLOYMENT_LIB = "deployment";
    public static final String QUARKUS = "quarkus";
    public static final String QUARKUS_APP_DEPS = "quarkus-app-dependencies.txt";
    public static final String DEPLOYMENT_CLASS_PATH_DAT = "deployment-class-path.dat";
    public static final String BUILD_SYSTEM_PROPERTIES = "build-system.properties";
    public static final String APPMODEL_DAT = "appmodel.dat";

    static final String GENERATED_BYTECODE_JAR = "generated-bytecode.jar";
    static final String TRANSFORMED_BYTECODE_JAR = "transformed-bytecode.jar";

    /**
     * Returns a JAR file name to be used for a content of a dependency, depending on whether the resolved path
     * is a directory or not.
     * We don't use getFileName() for directories, since directories would often be "classes" ending up merging
     * content from multiple dependencies in the same package
     *
     * @param dep dependency
     * @param resolvedPath path of the resolved dependency
     * @return JAR file name
     */
    public static String getJarFileName(ResolvedDependency dep, Path resolvedPath) {
        boolean isDirectory = Files.isDirectory(resolvedPath);
        if (!isDirectory) {
            return dep.getGroupId() + "." + resolvedPath.getFileName();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(dep.getGroupId()).append(".").append(dep.getArtifactId()).append("-");
        if (!dep.getClassifier().isEmpty()) {
            sb.append(dep.getClassifier()).append("-");
        }
        return sb.append(dep.getVersion()).append(".").append(dep.getType()).toString();
    }
}
