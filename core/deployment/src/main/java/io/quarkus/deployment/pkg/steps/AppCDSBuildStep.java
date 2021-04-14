package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSContainerImageBuildItem;
import io.quarkus.deployment.pkg.builditem.AppCDSRequestedBuildItem;
import io.quarkus.deployment.pkg.builditem.AppCDSResultBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.steps.MainClassBuildStep;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.utilities.JavaBinFinder;

public class AppCDSBuildStep {

    private static final Logger log = Logger.getLogger(AppCDSBuildStep.class);

    public static final String CLASSES_LIST_FILE_NAME = "classes.lst";
    private static final String CONTAINER_IMAGE_BASE_BUILD_DIR = "/tmp/quarkus";
    private static final String CONTAINER_IMAGE_APPCDS_DIR = CONTAINER_IMAGE_BASE_BUILD_DIR + "/appcds";

    @BuildStep(onlyIf = AppCDSRequired.class)
    public void requested(OutputTargetBuildItem outputTarget, BuildProducer<AppCDSRequestedBuildItem> producer)
            throws IOException {
        Path appCDSDir = outputTarget.getOutputDirectory().resolve("appcds");
        IoUtils.createOrEmptyDir(appCDSDir);

        producer.produce(new AppCDSRequestedBuildItem(outputTarget.getOutputDirectory().resolve("appcds")));
    }

    @BuildStep
    public void build(Optional<AppCDSRequestedBuildItem> appCDsRequested,
            JarBuildItem jarResult, OutputTargetBuildItem outputTarget, PackageConfig packageConfig,
            Optional<AppCDSContainerImageBuildItem> appCDSContainerImage,
            BuildProducer<AppCDSResultBuildItem> appCDS,
            BuildProducer<ArtifactResultBuildItem> artifactResult) throws Exception {
        if (!appCDsRequested.isPresent()) {
            return;
        }

        // to actually execute the commands needed to generate the AppCDS file, either the JVM in the container image will be used
        // (if specified), or the JVM running the build
        String containerImage = determineContainerImage(packageConfig, appCDSContainerImage);
        String javaBinPath = null;
        if (containerImage == null) {
            javaBinPath = System.getProperty("java.home") + File.separator + "bin" + File.separator
                    + JavaBinFinder.simpleBinaryName();
            if (!new File(javaBinPath).canExecute()) {
                log.warnf(
                        "In order to create AppCDS the JDK used to build the Quarkus application must contain an executable named '%s' in its 'bin' directory.",
                        javaBinPath);
                return;
            }
        }

        Path classesLstPath = createClassesLst(jarResult, outputTarget, javaBinPath, containerImage,
                appCDsRequested.get().getAppCDSDir(), packageConfig.isFastJar());
        if (classesLstPath == null) {
            return;
        }

        log.debugf("'%s' successfully created.", CLASSES_LIST_FILE_NAME);

        log.info("Launching AppCDS creation process.");
        Path appCDSPath = createAppCDS(jarResult, outputTarget, javaBinPath, containerImage, classesLstPath,
                packageConfig.isFastJar());
        if (appCDSPath == null) {
            log.warn("Unable to create AppCDS.");
            return;
        }

        log.infof("AppCDS successfully created at: '%s'.", appCDSPath.toAbsolutePath().toString());
        if (containerImage == null) {
            log.infof(
                    "To ensure they are loaded properly, " +
                            "run the application jar from its directory and also add the '-XX:SharedArchiveFile=app-cds.jsa' " +
                            "JVM flag.\nMoreover, make sure to use the exact same Java version (%s) to run the application as was used to build it.",
                    System.getProperty("java.version"));
        }

        appCDS.produce(new AppCDSResultBuildItem(appCDSPath));
        artifactResult.produce(new ArtifactResultBuildItem(appCDSPath, "appCDS", Collections.emptyMap()));
    }

    private String determineContainerImage(PackageConfig packageConfig,
            Optional<AppCDSContainerImageBuildItem> appCDSContainerImage) {
        if (packageConfig.appcdsBuilderImage.isPresent()) {
            return packageConfig.appcdsBuilderImage.get();
        } else if (appCDSContainerImage.isPresent()) {
            return appCDSContainerImage.get().getContainerImage();
        }
        return null;
    }

    /**
     * @return The path of the created classes.lst file or null if the file was not created
     */
    private Path createClassesLst(JarBuildItem jarResult,
            OutputTargetBuildItem outputTarget, String javaBinPath, String containerImage, Path appCDSDir, boolean isFastJar) {

        List<String> commonJavaArgs = new ArrayList<>(3);
        commonJavaArgs.add("-XX:DumpLoadedClassList=" + CLASSES_LIST_FILE_NAME);
        commonJavaArgs.add(String.format("-D%s=true", MainClassBuildStep.GENERATE_APP_CDS_SYSTEM_PROPERTY));
        commonJavaArgs.add("-jar");

        List<String> command;
        if (containerImage != null) {
            List<String> dockerRunCommand = dockerRunCommands(outputTarget, containerImage, CONTAINER_IMAGE_APPCDS_DIR);
            command = new ArrayList<>(dockerRunCommand.size() + 1 + commonJavaArgs.size());
            command.addAll(dockerRunCommand);
            command.add("java");
            command.addAll(commonJavaArgs);
            if (isFastJar) {
                command.add(CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME + "/"
                        + JarResultBuildStep.QUARKUS_RUN_JAR);
            } else {
                command.add(CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + jarResult.getPath().getFileName().toString());
            }
        } else {
            command = new ArrayList<>(2 + commonJavaArgs.size());
            command.add(javaBinPath);
            command.addAll(commonJavaArgs);
            if (isFastJar) {
                command
                        .add(jarResult.getLibraryDir().getParent().resolve(JarResultBuildStep.QUARKUS_RUN_JAR).toAbsolutePath()
                                .toString());
            } else {
                command.add(jarResult.getPath().toAbsolutePath().toString());
            }
        }

        if (log.isDebugEnabled()) {
            log.debugf("Launching command: '%s' to create '" + CLASSES_LIST_FILE_NAME + "' file.", String.join(" ", command));
        }

        int exitCode;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .directory(appCDSDir.toFile());
            if (log.isDebugEnabled()) {
                processBuilder.inheritIO();
            } else {
                processBuilder.redirectError(NULL_FILE);
                processBuilder.redirectOutput(NULL_FILE);
            }
            exitCode = processBuilder.start().waitFor();
        } catch (Exception e) {
            log.debug("Failed to launch process used to create '" + CLASSES_LIST_FILE_NAME + "'.", e);
            return null;
        }

        if (exitCode != 0) {
            log.debugf("The process that was supposed to create AppCDS exited with error code: %d.", exitCode);
            return null;
        }

        Path result = appCDSDir.resolve(CLASSES_LIST_FILE_NAME);
        if (!Files.exists(result)) {
            log.warnf("Unable to create AppCDS because '%s' was not created. Check the logs for details",
                    CLASSES_LIST_FILE_NAME);
            return null;
        }
        return result;
    }

    // the idea here is to use 'docker run -v ... java ...' in order to utilize the JVM of the builder image to
    // generate the classes file on the host
    private List<String> dockerRunCommands(OutputTargetBuildItem outputTarget, String containerImage,
            String containerWorkingDir) {
        List<String> command = new ArrayList<>(10);
        command.add("docker");
        command.add("run");
        command.add("-v");
        command.add(outputTarget.getOutputDirectory().toAbsolutePath().toString() + ":" + CONTAINER_IMAGE_BASE_BUILD_DIR
                + ":z");
        if (SystemUtils.IS_OS_LINUX) {
            String uid = getLinuxID("-ur");
            String gid = getLinuxID("-gr");
            if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                command.add("--user");
                command.add(uid + ":" + gid);
            }
        }
        command.add("-w");
        command.add(containerWorkingDir);
        command.add("--rm");
        command.add(containerImage);
        return command;
    }

    /**
     * @return The path of the created app-cds.jsa file or null if the file was not created
     */
    private Path createAppCDS(JarBuildItem jarResult, OutputTargetBuildItem outputTarget, String javaBinPath,
            String containerImage, Path classesLstPath, boolean isFastFar) {

        Path workingDirectory = jarResult.getPath().getParent();
        Path appCDSPath = workingDirectory.resolve("app-cds.jsa");
        if (appCDSPath.toFile().exists()) {
            try {
                Files.delete(appCDSPath);
            } catch (IOException e) {
                log.debug("Unable to delete existing 'app-cds.jsa' file.", e);
            }
        }

        List<String> javaArgs = new ArrayList<>(5);
        javaArgs.add("-Xshare:dump");
        javaArgs.add("-XX:SharedClassListFile="
                + ((containerImage != null) ? CONTAINER_IMAGE_APPCDS_DIR + "/" + classesLstPath.getFileName().toString()
                        : classesLstPath.toAbsolutePath().toString()));
        // We use the relative paths because at runtime 'java -XX:SharedArchiveFile=... -jar ...' expects the AppCDS and jar files
        // to match exactly what was used at build time.
        // For that reason we also run the creation process from inside the output directory,
        // The end result is that users can simply use 'java -XX:SharedArchiveFile=app-cds.jsa -jar app.jar'
        javaArgs.add("-XX:SharedArchiveFile=" + appCDSPath.getFileName().toString());
        javaArgs.add("--class-path");
        if (isFastFar) {
            javaArgs.add(JarResultBuildStep.QUARKUS_RUN_JAR);
        } else {
            javaArgs.add(jarResult.getPath().getFileName().toString());
        }

        List<String> command;
        if (containerImage != null) {
            List<String> dockerRunCommand = dockerRunCommands(outputTarget, containerImage,
                    CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME);
            command = new ArrayList<>(dockerRunCommand.size() + 1 + javaArgs.size());
            command.addAll(dockerRunCommand);
            command.add("java");
            command.addAll(javaArgs);
        } else {
            command = new ArrayList<>(1 + javaArgs.size());
            command.add(javaBinPath);
            command.addAll(javaArgs);
        }

        if (log.isDebugEnabled()) {
            log.debugf("Launching command: '%s' to create final AppCDS.", String.join(" ", command));
        }

        int exitCode;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile());
            if (log.isDebugEnabled()) {
                processBuilder.inheritIO();
            } else {
                processBuilder.redirectError(NULL_FILE);
                processBuilder.redirectOutput(NULL_FILE);
            }
            exitCode = processBuilder.start().waitFor();
        } catch (Exception e) {
            log.debug("Failed to launch process used to create AppCDS.", e);
            return null;
        }

        if (exitCode != 0) {
            log.debugf("The process that was supposed to create AppCDS exited with error code: %d.", exitCode);
            return null;
        }

        if (!appCDSPath.toFile().exists()) { // shouldn't happen, but let's avoid any surprises
            return null;
        }

        return appCDSPath;
    }

    static class AppCDSRequired implements BooleanSupplier {

        private final PackageConfig packageConfig;
        private final LaunchMode launchMode;

        AppCDSRequired(PackageConfig packageConfig, LaunchMode launchMode) {
            this.packageConfig = packageConfig;
            this.launchMode = launchMode;
        }

        @Override
        public boolean getAsBoolean() {
            if (launchMode != LaunchMode.NORMAL) {
                return false;
            }

            if (!packageConfig.createAppcds || !packageConfig.isAnyJarType()) {
                return false;
            }

            return true;
        }
    }

    // copied from Java 9
    // TODO remove when we move to Java 11

    private static final File NULL_FILE = new File(SystemUtils.IS_OS_WINDOWS ? "NUL" : "/dev/null");
}
