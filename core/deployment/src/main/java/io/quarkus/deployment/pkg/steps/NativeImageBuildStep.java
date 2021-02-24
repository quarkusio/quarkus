package io.quarkus.deployment.pkg.steps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled;

public class NativeImageBuildStep {

    private static final Logger log = Logger.getLogger(NativeImageBuildStep.class);
    public static final String DEBUG_BUILD_PROCESS_PORT = "5005";

    /**
     * Name of the <em>system</em> property to retrieve JAVA_HOME
     */
    private static final String JAVA_HOME_SYS = "java.home";

    /**
     * Name of the <em>environment</em> variable to retrieve JAVA_HOME
     */
    private static final String JAVA_HOME_ENV = "JAVA_HOME";

    /**
     * The name of the environment variable containing the system path.
     */
    private static final String PATH = "PATH";

    private static final int OOM_ERROR_VALUE = 137;
    private static final String QUARKUS_XMX_PROPERTY = "quarkus.native.native-image-xmx";
    public static final String CONTAINER_BUILD_VOLUME_PATH = "/project";
    private static final String TRUST_STORE_SYSTEM_PROPERTY_MARKER = "-Djavax.net.ssl.trustStore=";
    private static final String MOVED_TRUST_STORE_NAME = "trustStore";
    public static final String APP_SOURCES = "app-sources";

    @BuildStep(onlyIf = NativeBuild.class)
    ArtifactResultBuildItem result(NativeImageBuildItem image) {
        return new ArtifactResultBuildItem(image.getPath(), PackageConfig.NATIVE, Collections.emptyMap());
    }

    @BuildStep(onlyIf = NativeSourcesBuild.class)
    ArtifactResultBuildItem nativeSourcesResult(NativeConfig nativeConfig,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            PackageConfig packageConfig,
            List<NativeImageSystemPropertyBuildItem> nativeImageProperties) {

        Path outputDir;
        try {
            outputDir = buildSystemTargetBuildItem.getOutputDirectory().resolve("native-sources");
            IoUtils.createOrEmptyDir(outputDir);
            IoUtils.copy(nativeImageSourceJarBuildItem.getPath().getParent(), outputDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create native-sources output directory", e);
        }

        Path runnerJar = outputDir.resolve(nativeImageSourceJarBuildItem.getPath().getFileName());

        String nativeImageName = getResultingBinaryName(outputTargetBuildItem, packageConfig, isContainerBuild(nativeConfig));

        NativeImageInvokerInfo nativeImageArgs = new NativeImageInvokerInfo.Builder()
                .setNativeConfig(nativeConfig)
                .setOutputTargetBuildItem(outputTargetBuildItem)
                .setNativeImageProperties(nativeImageProperties)
                .setOutputDir(outputDir)
                .setRunnerJarName(runnerJar.getFileName().toString())
                // the path to native-image is not known now, it is only known at the time the native-sources will be consumed
                .setResultingBinaryName(nativeImageName)
                .setContainerBuild(nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild)
                .build();
        List<String> command = nativeImageArgs.getArgs();
        try (FileOutputStream commandFOS = new FileOutputStream(outputDir.resolve("native-image.args").toFile())) {
            String commandStr = String.join(" ", command);
            commandFOS.write(commandStr.getBytes(StandardCharsets.UTF_8));

            log.info("The sources for a subsequent native-image run along with the necessary arguments can be found in "
                    + outputDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build native image sources", e);
        }

        // drop the original output to avoid confusion
        IoUtils.recursiveDelete(nativeImageSourceJarBuildItem.getPath().getParent());

        return new ArtifactResultBuildItem(nativeImageSourceJarBuildItem.getPath(), PackageConfig.NATIVE_SOURCES,
                Collections.emptyMap());
    }

    @BuildStep
    public NativeImageBuildItem build(NativeConfig nativeConfig, NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            PackageConfig packageConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<NativeImageSystemPropertyBuildItem> nativeImageProperties,
            Optional<ProcessInheritIODisabled> processInheritIODisabled) {
        if (nativeConfig.debug.enabled) {
            copyJarSourcesToLib(outputTargetBuildItem, curateOutcomeBuildItem);
            copySourcesToSourceCache(outputTargetBuildItem);
        }

        Path runnerJar = nativeImageSourceJarBuildItem.getPath();
        log.info("Building native image from " + runnerJar);
        Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();

        final String runnerJarName = runnerJar.getFileName().toString();

        String noPIE = "";

        boolean isContainerBuild = isContainerBuild(nativeConfig);
        if (!isContainerBuild && SystemUtils.IS_OS_LINUX) {
            noPIE = detectNoPIE();
        }

        String nativeImageName = getResultingBinaryName(outputTargetBuildItem, packageConfig, isContainerBuild);

        NativeImageBuildRunner buildRunner = getNativeImageBuildRunner(nativeConfig, outputDir, nativeImageName);
        buildRunner.setup(processInheritIODisabled.isPresent());
        final GraalVM.Version graalVMVersion = buildRunner.getGraalVMVersion();

        if (graalVMVersion.isDetected()) {
            checkGraalVMVersion(graalVMVersion);
        } else {
            log.error("Unable to get GraalVM version from the native-image binary.");
        }

        try {
            if (nativeConfig.cleanupServer && !graalVMVersion.isMandrel()) {
                buildRunner.cleanupServer(outputDir.toFile());
            }

            NativeImageInvokerInfo commandAndExecutable = new NativeImageInvokerInfo.Builder()
                    .setNativeConfig(nativeConfig)
                    .setOutputTargetBuildItem(outputTargetBuildItem)
                    .setNativeImageProperties(nativeImageProperties)
                    .setOutputDir(outputDir)
                    .setRunnerJarName(runnerJarName)
                    .setResultingBinaryName(nativeImageName)
                    .setNoPIE(noPIE)
                    .setContainerBuild(isContainerBuild)
                    .setGraalVMVersion(graalVMVersion)
                    .build();

            List<String> nativeImageArgs = commandAndExecutable.args;

            int exitCode = buildRunner.build(nativeImageArgs, outputDir, processInheritIODisabled.isPresent());
            if (exitCode != 0) {
                throw imageGenerationFailed(exitCode, nativeImageArgs);
            }
            Path generatedImage = outputDir.resolve(nativeImageName);
            Path finalPath = outputTargetBuildItem.getOutputDirectory().resolve(nativeImageName);
            IoUtils.copy(generatedImage, finalPath);
            Files.delete(generatedImage);
            if (nativeConfig.debug.enabled) {
                final String sources = "sources";
                final Path generatedSources = outputDir.resolve(sources);
                final Path finalSources = outputTargetBuildItem.getOutputDirectory().resolve(sources);
                IoUtils.copy(generatedSources, finalSources);
                IoUtils.recursiveDelete(generatedSources);
            }
            System.setProperty("native.image.path", finalPath.toAbsolutePath().toString());

            if (objcopyExists()) {
                if (nativeConfig.debug.enabled) {
                    splitDebugSymbols(finalPath);
                }
                // Strip debug symbols regardless, because the underlying JDK might contain them
                objcopy("--strip-debug", finalPath.toString());
            } else {
                log.warn("objcopy executable not found in PATH. Debug symbols will not be separated from executable.");
                log.warn("That will result in a larger native image with debug symbols embedded in it.");
            }

            return new NativeImageBuildItem(finalPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build native image", e);
        } finally {
            if (nativeConfig.debug.enabled) {
                removeJarSourcesFromLib(outputTargetBuildItem);
                IoUtils.recursiveDelete(outputDir.resolve(Paths.get(APP_SOURCES)));
            }
        }
    }

    private String getResultingBinaryName(OutputTargetBuildItem outputTargetBuildItem, PackageConfig packageConfig,
            boolean isContainerBuild) {
        String nativeImageName = outputTargetBuildItem.getBaseName() + packageConfig.runnerSuffix;
        if (SystemUtils.IS_OS_WINDOWS && !isContainerBuild) {
            //once image is generated it gets added .exe on Windows
            nativeImageName = nativeImageName + ".exe";
        }
        return nativeImageName;
    }

    public static boolean isContainerBuild(NativeConfig nativeConfig) {
        return nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild || nativeConfig.remoteContainerBuild;
    }

    private static NativeImageBuildRunner getNativeImageBuildRunner(NativeConfig nativeConfig, Path outputDir,
            String nativeImageName) {
        if (!isContainerBuild(nativeConfig)) {
            NativeImageBuildLocalRunner localRunner = getNativeImageBuildLocalRunner(nativeConfig);
            if (localRunner != null) {
                return localRunner;
            }
            String executableName = getNativeImageExecutableName();
            String errorMessage = "Cannot find the `" + executableName
                    + "` in the GRAALVM_HOME, JAVA_HOME and System PATH. Install it using `gu install native-image`";
            if (!SystemUtils.IS_OS_LINUX) {
                throw new RuntimeException(errorMessage);
            }
            log.warn(errorMessage + " Attempting to fall back to container build.");
        }
        if (nativeConfig.remoteContainerBuild) {
            return new NativeImageBuildRemoteContainerRunner(nativeConfig, outputDir, nativeImageName);
        }
        return new NativeImageBuildLocalContainerRunner(nativeConfig, outputDir);
    }

    private void copyJarSourcesToLib(OutputTargetBuildItem outputTargetBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        Path targetDirectory = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + "-native-image-source-jar");
        Path libDir = targetDirectory.resolve(JarResultBuildStep.LIB);
        File libDirFile = libDir.toFile();
        if (!libDirFile.exists()) {
            libDirFile.mkdirs();
        }

        final List<AppDependency> appDeps = curateOutcomeBuildItem.getEffectiveModel().getUserDependencies();
        for (AppDependency appDep : appDeps) {
            final AppArtifact depArtifact = appDep.getArtifact();
            if (depArtifact.getType().equals("jar")) {
                for (Path resolvedDep : depArtifact.getPaths()) {
                    if (!Files.isDirectory(resolvedDep)) {
                        // Do we need to handle transformed classes?
                        // Their bytecode might have been modified but is there source for such modification?
                        final Path jarSourceDep = toJarSource(resolvedDep);
                        if (jarSourceDep.toFile().exists()) {
                            final String fileName = depArtifact.getGroupId() + "." + jarSourceDep.getFileName();
                            final Path targetPath = libDir.resolve(fileName);
                            try {
                                Files.copy(jarSourceDep, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                throw new RuntimeException("Unable to copy from " + jarSourceDep + " to " + targetPath, e);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Path toJarSource(Path path) {
        final Path parent = path.getParent();
        final String fileName = path.getFileName().toString();
        final int extensionIndex = fileName.lastIndexOf('.');
        final String sourcesFileName = String.format("%s-sources.jar", fileName.substring(0, extensionIndex));
        return parent.resolve(sourcesFileName);
    }

    private void removeJarSourcesFromLib(OutputTargetBuildItem outputTargetBuildItem) {
        Path targetDirectory = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + "-native-image-source-jar");
        Path libDir = targetDirectory.resolve(JarResultBuildStep.LIB);

        final File[] jarSources = libDir.toFile()
                .listFiles((file, name) -> name.endsWith("-sources.jar"));
        Stream.of(Objects.requireNonNull(jarSources)).forEach(File::delete);
    }

    private static void copySourcesToSourceCache(OutputTargetBuildItem outputTargetBuildItem) {
        Path targetDirectory = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + "-native-image-source-jar");

        final Path targetSrc = targetDirectory.resolve(Paths.get(APP_SOURCES));
        final File targetSrcFile = targetSrc.toFile();
        if (!targetSrcFile.exists()) {
            targetSrcFile.mkdirs();
        }

        final Path javaSourcesPath = outputTargetBuildItem.getOutputDirectory().resolve(
                Paths.get("..", "src", "main", "java"));

        if (Files.exists(javaSourcesPath)) {
            try (Stream<Path> paths = Files.walk(javaSourcesPath)) {
                paths.forEach(path -> {
                    Path targetPath = Paths.get(targetSrc.toString(),
                            path.toString().substring(javaSourcesPath.toString().length()));
                    try {
                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Unable to copy from " + path + " to " + targetPath, e);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to walk path " + javaSourcesPath, e);
            }
        }
    }

    private RuntimeException imageGenerationFailed(int exitValue, List<String> command) {
        if (exitValue == OOM_ERROR_VALUE) {
            if (command.contains("docker") && !SystemUtils.IS_OS_LINUX) {
                return new RuntimeException("Image generation failed. Exit code was " + exitValue
                        + " which indicates an out of memory error. The most likely cause is Docker not being given enough memory. Also consider increasing the Xmx value for native image generation by setting the \""
                        + QUARKUS_XMX_PROPERTY + "\" property");
            } else {
                return new RuntimeException("Image generation failed. Exit code was " + exitValue
                        + " which indicates an out of memory error. Consider increasing the Xmx value for native image generation by setting the \""
                        + QUARKUS_XMX_PROPERTY + "\" property");
            }
        } else {
            return new RuntimeException("Image generation failed. Exit code: " + exitValue);
        }
    }

    private void checkGraalVMVersion(GraalVM.Version version) {
        log.info("Running Quarkus native-image plugin on " + version.getFullVersion());
        if (version.isObsolete()) {
            final int major = GraalVM.Version.CURRENT.major;
            final int minor = GraalVM.Version.CURRENT.minor;
            throw new IllegalStateException("Out of date version of GraalVM detected: " + version.getFullVersion() + "."
                    + " Quarkus currently supports " + major + "." + minor + ". Please upgrade GraalVM to this version.");
        }
    }

    private static NativeImageBuildLocalRunner getNativeImageBuildLocalRunner(NativeConfig nativeConfig) {
        String executableName = getNativeImageExecutableName();
        if (nativeConfig.graalvmHome.isPresent()) {
            File file = Paths.get(nativeConfig.graalvmHome.get(), "bin", executableName).toFile();
            if (file.exists()) {
                return new NativeImageBuildLocalRunner(file.getAbsolutePath());
            }
        }

        File javaHome = nativeConfig.javaHome;
        if (javaHome == null) {
            // try system property first - it will be the JAVA_HOME used by the current JVM
            String home = System.getProperty(JAVA_HOME_SYS);
            if (home == null) {
                // No luck, somewhat a odd JVM not enforcing this property
                // try with the JAVA_HOME environment variable
                home = System.getenv(JAVA_HOME_ENV);
            }

            if (home != null) {
                javaHome = new File(home);
            }
        }

        if (javaHome != null) {
            File file = new File(javaHome, "bin/" + executableName);
            if (file.exists()) {
                return new NativeImageBuildLocalRunner(file.getAbsolutePath());
            }
        }

        // System path
        String systemPath = System.getenv(PATH);
        if (systemPath != null) {
            String[] pathDirs = systemPath.split(File.pathSeparator);
            for (String pathDir : pathDirs) {
                File dir = new File(pathDir);
                if (dir.isDirectory()) {
                    File file = new File(dir, executableName);
                    if (file.exists()) {
                        return new NativeImageBuildLocalRunner(file.getAbsolutePath());
                    }
                }
            }
        }

        return null;
    }

    private static String getNativeImageExecutableName() {
        return SystemUtils.IS_OS_WINDOWS ? "native-image.cmd" : "native-image";
    }

    private static String detectNoPIE() {
        String argument = testGCCArgument("-no-pie");

        return argument.length() == 0 ? testGCCArgument("-nopie") : argument;
    }

    private static String testGCCArgument(String argument) {
        try {
            Process gcc = new ProcessBuilder("cc", "-v", "-E", argument, "-").start();
            gcc.getOutputStream().close();
            if (gcc.waitFor() == 0) {
                return argument;
            }

        } catch (IOException | InterruptedException e) {
            // eat
        }

        return "";
    }

    private boolean objcopyExists() {
        // System path
        String systemPath = System.getenv(PATH);
        if (systemPath != null) {
            String[] pathDirs = systemPath.split(File.pathSeparator);
            for (String pathDir : pathDirs) {
                File dir = new File(pathDir);
                if (dir.isDirectory()) {
                    File file = new File(dir, "objcopy");
                    if (file.exists()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void splitDebugSymbols(Path executable) {
        Path symbols = Paths.get(String.format("%s.debug", executable.toString()));
        objcopy("--only-keep-debug", executable.toString(), symbols.toString());
        objcopy(String.format("--add-gnu-debuglink=%s", symbols.toString()), executable.toString());
    }

    private static void objcopy(String... args) {
        final List<String> command = new ArrayList<>(args.length + 1);
        command.add("objcopy");
        command.addAll(Arrays.asList(args));
        if (log.isDebugEnabled()) {
            log.debugf("Execute %s", String.join(" ", command));
        }
        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Unable to invoke objcopy", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    protected static final class GraalVM {
        static final class Version implements Comparable<Version> {
            private static final Pattern PATTERN = Pattern.compile(
                    "GraalVM Version (([1-9][0-9]*)\\.([0-9]+)\\.[0-9]+|\\p{XDigit}*)[^(\n$]*(\\(Mandrel Distribution\\))?\\s*");

            static final Version UNVERSIONED = new Version("Undefined", -1, -1, Distribution.ORACLE);
            static final Version SNAPSHOT_ORACLE = new Version("Snapshot", Integer.MAX_VALUE, Integer.MAX_VALUE,
                    Distribution.ORACLE);
            static final Version SNAPSHOT_MANDREL = new Version("Snapshot", Integer.MAX_VALUE, Integer.MAX_VALUE,
                    Distribution.MANDREL);

            static final Version VERSION_20_3 = new Version("GraalVM 20.3", 20, 3, Distribution.ORACLE);
            static final Version VERSION_21_0 = new Version("GraalVM 21.0", 21, 0, Distribution.ORACLE);

            static final Version MINIMUM = VERSION_20_3;
            static final Version CURRENT = VERSION_21_0;

            final String fullVersion;
            final int major;
            final int minor;
            final Distribution distribution;

            Version(String fullVersion, int major, int minor, Distribution distro) {
                this.fullVersion = fullVersion;
                this.major = major;
                this.minor = minor;
                this.distribution = distro;
            }

            String getFullVersion() {
                return fullVersion;
            }

            boolean isDetected() {
                return this != UNVERSIONED;
            }

            boolean isObsolete() {
                return this.compareTo(MINIMUM) < 0;
            }

            boolean isMandrel() {
                return distribution == Distribution.MANDREL;
            }

            boolean isSnapshot() {
                return this == SNAPSHOT_ORACLE || this == SNAPSHOT_MANDREL;
            }

            boolean isNewerThan(Version version) {
                return this.compareTo(version) > 0;
            }

            @Override
            public int compareTo(Version o) {
                if (major > o.major) {
                    return 1;
                }

                if (major == o.major) {
                    if (minor > o.minor) {
                        return 1;
                    } else if (minor == o.minor) {
                        return 0;
                    }
                }

                return -1;
            }

            static Version of(Stream<String> lines) {
                final Iterator<String> it = lines.iterator();
                while (it.hasNext()) {
                    final String line = it.next();
                    final Matcher matcher = PATTERN.matcher(line);
                    if (matcher.find() && matcher.groupCount() >= 3) {
                        final String distro = matcher.group(4);
                        if (isSnapshot(matcher.group(2))) {
                            return isMandrel(distro) ? SNAPSHOT_MANDREL : SNAPSHOT_ORACLE;
                        } else {
                            return new Version(
                                    line,
                                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                                    isMandrel(distro) ? Distribution.MANDREL : Distribution.ORACLE);
                        }
                    }
                }

                return UNVERSIONED;
            }

            private static boolean isSnapshot(String s) {
                return s == null;
            }

            private static boolean isMandrel(String s) {
                return "(Mandrel Distribution)".equals(s);
            }

            @Override
            public String toString() {
                return "Version{" +
                        "major=" + major +
                        ", minor=" + minor +
                        ", distribution=" + distribution +
                        '}';
            }
        }

        enum Distribution {
            ORACLE,
            MANDREL;
        }
    }

    private static class NativeImageInvokerInfo {
        private final List<String> args;

        private NativeImageInvokerInfo(List<String> args) {
            this.args = args;
        }

        List<String> getArgs() {
            return args;
        }

        static class Builder {
            private NativeConfig nativeConfig;
            private OutputTargetBuildItem outputTargetBuildItem;
            private List<NativeImageSystemPropertyBuildItem> nativeImageProperties;
            private Path outputDir;
            private String runnerJarName;
            private String noPIE = "";
            private boolean isContainerBuild = false;
            private GraalVM.Version graalVMVersion = GraalVM.Version.UNVERSIONED;
            private String resultingBinaryName;

            public Builder setNativeConfig(NativeConfig nativeConfig) {
                this.nativeConfig = nativeConfig;
                return this;
            }

            public Builder setOutputTargetBuildItem(OutputTargetBuildItem outputTargetBuildItem) {
                this.outputTargetBuildItem = outputTargetBuildItem;
                return this;
            }

            public Builder setNativeImageProperties(List<NativeImageSystemPropertyBuildItem> nativeImageProperties) {
                this.nativeImageProperties = nativeImageProperties;
                return this;
            }

            public Builder setOutputDir(Path outputDir) {
                this.outputDir = outputDir;
                return this;
            }

            public Builder setRunnerJarName(String runnerJarName) {
                this.runnerJarName = runnerJarName;
                return this;
            }

            public Builder setNoPIE(String noPIE) {
                this.noPIE = noPIE;
                return this;
            }

            public Builder setContainerBuild(boolean containerBuild) {
                isContainerBuild = containerBuild;
                return this;
            }

            public Builder setGraalVMVersion(GraalVM.Version graalVMVersion) {
                this.graalVMVersion = graalVMVersion;
                return this;
            }

            public Builder setResultingBinaryName(String resultingBinaryName) {
                this.resultingBinaryName = resultingBinaryName;
                return this;
            }

            public NativeImageInvokerInfo build() {
                List<String> nativeImageArgs = new ArrayList<>();
                boolean enableSslNative = false;
                boolean enableAllSecurityServices = nativeConfig.enableAllSecurityServices;
                boolean addAllCharsets = nativeConfig.addAllCharsets;
                boolean enableHttpsUrlHandler = nativeConfig.enableHttpsUrlHandler;
                for (NativeImageSystemPropertyBuildItem prop : nativeImageProperties) {
                    //todo: this should be specific build items
                    if (prop.getKey().equals("quarkus.ssl.native") && prop.getValue() != null) {
                        enableSslNative = Boolean.parseBoolean(prop.getValue());
                    } else if (prop.getKey().equals("quarkus.jni.enable") && prop.getValue().equals("false")) {
                        log.warn("Your application is setting the deprecated 'quarkus.jni.enable' configuration key to false."
                                + " Please consider removing this configuration key as it is ignored (JNI is always enabled) and it"
                                + " will be removed in a future Quarkus version.");
                    } else if (prop.getKey().equals("quarkus.native.enable-all-security-services") && prop.getValue() != null) {
                        enableAllSecurityServices |= Boolean.parseBoolean(prop.getValue());
                    } else if (prop.getKey().equals("quarkus.native.enable-all-charsets") && prop.getValue() != null) {
                        addAllCharsets |= Boolean.parseBoolean(prop.getValue());
                    } else {
                        // todo maybe just -D is better than -J-D in this case
                        if (prop.getValue() == null) {
                            nativeImageArgs.add("-J-D" + prop.getKey());
                        } else {
                            nativeImageArgs.add("-J-D" + prop.getKey() + "=" + prop.getValue());
                        }
                    }
                }
                if (nativeConfig.userLanguage.isPresent()) {
                    nativeImageArgs.add("-J-Duser.language=" + nativeConfig.userLanguage.get());
                }
                if (nativeConfig.userCountry.isPresent()) {
                    nativeImageArgs.add("-J-Duser.country=" + nativeConfig.userCountry.get());
                }
                nativeImageArgs.add("-J-Dfile.encoding=" + nativeConfig.fileEncoding);

                if (enableSslNative) {
                    enableHttpsUrlHandler = true;
                    enableAllSecurityServices = true;
                }

                handleAdditionalProperties(nativeConfig, nativeImageArgs, isContainerBuild, outputDir);
                nativeImageArgs.add("--initialize-at-build-time=");
                nativeImageArgs.add(
                        "-H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy$BySpaceAndTime"); //the default collection policy results in full GC's 50% of the time
                nativeImageArgs.add("-H:+JNI");
                nativeImageArgs.add("-H:+AllowFoldMethods");
                nativeImageArgs.add("-jar");
                nativeImageArgs.add(runnerJarName);

                if (nativeConfig.enableFallbackImages) {
                    nativeImageArgs.add("-H:FallbackThreshold=5");
                } else {
                    //Default: be strict as those fallback images aren't very useful
                    //and tend to cover up real problems.
                    nativeImageArgs.add("-H:FallbackThreshold=0");
                }

                if (nativeConfig.reportErrorsAtRuntime) {
                    nativeImageArgs.add("-H:+ReportUnsupportedElementsAtRuntime");
                }
                if (nativeConfig.reportExceptionStackTraces) {
                    nativeImageArgs.add("-H:+ReportExceptionStackTraces");
                }
                if (nativeConfig.debug.enabled) {
                    nativeImageArgs.add("-g");
                    nativeImageArgs.add("-H:DebugInfoSourceSearchPath=" + APP_SOURCES);
                }
                if (nativeConfig.debugBuildProcess) {
                    nativeImageArgs
                            .add("-J-Xrunjdwp:transport=dt_socket,address=" + DEBUG_BUILD_PROCESS_PORT + ",server=y,suspend=y");
                }
                if (nativeConfig.enableReports) {
                    nativeImageArgs.add("-H:+PrintAnalysisCallTree");
                }
                if (nativeConfig.dumpProxies) {
                    nativeImageArgs.add("-Dsun.misc.ProxyGenerator.saveGeneratedFiles=true");
                    if (nativeConfig.enableServer) {
                        log.warn(
                                "Options dumpProxies and enableServer are both enabled: this will get the proxies dumped in an unknown external working directory");
                    }
                }
                if (nativeConfig.nativeImageXmx.isPresent()) {
                    nativeImageArgs.add("-J-Xmx" + nativeConfig.nativeImageXmx.get());
                }
                List<String> protocols = new ArrayList<>(2);
                if (nativeConfig.enableHttpUrlHandler) {
                    protocols.add("http");
                }
                if (enableHttpsUrlHandler) {
                    protocols.add("https");
                }
                if (addAllCharsets) {
                    nativeImageArgs.add("-H:+AddAllCharsets");
                } else {
                    nativeImageArgs.add("-H:-AddAllCharsets");
                }
                if (!protocols.isEmpty()) {
                    nativeImageArgs.add("-H:EnableURLProtocols=" + String.join(",", protocols));
                }
                if (enableAllSecurityServices) {
                    nativeImageArgs.add("--enable-all-security-services");
                }
                if (!noPIE.isEmpty()) {
                    nativeImageArgs.add("-H:NativeLinkerOption=" + noPIE);
                }

                if (!nativeConfig.enableIsolates) {
                    nativeImageArgs.add("-H:-SpawnIsolates");
                }
                if (!nativeConfig.enableJni) {
                    log.warn(
                            "Your application is setting the deprecated 'quarkus.native.enable-jni' configuration key to false."
                                    + " Please consider removing this configuration key as it is ignored (JNI is always enabled) and it"
                                    + " will be removed in a future Quarkus version.");
                }
                if (!nativeConfig.enableServer && !SystemUtils.IS_OS_WINDOWS && !graalVMVersion.isMandrel()) {
                    nativeImageArgs.add("--no-server");
                }
                if (nativeConfig.enableVmInspection) {
                    nativeImageArgs.add("-H:+AllowVMInspection");
                }
                if (nativeConfig.autoServiceLoaderRegistration) {
                    nativeImageArgs.add("-H:+UseServiceLoaderFeature");
                    //When enabling, at least print what exactly is being added:
                    nativeImageArgs.add("-H:+TraceServiceLoaderFeature");
                } else {
                    nativeImageArgs.add("-H:-UseServiceLoaderFeature");
                }
                if (nativeConfig.fullStackTraces) {
                    nativeImageArgs.add("-H:+StackTrace");
                } else {
                    nativeImageArgs.add("-H:-StackTrace");
                }

                if (nativeConfig.enableDashboardDump) {
                    nativeImageArgs.add("-H:DashboardDump=" + outputTargetBuildItem.getBaseName() + "_dashboard.dump");
                    nativeImageArgs.add("-H:+DashboardAll");
                }

                nativeImageArgs.add(resultingBinaryName);

                return new NativeImageInvokerInfo(nativeImageArgs);
            }

            private void handleAdditionalProperties(NativeConfig nativeConfig, List<String> command, boolean isContainerBuild,
                    Path outputDir) {
                if (nativeConfig.additionalBuildArgs.isPresent()) {
                    List<String> strings = nativeConfig.additionalBuildArgs.get();
                    for (String buildArg : strings) {
                        String trimmedBuildArg = buildArg.trim();
                        if (trimmedBuildArg.contains(TRUST_STORE_SYSTEM_PROPERTY_MARKER) && isContainerBuild) {
                            /*
                             * When the native binary is being built with a docker container, because a volume is created,
                             * we need to copy the trustStore file into the output directory (which is the root of volume)
                             * and change the value of 'javax.net.ssl.trustStore' property to point to this value
                             *
                             * TODO: we might want to introduce a dedicated property in order to overcome this ugliness
                             */
                            int index = trimmedBuildArg.indexOf(TRUST_STORE_SYSTEM_PROPERTY_MARKER);
                            if (trimmedBuildArg.length() > index + 2) {
                                String configuredTrustStorePath = trimmedBuildArg
                                        .substring(index + TRUST_STORE_SYSTEM_PROPERTY_MARKER.length());
                                try {
                                    IoUtils.copy(Paths.get(configuredTrustStorePath),
                                            outputDir.resolve(MOVED_TRUST_STORE_NAME));
                                    command.add(trimmedBuildArg.substring(0, index) + TRUST_STORE_SYSTEM_PROPERTY_MARKER
                                            + CONTAINER_BUILD_VOLUME_PATH + "/" + MOVED_TRUST_STORE_NAME);
                                } catch (IOException e) {
                                    throw new UncheckedIOException("Unable to copy trustStore file '" + configuredTrustStorePath
                                            + "' to volume root directory '" + outputDir.toAbsolutePath().toString() + "'", e);
                                }
                            }
                        } else {
                            command.add(trimmedBuildArg);
                        }
                    }
                }
            }
        }
    }
}
