package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.FAST_JAR;
import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;
import static io.quarkus.deployment.util.ContainerRuntimeUtil.detectContainerRuntime;

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
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveContainerImageBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveRequestedBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.steps.MainClassBuildStep;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.utilities.JavaBinFinder;

public class JvmStartupOptimizerArchiveBuildStep {

    private static final Logger log = Logger.getLogger(JvmStartupOptimizerArchiveBuildStep.class);

    public static final String CLASSES_LIST_FILE_NAME = "classes.lst";
    private static final String CONTAINER_IMAGE_BASE_BUILD_DIR = "/tmp/quarkus";
    private static final String CONTAINER_IMAGE_APPCDS_DIR = CONTAINER_IMAGE_BASE_BUILD_DIR + "/appcds";

    @BuildStep(onlyIf = AppCDSRequired.class)
    public void requested(PackageConfig packageConfig, OutputTargetBuildItem outputTarget,
            BuildProducer<JvmStartupOptimizerArchiveRequestedBuildItem> producer)
            throws IOException {
        Path archiveDir = outputTarget.getOutputDirectory().resolve("jvmstartuparchive");
        IoUtils.createOrEmptyDir(archiveDir);

        producer.produce(
                new JvmStartupOptimizerArchiveRequestedBuildItem(outputTarget.getOutputDirectory().resolve("jvmstartuparchive"),
                        packageConfig.jar().appcds().useAot() ? JvmStartupOptimizerArchiveType.AOT
                                : JvmStartupOptimizerArchiveType.AppCDS));
    }

    @BuildStep(onlyIfNot = NativeOrNativeSourcesBuild.class)
    public void build(Optional<JvmStartupOptimizerArchiveRequestedBuildItem> requested,
            JarBuildItem jarResult, OutputTargetBuildItem outputTarget, PackageConfig packageConfig,
            CompiledJavaVersionBuildItem compiledJavaVersion,
            Optional<JvmStartupOptimizerArchiveContainerImageBuildItem> jvmStartupOptimizerArchiveContainerImage,
            BuildProducer<JvmStartupOptimizerArchiveResultBuildItem> jvmStartupOptimizerArchive,
            BuildProducer<ArtifactResultBuildItem> artifactResult) throws Exception {
        if (requested.isEmpty()) {
            return;
        }

        // to actually execute the commands needed to generate the AppCDS file, either the JVM in the container image will be used
        // (if specified), or the JVM running the build
        String containerImage = determineContainerImage(packageConfig, jvmStartupOptimizerArchiveContainerImage);
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

        Path archivePath;
        JvmStartupOptimizerArchiveType archiveType = requested.get().getType();
        log.infof("Launching %s creation process.", archiveType);
        boolean isFastJar = packageConfig.jar().type() == FAST_JAR;
        if (archiveType == JvmStartupOptimizerArchiveType.AppCDS) {
            archivePath = createAppCDSFromExit(jarResult, outputTarget, javaBinPath, containerImage,
                    isFastJar);
        } else if (archiveType == JvmStartupOptimizerArchiveType.AOT) {
            archivePath = createAot(jarResult, outputTarget, javaBinPath, containerImage, isFastJar);
        } else {
            throw new IllegalStateException("Unsupported archive type: " + archiveType);
        }

        if (archivePath == null) {
            log.warnf("Unable to create %s.", archiveType);
            return;
        }

        log.infof("%s archive successfully created at: '%s'.", archiveType, archivePath.toAbsolutePath().toString());
        if (containerImage == null) {
            if (archiveType == JvmStartupOptimizerArchiveType.AppCDS) {
                log.infof(
                        "To ensure they are loaded properly, " +
                                "run the application jar from its directory and also add the '-XX:SharedArchiveFile=app-cds.jsa' "
                                +
                                "JVM flag.\nMoreover, make sure to use the exact same Java version (%s) to run the application as was used to build it.",
                        System.getProperty("java.version"));
            } else {
                log.infof(
                        "To ensure they are loaded properly, " +
                                "run the application jar from its directory and also add the '-XX:AOTCache=app.aot' "
                                +
                                "JVM flag.\nMoreover, make sure to use the exact same Java version (%s) to run the application as was used to build it.",
                        System.getProperty("java.version"));
            }
        }

        jvmStartupOptimizerArchive.produce(new JvmStartupOptimizerArchiveResultBuildItem(archivePath));
        artifactResult.produce(new ArtifactResultBuildItem(archivePath, "appCDS", Collections.emptyMap()));
    }

    private String determineContainerImage(PackageConfig packageConfig,
            Optional<JvmStartupOptimizerArchiveContainerImageBuildItem> jvmStartupOptimizerArchiveContainer) {
        if (!packageConfig.jar().appcds().useContainer()) {
            return null;
        } else if (packageConfig.jar().appcds().builderImage().isPresent()) {
            return packageConfig.jar().appcds().builderImage().get();
        } else if (jvmStartupOptimizerArchiveContainer.isPresent()) {
            return jvmStartupOptimizerArchiveContainer.get().getContainerImage();
        }
        return null;
    }

    // the idea here is to use 'docker run -v ... java ...' in order to utilize the JVM of the builder image to
    // generate the classes file on the host
    private List<String> dockerRunCommands(OutputTargetBuildItem outputTarget, String containerImage,
            String containerWorkingDir) {
        ContainerRuntime containerRuntime = detectContainerRuntime(true);

        List<String> command = new ArrayList<>(10);
        command.add(containerRuntime.getExecutableName());
        command.add("run");
        command.add("-v");
        command.add(outputTarget.getOutputDirectory().toAbsolutePath().toString() + ":" + CONTAINER_IMAGE_BASE_BUILD_DIR
                + ":z");
        if (SystemUtils.IS_OS_LINUX) {
            if (containerRuntime.isDocker() && containerRuntime.isRootless()) {
                Collections.addAll(command, "--user", String.valueOf(0));
            } else {
                String uid = getLinuxID("-ur");
                String gid = getLinuxID("-gr");
                if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                    Collections.addAll(command, "--user", uid + ":" + gid);
                    if (containerRuntime.isPodman() && containerRuntime.isRootless()) {
                        // Needed to avoid AccessDeniedExceptions
                        command.add("--userns=keep-id");
                    }
                }
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
    private Path createAppCDSFromExit(JarBuildItem jarResult,
            OutputTargetBuildItem outputTarget, String javaBinPath, String containerImage,
            boolean isFastJar) {

        ArchivePathsContainer appCDSPathsContainer = ArchivePathsContainer.appCDSFromQuarkusJar(jarResult.getPath());
        Path workingDirectory = appCDSPathsContainer.workingDirectory;
        Path appCDSPath = appCDSPathsContainer.resultingFile;

        boolean debug = log.isDebugEnabled();
        List<String> javaArgs = new ArrayList<>(debug ? 4 : 3);
        javaArgs.add("-XX:ArchiveClassesAtExit=" + appCDSPath.getFileName().toString());
        javaArgs.add(String.format("-D%s=true", MainClassBuildStep.GENERATE_APP_CDS_SYSTEM_PROPERTY));
        if (debug) {
            javaArgs.add("-Xlog:cds=debug");
        }
        javaArgs.add("-jar");

        List<String> command;
        if (containerImage != null) {
            List<String> dockerRunCommand = dockerRunCommands(outputTarget, containerImage,
                    isFastJar ? CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME
                            : CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + jarResult.getPath().getFileName().toString());
            command = new ArrayList<>(dockerRunCommand.size() + 1 + javaArgs.size());
            command.addAll(dockerRunCommand);
            command.add("java");
            command.addAll(javaArgs);
            if (isFastJar) {
                command.add(JarResultBuildStep.QUARKUS_RUN_JAR);
            } else {
                command.add(jarResult.getPath().getFileName().toString());
            }
        } else {
            command = new ArrayList<>(2 + javaArgs.size());
            command.add(javaBinPath);
            command.addAll(javaArgs);
            if (isFastJar) {
                command
                        .add(jarResult.getLibraryDir().getParent().resolve(JarResultBuildStep.QUARKUS_RUN_JAR)
                                .getFileName().toString());
            } else {
                command.add(jarResult.getPath().getFileName().toString());
            }
        }

        return launchArchiveCreateCommand(workingDirectory, appCDSPath, command);
    }

    /**
     * @return The path of the created app.aot file or null if the file was not created
     */
    private Path createAot(JarBuildItem jarResult,
            OutputTargetBuildItem outputTarget, String javaBinPath, String containerImage,
            boolean isFastJar) {
        // first we run java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf -jar ...
        ArchivePathsContainer aotConfigPathContainers = ArchivePathsContainer.aotConfFromQuarkusJar(jarResult.getPath());
        Path aotConfPath = launchArchiveCreateCommand(aotConfigPathContainers.workingDirectory,
                aotConfigPathContainers.resultingFile,
                recordAotConfCommand(jarResult, outputTarget, javaBinPath, containerImage, isFastJar, aotConfigPathContainers));
        if (aotConfPath == null) {
            // something went wrong, bail as the issue has already been logged
            return null;
        }

        // now we run java -XX:AOTMode=create -XX:AOTConfiguration=app.aotconf -jar ...
        ArchivePathsContainer aotPathContainers = ArchivePathsContainer.aotFromQuarkusJar(jarResult.getPath());
        return launchArchiveCreateCommand(aotPathContainers.workingDirectory, aotPathContainers.resultingFile,
                createAotCommand(jarResult, outputTarget, javaBinPath, containerImage, isFastJar, aotConfPath));

    }

    private List<String> recordAotConfCommand(JarBuildItem jarResult, OutputTargetBuildItem outputTarget, String javaBinPath,
            String containerImage, boolean isFastJar,
            ArchivePathsContainer aotConfigPathContainers) {
        List<String> javaArgs = new ArrayList<>();
        javaArgs.add("-XX:AOTMode=record");
        javaArgs.add("-XX:AOTConfiguration=" + aotConfigPathContainers.resultingFile.getFileName().toString());
        javaArgs.add(String.format("-D%s=true", MainClassBuildStep.GENERATE_APP_CDS_SYSTEM_PROPERTY));
        javaArgs.add("-jar");

        List<String> command;
        if (containerImage != null) {
            List<String> dockerRunCommand = dockerRunCommands(outputTarget, containerImage,
                    isFastJar ? CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME
                            : CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + jarResult.getPath().getFileName().toString());
            command = new ArrayList<>(dockerRunCommand.size() + 1 + javaArgs.size());
            command.addAll(dockerRunCommand);
            command.add("java");
            command.addAll(javaArgs);
            if (isFastJar) {
                command.add(JarResultBuildStep.QUARKUS_RUN_JAR);
            } else {
                command.add(jarResult.getPath().getFileName().toString());
            }
        } else {
            command = new ArrayList<>(2 + javaArgs.size());
            command.add(javaBinPath);
            command.addAll(javaArgs);
            if (isFastJar) {
                command
                        .add(jarResult.getLibraryDir().getParent().resolve(JarResultBuildStep.QUARKUS_RUN_JAR)
                                .getFileName().toString());
            } else {
                command.add(jarResult.getPath().getFileName().toString());
            }
        }
        return command;
    }

    private List<String> createAotCommand(JarBuildItem jarResult, OutputTargetBuildItem outputTarget, String javaBinPath,
            String containerImage, boolean isFastJar,
            Path aotConfPath) {
        List<String> javaArgs = new ArrayList<>();
        javaArgs.add("-XX:AOTMode=create");
        javaArgs.add("-XX:AOTConfiguration=" + aotConfPath.getFileName().toString());
        javaArgs.add("-XX:AOTCache=app.aot");
        javaArgs.add("-jar");

        List<String> command;
        if (containerImage != null) {
            List<String> dockerRunCommand = dockerRunCommands(outputTarget, containerImage,
                    isFastJar ? CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME
                            : CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + jarResult.getPath().getFileName().toString());
            command = new ArrayList<>(dockerRunCommand.size() + 1 + javaArgs.size());
            command.addAll(dockerRunCommand);
            command.add("java");
            command.addAll(javaArgs);
            if (isFastJar) {
                command.add(JarResultBuildStep.QUARKUS_RUN_JAR);
            } else {
                command.add(jarResult.getPath().getFileName().toString());
            }
        } else {
            command = new ArrayList<>(2 + javaArgs.size());
            command.add(javaBinPath);
            command.addAll(javaArgs);
            if (isFastJar) {
                command
                        .add(jarResult.getLibraryDir().getParent().resolve(JarResultBuildStep.QUARKUS_RUN_JAR)
                                .getFileName().toString());
            } else {
                command.add(jarResult.getPath().getFileName().toString());
            }
        }
        return command;
    }

    private Path launchArchiveCreateCommand(Path workingDirectory, Path archivePath, List<String> command) {
        if (log.isDebugEnabled()) {
            log.debugf("Launching command: '%s'", String.join(" ", command));
        }

        int exitCode;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile());
            if (log.isDebugEnabled()) {
                processBuilder.inheritIO();
            } else {
                processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD);
            }
            exitCode = processBuilder.start().waitFor();
        } catch (Exception e) {
            log.debug("Failed to launch process used to create archive.", e);
            return null;
        }

        if (exitCode != 0) {
            log.debugf("The process that was supposed to create an archive exited with error code: %d.", exitCode);
            return null;
        }

        if (!archivePath.toFile().exists()) { // shouldn't happen, but let's avoid any surprises
            return null;
        }

        return archivePath;
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

            return packageConfig.jar().appcds().enabled() && packageConfig.jar().enabled();
        }
    }

    private record ArchivePathsContainer(Path workingDirectory, Path resultingFile) {

        public static ArchivePathsContainer appCDSFromQuarkusJar(Path jar) {
            return doCreate(jar, "app-cds.jsa");
        }

        public static ArchivePathsContainer aotConfFromQuarkusJar(Path jar) {
            return doCreate(jar, "app.aotconf");
        }

        public static ArchivePathsContainer aotFromQuarkusJar(Path jar) {
            return doCreate(jar, "app.aot");
        }

        private static ArchivePathsContainer doCreate(Path jar, String fileName) {
            Path workingDirectory = jar.getParent();
            Path appCDSPath = workingDirectory.resolve(fileName);
            if (appCDSPath.toFile().exists()) {
                try {
                    Files.delete(appCDSPath);
                } catch (IOException e) {
                    log.debugf(e, "Unable to delete existing '%s' file.", fileName);
                }
            }
            return new ArchivePathsContainer(workingDirectory, appCDSPath);
        }
    }

}
