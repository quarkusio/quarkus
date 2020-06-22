package io.quarkus.deployment.pkg.steps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSRequestedBuildItem;
import io.quarkus.deployment.pkg.builditem.AppCDSResultBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.steps.MainClassBuildStep;
import io.quarkus.deployment.util.JavaVersionUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.utilities.JavaBinFinder;

public class AppCDSBuildStep {

    private static final Logger log = Logger.getLogger(AppCDSBuildStep.class);
    public static final String JDK_CLASSLIST_FILE = "classlist";
    public static final String CLASSES_LIST_FILE_NAME = "classes.lst";

    @BuildStep(onlyIf = AppCDSRequired.class)
    public void requested(OutputTargetBuildItem outputTarget, BuildProducer<AppCDSRequestedBuildItem> producer)
            throws IOException {
        Path appCDSDir = outputTarget.getOutputDirectory().resolve("appcds");
        IoUtils.recursiveDelete(appCDSDir);
        Files.createDirectories(appCDSDir);

        producer.produce(new AppCDSRequestedBuildItem(outputTarget.getOutputDirectory().resolve("appcds")));
    }

    @BuildStep
    public void build(Optional<AppCDSRequestedBuildItem> appCDsRequested,
            JarBuildItem jarResult, PackageConfig packageConfig,
            BuildProducer<AppCDSResultBuildItem> appCDS,
            BuildProducer<ArtifactResultBuildItem> artifactResult) throws Exception {
        if (!appCDsRequested.isPresent()) {
            return;
        }

        Path appCDSDir = appCDsRequested.get().getAppCDSDir();
        String javaHomeStr = System.getProperty("java.home");
        Path javaHomeDir = Paths.get(javaHomeStr);
        Path jdkClassList = javaHomeDir.resolve("lib").resolve(JDK_CLASSLIST_FILE);
        if (!jdkClassList.toFile().exists()) {
            log.warnf(
                    "In order to create AppCDS the JDK used to build the Quarkus application must contain a file named '%s' in the its 'lib' directory.",
                    JDK_CLASSLIST_FILE);
            return;
        }
        String javaExecutableStr = JavaBinFinder.simpleBinaryName();
        String javaBinStr = javaHomeStr + File.separator + "bin" + File.separator + javaExecutableStr;
        if (!new File(javaBinStr).canExecute()) {
            log.warnf(
                    "In order to create AppCDS the JDK used to build the Quarkus application must contain an executable named '%s' in its 'bin' directory.",
                    javaBinStr);
            return;
        }

        Path classesLstPath = createClassesLst(packageConfig, jarResult, javaBinStr, appCDSDir);
        if (classesLstPath == null) {
            log.warnf("Unable to create AppCDS because '%s' was not created.", CLASSES_LIST_FILE_NAME);
            return;
        }

        log.debugf("'%s' successfully created.", CLASSES_LIST_FILE_NAME);

        log.info("Launching AppCDS creation process.");
        Path appCDSPath = createAppCDS(jarResult, javaBinStr, classesLstPath, packageConfig.isFastJar());
        if (appCDSPath == null) {
            log.warn("Unable to create AppCDS.");
            return;
        }

        log.infof(
                "AppCDS successfully created at: '%s'.\nTo ensure they are loaded properly, " +
                        "run the application jar from its directory and also add the '-XX:SharedArchiveFile=app-cds.jsa' " +
                        "JVM flag.\nMoreover, make sure to use the exact same Java version (%s) to run the application as was used to build it.",
                appCDSPath.toAbsolutePath().toString(), System.getProperty("java.version"));

        appCDS.produce(new AppCDSResultBuildItem(appCDSPath));
        artifactResult.produce(new ArtifactResultBuildItem(appCDSPath, "appCDS", Collections.emptyMap()));
    }

    /**
     * @return The path of the created classes.lst file or null if the file was not created
     */
    private Path createClassesLst(PackageConfig packageConfig, JarBuildItem jarResult,
            String javaBin, Path appCDSDir) {

        List<String> command = new ArrayList<>(5);
        command.add(javaBin);
        command.add("-XX:DumpLoadedClassList=" + CLASSES_LIST_FILE_NAME);
        command.add(String.format("-D%s=true", MainClassBuildStep.GENERATE_APP_CDS_SYSTEM_PROPERTY));
        command.add("-jar");
        if (packageConfig.isFastJar()) {
            command.add(jarResult.getLibraryDir().getParent().resolve(JarResultBuildStep.QUARKUS_RUN_JAR).toAbsolutePath()
                    .toString());
        } else {
            command.add(jarResult.getPath().toAbsolutePath().toString());
        }

        if (log.isDebugEnabled()) {
            log.debugf("Launching command: '%s' to create '" + CLASSES_LIST_FILE_NAME + "' AppCDS.", String.join(" ", command));
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

        return appCDSDir.resolve(CLASSES_LIST_FILE_NAME);
    }

    /**
     * @return The path of the created app-cds.jsa file or null if the file was not created
     */
    private Path createAppCDS(JarBuildItem jarResult, String javaBin,
            Path classesLstPath, boolean isFastFar) {

        Path workingDirectory = isFastFar ? jarResult.getPath().getParent().getParent() : jarResult.getPath().getParent();
        Path appCDSPath = workingDirectory.resolve("app-cds.jsa");
        if (appCDSPath.toFile().exists()) {
            try {
                Files.delete(appCDSPath);
            } catch (IOException e) {
                log.debug("Unable to delete existing 'app-cds.jsa' file.", e);
            }
        }

        List<String> command = new ArrayList<>(6);
        command.add(javaBin);
        command.add("-Xshare:dump");
        command.add("-XX:SharedClassListFile=" + classesLstPath.toAbsolutePath().toString());
        // We use the relative paths because at runtime 'java -XX:SharedArchiveFile=... -jar ...' expects the AppCDS and jar files
        // to match exactly what was used at build time.
        // For that reason we also run the creation process from inside the output directory,
        // The end result is that users can simply use 'java -XX:SharedArchiveFile=app-cds.jsa -jar app.jar'
        command.add("-XX:SharedArchiveFile=" + appCDSPath.getFileName().toString());
        command.add("--class-path");
        if (isFastFar) {
            command.add(JarResultBuildStep.QUARKUS_RUN_JAR);
        } else {
            command.add(jarResult.getPath().getFileName().toString());
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

            if (!JavaVersionUtil.isJava11OrHigher()) {
                log.warn("AppCDS can only be used with Java 11+.");
                return false;
            }
            return true;
        }
    }

    // copied from Java 9
    // TODO remove when we move to Java 11

    private static final File NULL_FILE = new File(
            (System.getProperty("os.name")
                    .startsWith("Windows") ? "NUL" : "/dev/null"));
}
