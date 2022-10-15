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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAllowIncompleteClasspathAggregateBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageEnableModule;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeMinimalJavaVersionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsupportedOSBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabledBuildItem;
import io.quarkus.deployment.steps.LocaleProcessor;
import io.quarkus.deployment.steps.NativeImageFeatureStep;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.graal.DisableLoggingFeature;
import io.quarkus.runtime.graal.ResourcesFeature;

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

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void addExportsToNativeImage(BuildProducer<JPMSExportBuildItem> exports) {
        // Needed by io.quarkus.runtime.ResourceHelper.registerResources
        exports.produce(new JPMSExportBuildItem("org.graalvm.nativeimage.builder", "com.oracle.svm.core.jdk"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void nativeImageFeatures(BuildProducer<NativeImageFeatureBuildItem> features) {
        features.produce(new NativeImageFeatureBuildItem(NativeImageFeatureStep.GRAAL_FEATURE));
        features.produce(new NativeImageFeatureBuildItem(ResourcesFeature.class));
        features.produce(new NativeImageFeatureBuildItem(DisableLoggingFeature.class));
    }

    @BuildStep(onlyIf = NativeBuild.class)
    ArtifactResultBuildItem result(NativeImageBuildItem image) {
        NativeImageBuildItem.GraalVMVersion graalVMVersion = image.getGraalVMInfo();
        Map<String, Object> graalVMInfoProps = new HashMap<>();
        graalVMInfoProps.put("graalvm.version.full", graalVMVersion.getFullVersion());
        graalVMInfoProps.put("graalvm.version.version", graalVMVersion.getVersion());
        graalVMInfoProps.put("graalvm.version.javaVersion", "" + graalVMVersion.getJavaVersion());
        graalVMInfoProps.put("graalvm.version.distribution", graalVMVersion.getDistribution());
        return new ArtifactResultBuildItem(image.getPath(), PackageConfig.NATIVE, graalVMInfoProps);
    }

    @BuildStep(onlyIf = NativeSourcesBuild.class)
    ArtifactResultBuildItem nativeSourcesResult(NativeConfig nativeConfig,
            LocalesBuildTimeConfig localesBuildTimeConfig,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            PackageConfig packageConfig,
            List<NativeImageSystemPropertyBuildItem> nativeImageProperties,
            List<ExcludeConfigBuildItem> excludeConfigs,
            NativeImageAllowIncompleteClasspathAggregateBuildItem incompleteClassPathAllowed,
            List<NativeImageEnableModule> enableModules,
            List<JPMSExportBuildItem> jpmsExportBuildItems,
            List<NativeImageSecurityProviderBuildItem> nativeImageSecurityProviders,
            List<NativeImageFeatureBuildItem> nativeImageFeatures) {

        Path outputDir;
        try {
            outputDir = buildSystemTargetBuildItem.getOutputDirectory().resolve("native-sources");
            IoUtils.createOrEmptyDir(outputDir);
            IoUtils.copy(nativeImageSourceJarBuildItem.getPath().getParent(), outputDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create native-sources output directory", e);
        }

        Path runnerJar = outputDir.resolve(nativeImageSourceJarBuildItem.getPath().getFileName());

        String nativeImageName = getNativeImageName(outputTargetBuildItem, packageConfig);

        NativeImageInvokerInfo nativeImageArgs = new NativeImageInvokerInfo.Builder()
                .setNativeConfig(nativeConfig)
                .setLocalesBuildTimeConfig(localesBuildTimeConfig)
                .setOutputTargetBuildItem(outputTargetBuildItem)
                .setNativeImageProperties(nativeImageProperties)
                .setExcludeConfigs(excludeConfigs)
                .setJPMSExportBuildItems(jpmsExportBuildItems)
                .setEnableModules(enableModules)
                .setBrokenClasspath(incompleteClassPathAllowed.isAllow())
                .setNativeImageSecurityProviders(nativeImageSecurityProviders)
                .setOutputDir(outputDir)
                .setRunnerJarName(runnerJar.getFileName().toString())
                // the path to native-image is not known now, it is only known at the time the native-sources will be consumed
                .setNativeImageName(nativeImageName)
                .setGraalVMVersion(GraalVM.Version.CURRENT)
                .setNativeImageFeatures(nativeImageFeatures)
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
    public NativeImageBuildItem build(NativeConfig nativeConfig, LocalesBuildTimeConfig localesBuildTimeConfig,
            NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            PackageConfig packageConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<NativeImageSystemPropertyBuildItem> nativeImageProperties,
            List<ExcludeConfigBuildItem> excludeConfigs,
            NativeImageAllowIncompleteClasspathAggregateBuildItem incompleteClassPathAllowed,
            List<NativeImageSecurityProviderBuildItem> nativeImageSecurityProviders,
            List<JPMSExportBuildItem> jpmsExportBuildItems,
            List<NativeImageEnableModule> enableModules,
            List<NativeMinimalJavaVersionBuildItem> nativeMinimalJavaVersions,
            List<UnsupportedOSBuildItem> unsupportedOses,
            Optional<ProcessInheritIODisabled> processInheritIODisabled,
            Optional<ProcessInheritIODisabledBuildItem> processInheritIODisabledBuildItem,
            List<NativeImageFeatureBuildItem> nativeImageFeatures) {
        if (nativeConfig.debug.enabled) {
            copyJarSourcesToLib(outputTargetBuildItem, curateOutcomeBuildItem);
            copySourcesToSourceCache(outputTargetBuildItem);
        }

        Path runnerJar = nativeImageSourceJarBuildItem.getPath();
        log.info("Building native image from " + runnerJar);
        Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();

        final String runnerJarName = runnerJar.getFileName().toString();

        String noPIE = "";

        boolean isContainerBuild = nativeConfig.isContainerBuild();
        if (!isContainerBuild && SystemUtils.IS_OS_LINUX) {
            noPIE = detectNoPIE();
        }

        String nativeImageName = getNativeImageName(outputTargetBuildItem, packageConfig);
        String resultingExecutableName = getResultingExecutableName(nativeImageName, isContainerBuild);
        Path generatedExecutablePath = outputDir.resolve(resultingExecutableName);
        Path finalExecutablePath = outputTargetBuildItem.getOutputDirectory().resolve(resultingExecutableName);
        if (nativeConfig.reuseExisting) {
            if (Files.exists(finalExecutablePath)) {
                return new NativeImageBuildItem(finalExecutablePath,
                        NativeImageBuildItem.GraalVMVersion.unknown());
            }
        }

        NativeImageBuildRunner buildRunner = getNativeImageBuildRunner(nativeConfig, outputDir,
                nativeImageName, resultingExecutableName);
        buildRunner.setup(processInheritIODisabled.isPresent() || processInheritIODisabledBuildItem.isPresent());
        final GraalVM.Version graalVMVersion = buildRunner.getGraalVMVersion();

        if (graalVMVersion.isDetected()) {
            checkGraalVMVersion(graalVMVersion);
        } else {
            log.error("Unable to get GraalVM version from the native-image binary.");
        }

        try {
            if (nativeConfig.cleanupServer) {
                log.warn(
                        "Your application is setting the deprecated 'quarkus.native.cleanup-server' configuration key"
                                + " to true. Please consider removing this configuration key as it is ignored"
                                + " (The Native image build server is always disabled) and it will be removed in a"
                                + " future Quarkus version.");
            }

            NativeImageInvokerInfo commandAndExecutable = new NativeImageInvokerInfo.Builder()
                    .setNativeConfig(nativeConfig)
                    .setLocalesBuildTimeConfig(localesBuildTimeConfig)
                    .setOutputTargetBuildItem(outputTargetBuildItem)
                    .setNativeImageProperties(nativeImageProperties)
                    .setExcludeConfigs(excludeConfigs)
                    .setBrokenClasspath(incompleteClassPathAllowed.isAllow())
                    .setNativeImageSecurityProviders(nativeImageSecurityProviders)
                    .setJPMSExportBuildItems(jpmsExportBuildItems)
                    .setEnableModules(enableModules)
                    .setNativeMinimalJavaVersions(nativeMinimalJavaVersions)
                    .setUnsupportedOSes(unsupportedOses)
                    .setOutputDir(outputDir)
                    .setRunnerJarName(runnerJarName)
                    .setNativeImageName(nativeImageName)
                    .setNoPIE(noPIE)
                    .setGraalVMVersion(graalVMVersion)
                    .setNativeImageFeatures(nativeImageFeatures)
                    .build();

            List<String> nativeImageArgs = commandAndExecutable.args;

            NativeImageBuildRunner.Result buildNativeResult = buildRunner.build(nativeImageArgs, nativeImageName,
                    resultingExecutableName, outputDir,
                    nativeConfig.debug.enabled,
                    processInheritIODisabled.isPresent() || processInheritIODisabledBuildItem.isPresent());
            if (buildNativeResult.getExitCode() != 0) {
                throw imageGenerationFailed(buildNativeResult.getExitCode(), nativeConfig.isContainerBuild());
            }
            IoUtils.copy(generatedExecutablePath, finalExecutablePath);
            Files.delete(generatedExecutablePath);
            if (nativeConfig.debug.enabled) {
                if (buildNativeResult.isObjcopyExists()) {
                    final String symbolsName = String.format("%s.debug", nativeImageName);
                    Path generatedSymbols = outputDir.resolve(symbolsName);
                    Path finalSymbolsPath = outputTargetBuildItem.getOutputDirectory().resolve(symbolsName);
                    IoUtils.copy(generatedSymbols, finalSymbolsPath);
                    Files.delete(generatedSymbols);
                }
            }
            System.setProperty("native.image.path", finalExecutablePath.toAbsolutePath().toString());

            return new NativeImageBuildItem(finalExecutablePath,
                    new NativeImageBuildItem.GraalVMVersion(graalVMVersion.fullVersion,
                            graalVMVersion.version.toString(),
                            graalVMVersion.javaFeatureVersion,
                            graalVMVersion.distribution.name()));
        } catch (ImageGenerationFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build native image", e);
        } finally {
            if (nativeConfig.debug.enabled) {
                removeJarSourcesFromLib(outputTargetBuildItem);
                IoUtils.recursiveDelete(outputDir.resolve(Paths.get(APP_SOURCES)));
            }
        }
    }

    private String getNativeImageName(OutputTargetBuildItem outputTargetBuildItem, PackageConfig packageConfig) {
        return outputTargetBuildItem.getBaseName() + packageConfig.getRunnerSuffix();
    }

    private String getResultingExecutableName(String nativeImageName, boolean isContainerBuild) {
        String resultingExecutableName = nativeImageName;
        if (SystemUtils.IS_OS_WINDOWS && !isContainerBuild) {
            //once image is generated it gets added .exe on Windows
            resultingExecutableName = resultingExecutableName + ".exe";
        }
        return resultingExecutableName;
    }

    private static NativeImageBuildRunner getNativeImageBuildRunner(NativeConfig nativeConfig, Path outputDir,
            String nativeImageName, String resultingExecutableName) {
        if (!nativeConfig.isContainerBuild()) {
            NativeImageBuildLocalRunner localRunner = getNativeImageBuildLocalRunner(nativeConfig, outputDir.toFile());
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
            return new NativeImageBuildRemoteContainerRunner(nativeConfig, outputDir,
                    nativeImageName, resultingExecutableName);
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

        for (ResolvedDependency depArtifact : curateOutcomeBuildItem.getApplicationModel().getRuntimeDependencies()) {
            if (depArtifact.isJar()) {
                for (Path resolvedDep : depArtifact.getResolvedPaths()) {
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

    private RuntimeException imageGenerationFailed(int exitValue, boolean isContainerBuild) {
        if (exitValue == OOM_ERROR_VALUE) {
            if (isContainerBuild && !SystemUtils.IS_OS_LINUX) {
                return new ImageGenerationFailureException("Image generation failed. Exit code was " + exitValue
                        + " which indicates an out of memory error. The most likely cause is Docker not being given enough memory. Also consider increasing the Xmx value for native image generation by setting the \""
                        + QUARKUS_XMX_PROPERTY + "\" property");
            } else {
                return new ImageGenerationFailureException("Image generation failed. Exit code was " + exitValue
                        + " which indicates an out of memory error. Consider increasing the Xmx value for native image generation by setting the \""
                        + QUARKUS_XMX_PROPERTY + "\" property");
            }
        } else {
            return new ImageGenerationFailureException("Image generation failed. Exit code: " + exitValue);
        }
    }

    private void checkGraalVMVersion(GraalVM.Version version) {
        log.info("Running Quarkus native-image plugin on " + version.getFullVersion());
        if (version.isObsolete()) {
            throw new IllegalStateException("Out of date version of GraalVM detected: " + version.getFullVersion() + "."
                    + " Quarkus currently supports " + GraalVM.Version.CURRENT.version
                    + ". Please upgrade GraalVM to this version.");
        }
    }

    private static NativeImageBuildLocalRunner getNativeImageBuildLocalRunner(NativeConfig nativeConfig, File outputDir) {
        String executableName = getNativeImageExecutableName();
        if (nativeConfig.graalvmHome.isPresent()) {
            File file = Paths.get(nativeConfig.graalvmHome.get(), "bin", executableName).toFile();
            if (file.exists()) {
                return new NativeImageBuildLocalRunner(file.getAbsolutePath(), outputDir);
            }
        }

        File javaHome = nativeConfig.javaHome;
        if (javaHome == null) {
            // try system property first - it will be the JAVA_HOME used by the current JVM
            String home = System.getProperty(JAVA_HOME_SYS);
            if (home == null) {
                // No luck, somewhat an odd JVM not enforcing this property
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
                return new NativeImageBuildLocalRunner(file.getAbsolutePath(), outputDir);
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
                        return new NativeImageBuildLocalRunner(file.getAbsolutePath(), outputDir);
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
            private LocalesBuildTimeConfig localesBuildTimeConfig;
            private OutputTargetBuildItem outputTargetBuildItem;
            private List<NativeImageSystemPropertyBuildItem> nativeImageProperties;
            private List<ExcludeConfigBuildItem> excludeConfigs;
            private List<NativeImageSecurityProviderBuildItem> nativeImageSecurityProviders;
            private List<JPMSExportBuildItem> jpmsExports;
            private List<NativeImageEnableModule> enableModules;
            private List<NativeMinimalJavaVersionBuildItem> nativeMinimalJavaVersions;
            private List<UnsupportedOSBuildItem> unsupportedOSes;
            private List<NativeImageFeatureBuildItem> nativeImageFeatures;
            private Path outputDir;
            private String runnerJarName;
            private String noPIE = "";
            private GraalVM.Version graalVMVersion = GraalVM.Version.UNVERSIONED;
            private String nativeImageName;
            private boolean classpathIsBroken;

            public Builder setNativeConfig(NativeConfig nativeConfig) {
                this.nativeConfig = nativeConfig;
                return this;
            }

            public Builder setLocalesBuildTimeConfig(LocalesBuildTimeConfig localesBuildTimeConfig) {
                this.localesBuildTimeConfig = localesBuildTimeConfig;
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

            public Builder setBrokenClasspath(boolean classpathIsBroken) {
                this.classpathIsBroken = classpathIsBroken;
                return this;
            }

            public Builder setExcludeConfigs(List<ExcludeConfigBuildItem> excludeConfigs) {
                this.excludeConfigs = excludeConfigs;
                return this;
            }

            public Builder setNativeImageSecurityProviders(
                    List<NativeImageSecurityProviderBuildItem> nativeImageSecurityProviders) {
                this.nativeImageSecurityProviders = nativeImageSecurityProviders;
                return this;
            }

            public Builder setJPMSExportBuildItems(List<JPMSExportBuildItem> JPMSExportBuildItems) {
                this.jpmsExports = JPMSExportBuildItems;
                return this;
            }

            public Builder setEnableModules(List<NativeImageEnableModule> modules) {
                this.enableModules = modules;
                return this;
            }

            public Builder setNativeMinimalJavaVersions(
                    List<NativeMinimalJavaVersionBuildItem> nativeMinimalJavaVersions) {
                this.nativeMinimalJavaVersions = nativeMinimalJavaVersions;
                return this;
            }

            public Builder setUnsupportedOSes(
                    List<UnsupportedOSBuildItem> unsupportedOSes) {
                this.unsupportedOSes = unsupportedOSes;
                return this;
            }

            public Builder setNativeImageFeatures(List<NativeImageFeatureBuildItem> nativeImageFeatures) {
                this.nativeImageFeatures = nativeImageFeatures;
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

            public Builder setGraalVMVersion(GraalVM.Version graalVMVersion) {
                this.graalVMVersion = graalVMVersion;
                return this;
            }

            public Builder setNativeImageName(String nativeImageName) {
                this.nativeImageName = nativeImageName;
                return this;
            }

            public NativeImageInvokerInfo build() {
                List<String> nativeImageArgs = new ArrayList<>();
                boolean enableSslNative = false;
                boolean inlineBeforeAnalysis = nativeConfig.inlineBeforeAnalysis;
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
                        log.warn(
                                "Your application is setting the deprecated 'quarkus.native.enable-all-security-services' configuration key."
                                        + " Please consider removing this configuration key as it is ignored and it"
                                        + " will be removed in a future Quarkus version.");
                    } else if (prop.getKey().equals("quarkus.native.enable-all-charsets") && prop.getValue() != null) {
                        addAllCharsets |= Boolean.parseBoolean(prop.getValue());
                    } else if (prop.getKey().equals("quarkus.native.inline-before-analysis") && prop.getValue() != null) {
                        inlineBeforeAnalysis = Boolean.parseBoolean(prop.getValue());
                    } else {
                        // todo maybe just -D is better than -J-D in this case
                        if (prop.getValue() == null) {
                            nativeImageArgs.add("-J-D" + prop.getKey());
                        } else {
                            nativeImageArgs.add("-J-D" + prop.getKey() + "=" + prop.getValue());
                        }
                    }
                }

                final String userLanguage = LocaleProcessor.nativeImageUserLanguage(nativeConfig, localesBuildTimeConfig);
                if (!userLanguage.isEmpty()) {
                    nativeImageArgs.add("-J-Duser.language=" + userLanguage);
                }
                final String userCountry = LocaleProcessor.nativeImageUserCountry(nativeConfig, localesBuildTimeConfig);
                if (!userCountry.isEmpty()) {
                    nativeImageArgs.add("-J-Duser.country=" + userCountry);
                }
                final String includeLocales = LocaleProcessor.nativeImageIncludeLocales(nativeConfig, localesBuildTimeConfig);
                if (!includeLocales.isEmpty()) {
                    nativeImageArgs.add("-H:IncludeLocales=" + includeLocales);
                }

                nativeImageArgs.add("-J-Dfile.encoding=" + nativeConfig.fileEncoding);

                if (enableSslNative) {
                    enableHttpsUrlHandler = true;
                }

                if (nativeImageFeatures == null || nativeImageFeatures.isEmpty()) {
                    throw new IllegalStateException("GraalVM features can't be empty, quarkus core is using some.");
                }
                List<String> featuresList = new ArrayList<>(nativeImageFeatures.size());
                for (NativeImageFeatureBuildItem nativeImageFeature : nativeImageFeatures) {
                    featuresList.add(nativeImageFeature.getQualifiedName());
                }
                nativeImageArgs.add("--features=" + String.join(",", featuresList));

                if (graalVMVersion.isOlderThan(GraalVM.Version.VERSION_22_2_0)) {
                    /*
                     * Instruct GraalVM / Mandrel parse compiler graphs twice, once for the static analysis and once again
                     * for the AOT compilation.
                     *
                     * We do this because single parsing significantly increases memory usage at build time
                     * see https://github.com/oracle/graal/issues/3435 and
                     * https://github.com/graalvm/mandrel/issues/304#issuecomment-952070568 for more details.
                     *
                     * Note: This option must come before the invocation of
                     * {@code handleAdditionalProperties(nativeImageArgs)} to ensure that devs and advanced users can
                     * override it by passing -Dquarkus.native.additional-build-args=-H:+ParseOnce
                     */
                    nativeImageArgs.add("-H:-ParseOnce");
                }

                /**
                 * This makes sure the Kerberos integration module is made available in case any library
                 * refers to it (e.g. the PostgreSQL JDBC requires it, seems plausible that many others will as well):
                 * the module is not available by default on Java 17.
                 * No flag was introduced as this merely exposes the visibility of the module, it doesn't
                 * control its actual inclusion which will depend on the usual analysis.
                 */
                nativeImageArgs.add("-J--add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED");

                //address https://github.com/quarkusio/quarkus-quickstarts/issues/993
                nativeImageArgs.add("-J--add-opens=java.base/java.text=ALL-UNNAMED");
                // kogito-dmn-quickstart is failing if we don't have this
                nativeImageArgs.add("-J--add-opens=java.base/java.io=ALL-UNNAMED");
                // mybatis extension
                nativeImageArgs.add("-J--add-opens=java.base/java.lang.invoke=ALL-UNNAMED");
                // required by camel-quarkus-xstream
                nativeImageArgs.add("-J--add-opens=java.base/java.util=ALL-UNNAMED");

                if (nativeConfig.enableReports) {
                    nativeImageArgs.add("-H:PrintAnalysisCallTreeType=CSV");
                }

                // only available in GraalVM 22.3.0 and better.
                if (graalVMVersion.compareTo(GraalVM.Version.VERSION_22_3_0) >= 0) {
                    // For build time information
                    nativeImageArgs.add("-H:+CollectImageBuildStatistics");
                    nativeImageArgs.add("-H:ImageBuildStatisticsFile=" + nativeImageName + "-timing-stats.json");
                    // For getting the build output stats as a JSON file
                    nativeImageArgs.add("-H:BuildOutputJSONFile=" + nativeImageName + "-build-output-stats.json");
                }

                /*
                 * Any parameters following this call are forced over the user provided parameters in
                 * quarkus.native.additional-build-args. So if you need a parameter to be overridable through
                 * quarkus.native.additional-build-args please make sure to add it before this call.
                 */
                handleAdditionalProperties(nativeImageArgs);

                nativeImageArgs.add("-H:+AllowFoldMethods");

                if (nativeConfig.headless) {
                    nativeImageArgs.add("-J-Djava.awt.headless=true");
                }

                if (nativeConfig.enableFallbackImages) {
                    nativeImageArgs.add("--auto-fallback");
                } else {
                    //Default: be strict as those fallback images aren't very useful
                    //and tend to cover up real problems.
                    nativeImageArgs.add("--no-fallback");
                }

                if (!classpathIsBroken) {
                    nativeImageArgs.add("--link-at-build-time");
                }

                if (nativeConfig.reportErrorsAtRuntime) {
                    nativeImageArgs.add("--report-unsupported-elements-at-runtime");
                }
                if (nativeConfig.reportExceptionStackTraces) {
                    nativeImageArgs.add("-H:+ReportExceptionStackTraces");
                }
                if (nativeConfig.debug.enabled) {
                    nativeImageArgs.add("-g");
                    nativeImageArgs.add("-H:DebugInfoSourceSearchPath=" + APP_SOURCES);
                }
                if (nativeConfig.debugBuildProcess) {
                    String debugBuildProcessHost;
                    if (nativeConfig.isContainerBuild()) {
                        debugBuildProcessHost = "0.0.0.0";
                    } else {
                        debugBuildProcessHost = "localhost";
                    }
                    nativeImageArgs
                            .add("-J-Xrunjdwp:transport=dt_socket,address=" + debugBuildProcessHost + ":"
                                    + DEBUG_BUILD_PROCESS_PORT + ",server=y,suspend=y");
                }
                if (nativeConfig.dumpProxies) {
                    nativeImageArgs.add("-Dsun.misc.ProxyGenerator.saveGeneratedFiles=true");
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
                    nativeImageArgs.add("--enable-url-protocols=" + String.join(",", protocols));
                }
                if (!inlineBeforeAnalysis) {
                    nativeImageArgs.add("-H:-InlineBeforeAnalysis");
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
                if (nativeConfig.enableServer) {
                    log.warn(
                            "Your application is setting the deprecated 'quarkus.native.enable-server' configuration key to true."
                                    + " Please consider removing this configuration key as it is ignored"
                                    + " (The Native image build server is always disabled) and it"
                                    + " will be removed in a future Quarkus version.");
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

                if (nativeImageSecurityProviders != null && !nativeImageSecurityProviders.isEmpty()) {
                    String additionalSecurityProviders = nativeImageSecurityProviders.stream()
                            .map(p -> p.getSecurityProvider())
                            .collect(Collectors.joining(","));
                    nativeImageArgs.add("-H:AdditionalSecurityProviders=" + additionalSecurityProviders);
                }

                if (jpmsExports != null) {
                    HashSet<JPMSExportBuildItem> deduplicatedJpmsExport = new HashSet<>(jpmsExports);
                    for (JPMSExportBuildItem jpmsExport : deduplicatedJpmsExport) {
                        if (jpmsExport.isRequired(graalVMVersion)) {
                            nativeImageArgs.add(
                                    "-J--add-exports=" + jpmsExport.getModule() + "/" + jpmsExport.getPackage()
                                            + "=ALL-UNNAMED");
                        }
                    }
                }
                if (enableModules != null && enableModules.size() > 0) {
                    String modules = enableModules.stream().map(NativeImageEnableModule::getModuleName).distinct().sorted()
                            .collect(Collectors.joining(","));
                    nativeImageArgs.add("--add-modules=" + modules);
                }

                if (nativeMinimalJavaVersions != null && !nativeMinimalJavaVersions.isEmpty()) {
                    if (graalVMVersion.javaUpdateVersion == GraalVM.Version.UNDEFINED) {
                        log.warnf(
                                "Unable to parse used Java version from native-image version string `%s'. Java version checks will be skipped.",
                                graalVMVersion.fullVersion);
                    } else {
                        nativeMinimalJavaVersions.stream()
                                .filter(a -> !graalVMVersion.jdkVersionGreaterOrEqualTo(a.minFeature, a.minUpdate))
                                .forEach(a -> log.warnf("Expected: Java %d, update %d, Actual: Java %d, update %d. %s",
                                        a.minFeature, a.minUpdate, graalVMVersion.javaFeatureVersion,
                                        graalVMVersion.javaUpdateVersion, a.warning));
                    }
                }

                if (unsupportedOSes != null && !unsupportedOSes.isEmpty()) {
                    final String errs = unsupportedOSes.stream()
                            .filter(o -> o.triggerError(nativeConfig))
                            .map(o -> o.error)
                            .collect(Collectors.joining(", "));
                    if (!errs.isEmpty()) {
                        throw new UnsupportedOperationException(errs);
                    }
                }

                for (ExcludeConfigBuildItem excludeConfig : excludeConfigs) {
                    nativeImageArgs.add("--exclude-config");
                    nativeImageArgs.add(excludeConfig.getJarFile());
                    nativeImageArgs.add(excludeConfig.getResourceName());
                }

                nativeImageArgs.add(nativeImageName);

                //Make sure to have the -jar as last one, as it otherwise breaks "--exclude-config"
                nativeImageArgs.add("-jar");
                nativeImageArgs.add(runnerJarName);

                return new NativeImageInvokerInfo(nativeImageArgs);
            }

            private void handleAdditionalProperties(List<String> command) {
                if (nativeConfig.additionalBuildArgs.isPresent()) {
                    List<String> strings = nativeConfig.additionalBuildArgs.get();
                    for (String buildArg : strings) {
                        String trimmedBuildArg = buildArg.trim();
                        if (trimmedBuildArg.contains(TRUST_STORE_SYSTEM_PROPERTY_MARKER) && nativeConfig.isContainerBuild()) {
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

    private static class ImageGenerationFailureException extends RuntimeException {

        private ImageGenerationFailureException(String message) {
            super(message);
        }
    }
}
