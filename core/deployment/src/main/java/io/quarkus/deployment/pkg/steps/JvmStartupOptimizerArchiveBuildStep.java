package io.quarkus.deployment.pkg.steps;

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
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveContainerImageBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveRequestedBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.jar.FastJarFormat;
import io.quarkus.deployment.steps.MainClassBuildStep;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.process.ProcessBuilder;
import io.smallrye.common.process.ProcessUtil;

public class JvmStartupOptimizerArchiveBuildStep {

    private static final Logger log = Logger.getLogger(JvmStartupOptimizerArchiveBuildStep.class);

    @Deprecated(forRemoval = true, since = "3.31")
    public static final String CLASSES_LIST_FILE_NAME = "classes.lst";
    private static final String CONTAINER_IMAGE_BASE_BUILD_DIR = "/tmp/quarkus";

    @BuildStep
    public void requested(PackageConfig packageConfig,
            LaunchModeBuildItem launchMode,
            OutputTargetBuildItem outputTarget,
            CompiledJavaVersionBuildItem compiledJavaVersion,
            BuildProducer<JvmStartupOptimizerArchiveRequestedBuildItem> producer)
            throws IOException {
        JvmStartupOptimizerArchiveType type = determineType(packageConfig, compiledJavaVersion.getJavaVersion());
        if (!shouldCreate(launchMode.getLaunchMode(), packageConfig, type)) {
            return;
        }

        Path archiveDir = outputTarget.getOutputDirectory().resolve("jvmstartuparchive");
        IoUtils.createOrEmptyDir(archiveDir);

        producer.produce(
                new JvmStartupOptimizerArchiveRequestedBuildItem(outputTarget.getOutputDirectory().resolve("jvmstartuparchive"),
                        type));
    }

    public boolean shouldCreate(LaunchMode launchMode, PackageConfig packageConfig, JvmStartupOptimizerArchiveType type) {
        if (launchMode != LaunchMode.NORMAL) {
            return false;
        }

        PackageConfig.JarConfig jarConfig = packageConfig.jar();
        if (!jarConfig.enabled()) {
            return false;
        }

        // new config
        if (jarConfig.aot().enabled()) {
            var maybePhase = jarConfig.aot().phase();
            if (maybePhase.isPresent()) {
                var phase = maybePhase.get();
                if ((type == JvmStartupOptimizerArchiveType.AppCDS)
                        && (phase == PackageConfig.JarConfig.AotConfig.AotPhase.INTEGRATION_TESTS)) {
                    log.warn("Building AppCDS file from integration tests is not supported");
                    return false;
                } else if (phase == PackageConfig.JarConfig.AotConfig.AotPhase.BUILD) {
                    // when the phase was explicitly set to build, we build no matter what the archive type
                    return true;
                } else if (phase == PackageConfig.JarConfig.AotConfig.AotPhase.AUTO) {
                    // when the phase is auto, then we default to creating the file only for AppCDS
                    return type == JvmStartupOptimizerArchiveType.AppCDS;
                }
            } else {
                // when the phase is not set, then we default to creating the file only for AppCDS
                return type == JvmStartupOptimizerArchiveType.AppCDS;
            }
        }

        // old config
        //noinspection removal
        return jarConfig.appcds().enabled();
    }

    private JvmStartupOptimizerArchiveType determineType(PackageConfig packageConfig,
            CompiledJavaVersionBuildItem.JavaVersion javaVersion) {
        PackageConfig.JarConfig jarConfig = packageConfig.jar();
        // first check new config
        PackageConfig.JarConfig.AotConfig aotConfig = jarConfig.aot();
        if (aotConfig.enabled()) {
            Optional<PackageConfig.JarConfig.AotConfig.AotType> typeOpt = aotConfig.type();
            if (typeOpt.isPresent()) {
                return switch (typeOpt.get()) {
                    case AOT -> JvmStartupOptimizerArchiveType.AOT;
                    case AppCDS -> JvmStartupOptimizerArchiveType.AppCDS;
                    case AUTO -> determineTypeFromJavaVersion(javaVersion);
                };
            }
            return determineTypeFromJavaVersion(javaVersion);
        }
        // now check the old config
        PackageConfig.JarConfig.AppcdsConfig appcdsConfig = jarConfig.appcds();
        return appcdsConfig.useAot() ? JvmStartupOptimizerArchiveType.AOT
                : JvmStartupOptimizerArchiveType.AppCDS;
    }

    private JvmStartupOptimizerArchiveType determineTypeFromJavaVersion(
            CompiledJavaVersionBuildItem.JavaVersion javaVersion) {
        if (javaVersion.isJava25OrHigher() == CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE) {
            log.debugf("Selecting %s as the startup file optimizer type since the project is targeting JDK 25+",
                    JvmStartupOptimizerArchiveType.AOT);
            return JvmStartupOptimizerArchiveType.AOT;
        }
        log.debugf("Selecting %s as the startup file optimizer type since the project is not targeting JDK 25+",
                JvmStartupOptimizerArchiveType.AppCDS);
        return JvmStartupOptimizerArchiveType.AppCDS;
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
                    + ProcessUtil.nameOfJava();
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
        boolean isFastJar = packageConfig.jar().type().usesFastJarLayout();
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

        jvmStartupOptimizerArchive.produce(new JvmStartupOptimizerArchiveResultBuildItem(archivePath, archiveType));
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
                    isFastJar ? CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + FastJarFormat.DEFAULT_FAST_JAR_DIRECTORY_NAME
                            : CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + jarResult.getPath().getFileName().toString());
            command = new ArrayList<>(dockerRunCommand.size() + 1 + javaArgs.size());
            command.addAll(dockerRunCommand);
            command.add("java");
            command.addAll(javaArgs);
            if (isFastJar) {
                command.add(FastJarFormat.QUARKUS_RUN_JAR);
            } else {
                command.add(jarResult.getPath().getFileName().toString());
            }
        } else {
            command = new ArrayList<>(2 + javaArgs.size());
            command.add(javaBinPath);
            command.addAll(javaArgs);
            if (isFastJar) {
                command
                        .add(jarResult.getLibraryDir().getParent().resolve(FastJarFormat.QUARKUS_RUN_JAR)
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
        if (Runtime.version().feature() < 25) {
            throw new IllegalStateException(
                    "AOT cache generation requires building with JDK 25 or newer (see JEP 514). ");
        }
        ArchivePathsContainer aotPathContainers = ArchivePathsContainer.aotFromQuarkusJar(jarResult.getPath());
        return launchArchiveCreateCommand(aotPathContainers.workingDirectory, aotPathContainers.resultingFile,
                createAotCommand(jarResult, outputTarget, javaBinPath, containerImage, isFastJar, aotPathContainers));

    }

    private List<String> createAotCommand(JarBuildItem jarResult, OutputTargetBuildItem outputTarget, String javaBinPath,
            String containerImage, boolean isFastJar,
            ArchivePathsContainer aotPathContainers) {
        List<String> javaArgs = new ArrayList<>();
        javaArgs.add("-XX:AOTCacheOutput=" + aotPathContainers.resultingFile.getFileName().toString());
        javaArgs.add(String.format("-D%s=true", MainClassBuildStep.GENERATE_APP_CDS_SYSTEM_PROPERTY));
        javaArgs.add("-jar");

        List<String> command;
        if (containerImage != null) {
            List<String> dockerRunCommand = dockerRunCommands(outputTarget, containerImage,
                    isFastJar ? CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + FastJarFormat.DEFAULT_FAST_JAR_DIRECTORY_NAME
                            : CONTAINER_IMAGE_BASE_BUILD_DIR + "/" + jarResult.getPath().getFileName().toString());
            command = new ArrayList<>(dockerRunCommand.size() + 1 + javaArgs.size());
            command.addAll(dockerRunCommand);
            command.add("java");
            command.addAll(javaArgs);
            if (isFastJar) {
                command.add(FastJarFormat.QUARKUS_RUN_JAR);
            } else {
                command.add(jarResult.getPath().getFileName().toString());
            }
        } else {
            command = new ArrayList<>(2 + javaArgs.size());
            command.add(javaBinPath);
            command.addAll(javaArgs);
            if (isFastJar) {
                command
                        .add(jarResult.getLibraryDir().getParent().resolve(FastJarFormat.QUARKUS_RUN_JAR)
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

        try {
            var pb = ProcessBuilder.newBuilder(command.get(0))
                    .arguments(command.subList(1, command.size()))
                    .directory(workingDirectory);
            pb.error().logOnSuccess(false);
            if (log.isDebugEnabled()) {
                pb.output().consumeLinesWith(8192, log::debug)
                        .error().consumeLinesWith(8192, log::debug);
            }
            pb.run();
        } catch (Exception e) {
            log.debug("Failed to launch process used to create archive", e);
        }
        if (!archivePath.toFile().exists()) { // shouldn't happen, but let's avoid any surprises
            return null;
        }

        return archivePath;
    }

    static class AotFileRequired implements BooleanSupplier {

        private final PackageConfig packageConfig;
        private final LaunchMode launchMode;

        AotFileRequired(PackageConfig packageConfig, LaunchMode launchMode) {
            this.packageConfig = packageConfig;
            this.launchMode = launchMode;
        }

        @Override
        public boolean getAsBoolean() {
            if (launchMode != LaunchMode.NORMAL) {
                return false;
            }

            PackageConfig.JarConfig jarConfig = packageConfig.jar();
            if (!jarConfig.enabled()) {
                return false;
            }

            // new config
            if (jarConfig.aot().enabled()) {
                // Only generate during build phase if phase is explicitly set to BUILD.
                // When phase is not set or set to AUTO/INTEGRATION_TESTS, the AOT file
                // will be generated during integration tests instead.
                Optional<PackageConfig.JarConfig.AotConfig.AotPhase> phase = jarConfig.aot().phase();
                return phase.isPresent() && phase.get() == PackageConfig.JarConfig.AotConfig.AotPhase.BUILD;
            }

            // old config
            //noinspection removal
            return jarConfig.appcds().enabled();
        }
    }

    private record ArchivePathsContainer(Path workingDirectory, Path resultingFile) {

        public static ArchivePathsContainer appCDSFromQuarkusJar(Path jar) {
            return doCreate(jar, "app-cds.jsa");
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
