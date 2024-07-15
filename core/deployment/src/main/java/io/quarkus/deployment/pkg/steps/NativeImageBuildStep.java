package io.quarkus.deployment.pkg.steps;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import io.quarkus.deployment.builditem.SuppressNonRuntimeConfigChangedWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAgentConfigDirectoryBuildItem;
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
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabledBuildItem;
import io.quarkus.deployment.steps.LocaleProcessor;
import io.quarkus.deployment.steps.NativeImageFeatureStep;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.graal.DisableLoggingFeature;

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
    void nativeImageFeatures(BuildProducer<NativeImageFeatureBuildItem> features) {
        features.produce(new NativeImageFeatureBuildItem(NativeImageFeatureStep.GRAAL_FEATURE));
        features.produce(new NativeImageFeatureBuildItem(DisableLoggingFeature.class));
    }

    @BuildStep(onlyIf = NativeBuild.class)
    ArtifactResultBuildItem result(NativeImageBuildItem image) {
        NativeImageBuildItem.GraalVMVersion graalVMVersion = image.getGraalVMInfo();
        return new ArtifactResultBuildItem(image.getPath(), "native",
                graalVMVersion.toMap());
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
            List<NativeImageFeatureBuildItem> nativeImageFeatures,
            NativeImageRunnerBuildItem nativeImageRunner) {

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
                .setContainerBuild(nativeImageRunner.isContainerBuild())
                .build();
        List<String> command = nativeImageArgs.getArgs();

        try {
            Files.writeString(outputDir.resolve("native-image.args"), String.join(" ", command));
            Files.writeString(outputDir.resolve("graalvm.version"), GraalVM.Version.CURRENT.getVersionAsString());
            if (nativeImageRunner.isContainerBuild()) {
                Files.writeString(outputDir.resolve("native-builder.image"), nativeConfig.builderImage().getEffectiveImage());
            }
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Failed to build native image sources", e);
        }

        log.info("The sources for a subsequent native-image run along with the necessary arguments can be found in "
                + outputDir);

        // drop the original output to avoid confusion
        IoUtils.recursiveDelete(nativeImageSourceJarBuildItem.getPath().getParent());

        return new ArtifactResultBuildItem(nativeImageSourceJarBuildItem.getPath(),
                "native-sources",
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
            List<NativeImageFeatureBuildItem> nativeImageFeatures,
            Optional<NativeImageAgentConfigDirectoryBuildItem> nativeImageAgentConfigDirectoryBuildItem,
            NativeImageRunnerBuildItem nativeImageRunner) {
        if (nativeConfig.debug().enabled()) {
            copyJarSourcesToLib(outputTargetBuildItem, curateOutcomeBuildItem);
            copySourcesToSourceCache(outputTargetBuildItem);
        }

        Path runnerJar = nativeImageSourceJarBuildItem.getPath();
        log.info("Building native image from " + runnerJar);
        Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();
        final String runnerJarName = runnerJar.getFileName().toString();

        String pie = "";

        boolean isContainerBuild = nativeImageRunner.isContainerBuild();
        if (!isContainerBuild && SystemUtils.IS_OS_LINUX) {
            if (nativeConfig.pie().isPresent() && nativeConfig.pie().get()) {
                pie = detectPIE();
            } else {
                pie = detectNoPIE();
            }
        }

        String nativeImageName = getNativeImageName(outputTargetBuildItem, packageConfig);
        String resultingExecutableName = getResultingExecutableName(nativeImageName, nativeImageRunner.isContainerBuild());
        Path generatedExecutablePath = outputDir.resolve(resultingExecutableName);
        Path finalExecutablePath = outputTargetBuildItem.getOutputDirectory().resolve(resultingExecutableName);
        if (nativeConfig.reuseExisting()) {
            if (Files.exists(finalExecutablePath)) {
                return new NativeImageBuildItem(finalExecutablePath,
                        NativeImageBuildItem.GraalVMVersion.unknown());
            }
        }

        NativeImageBuildRunner buildRunner = nativeImageRunner.getBuildRunner();

        buildRunner.setup(processInheritIODisabled.isPresent() || processInheritIODisabledBuildItem.isPresent());
        final GraalVM.Version graalVMVersion = buildRunner.getGraalVMVersion();
        checkGraalVMVersion(graalVMVersion);

        try {
            if (nativeConfig.cleanupServer()) {
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
                    .setPIE(pie)
                    .setGraalVMVersion(graalVMVersion)
                    .setNativeImageFeatures(nativeImageFeatures)
                    .setContainerBuild(isContainerBuild)
                    .setNativeImageAgentConfigDirectory(nativeImageAgentConfigDirectoryBuildItem)
                    .build();

            List<String> nativeImageArgs = commandAndExecutable.args;

            NativeImageBuildRunner.Result buildNativeResult = buildRunner.build(nativeImageArgs,
                    nativeImageName,
                    resultingExecutableName, outputDir,
                    graalVMVersion, nativeConfig.debug().enabled(),
                    processInheritIODisabled.isPresent() || processInheritIODisabledBuildItem.isPresent());
            if (buildNativeResult.getExitCode() != 0) {
                throw imageGenerationFailed(buildNativeResult.getExitCode(), isContainerBuild);
            }
            IoUtils.copy(generatedExecutablePath, finalExecutablePath);
            Files.delete(generatedExecutablePath);
            if (nativeConfig.debug().enabled()) {
                final String symbolsName = String.format("%s.debug", nativeImageName);
                Path generatedSymbols = outputDir.resolve(symbolsName);
                if (generatedSymbols.toFile().exists()) {
                    Path finalSymbolsPath = outputTargetBuildItem.getOutputDirectory().resolve(symbolsName);
                    IoUtils.copy(generatedSymbols, finalSymbolsPath);
                    Files.delete(generatedSymbols);
                }
            }

            if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_0_0) >= 0) {
                // See https://github.com/oracle/graal/issues/4921
                try (DirectoryStream<Path> sharedLibs = Files.newDirectoryStream(outputDir, "*.{so,dll}")) {
                    sharedLibs.forEach(src -> {
                        Path dst = null;
                        try {
                            dst = Path.of(outputTargetBuildItem.getOutputDirectory().toAbsolutePath().toString(),
                                    src.getFileName().toString());
                            log.debugf("Copying a shared lib from %s to %s.", src, dst);
                            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            log.errorf("Could not copy shared lib from %s to %s. Continuing. Error: %s", src, dst, e);
                        }
                    });
                } catch (IOException e) {
                    log.errorf("Could not list files in directory %s. Continuing. Error: %s", outputDir, e);
                }
            }

            System.setProperty("native.image.path", finalExecutablePath.toAbsolutePath().toString());

            return new NativeImageBuildItem(finalExecutablePath,
                    new NativeImageBuildItem.GraalVMVersion(graalVMVersion.fullVersion,
                            graalVMVersion.getVersionAsString(),
                            graalVMVersion.javaVersion.feature(),
                            graalVMVersion.distribution.name()));
        } catch (ImageGenerationFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build native image", e);
        } finally {
            if (nativeConfig.debug().enabled()) {
                removeJarSourcesFromLib(outputTargetBuildItem);
                IoUtils.recursiveDelete(outputDir.resolve(Paths.get(APP_SOURCES)));
            }
        }
    }

    private String getNativeImageName(OutputTargetBuildItem outputTargetBuildItem, PackageConfig packageConfig) {
        return outputTargetBuildItem.getBaseName() + packageConfig.computedRunnerSuffix();
    }

    private String getResultingExecutableName(String nativeImageName, boolean isContainerBuild) {
        String resultingExecutableName = nativeImageName;
        if (SystemUtils.IS_OS_WINDOWS && !isContainerBuild) {
            //once image is generated it gets added .exe on Windows
            resultingExecutableName = resultingExecutableName + ".exe";
        }
        return resultingExecutableName;
    }

    /**
     * Resolves the runner factory. Happens quite early, *before* the build.
     */
    @BuildStep(onlyIf = NativeBuild.class)
    public NativeImageRunnerBuildItem resolveNativeImageBuildRunner(NativeConfig nativeConfig) {
        boolean isExplicitContainerBuild = nativeConfig.containerBuild()
                .orElse(nativeConfig.containerRuntime().isPresent() || nativeConfig.remoteContainerBuild());
        if (!isExplicitContainerBuild) {
            NativeImageBuildLocalRunner localRunner = getNativeImageBuildLocalRunner(nativeConfig);
            if (localRunner != null) {
                return new NativeImageRunnerBuildItem(localRunner);
            }
            String executableName = getNativeImageExecutableName();
            String errorMessage = "Cannot find the `" + executableName
                    + "` in the GRAALVM_HOME, JAVA_HOME and System PATH. Install it using `gu install native-image`";
            if (!SystemUtils.IS_OS_LINUX) {
                // Delay the error: if we're just building native sources, we may not need the build runner at all.
                return new NativeImageRunnerBuildItem(new NativeImageBuildRunnerError(errorMessage));
            }
            log.warn(errorMessage + " Attempting to fall back to container build.");
        }
        if (nativeConfig.remoteContainerBuild()) {
            return new NativeImageRunnerBuildItem(new NativeImageBuildRemoteContainerRunner(nativeConfig));
        }
        return new NativeImageRunnerBuildItem(new NativeImageBuildLocalContainerRunner(nativeConfig));
    }

    /**
     * Creates a dummy runner for native-sources builds. This allows the creation of native-source jars without requiring
     * podman/docker or a local native-image installation.
     */
    @BuildStep(onlyIf = NativeSourcesBuild.class)
    public NativeImageRunnerBuildItem dummyNativeImageBuildRunner(NativeConfig nativeConfig) {
        boolean explicitContainerBuild = nativeConfig.isExplicitContainerBuild();
        return new NativeImageRunnerBuildItem(new NoopNativeImageBuildRunner(explicitContainerBuild));
    }

    @BuildStep
    public void ignoreBuildPropertyChanges(BuildProducer<SuppressNonRuntimeConfigChangedWarningBuildItem> producer) {
        // Don't produce warnings on static init for properties that are overridden through environment variables
        // if they are clearly only relevant when building.
        for (String propertyKey : new String[] {
                "quarkus.native.container-build",
                "quarkus.native.remote-container-build",
                "quarkus.native.builder-image.image",
                "quarkus.native.builder-image.pull",
                "quarkus.native.container-runtime",
                "quarkus.native.container-runtime-options"
        }) {
            producer.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem(propertyKey));
        }
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
        log.info("Running Quarkus native-image plugin on " + version.distribution.name() + " " + version.getVersionAsString()
                + " JDK " + version.javaVersion);
        if (version.isObsolete()) {
            throw new IllegalStateException(
                    "Out of date version of GraalVM or Mandrel detected: " + version.getVersionAsString() + "."
                            + " Quarkus currently supports " + GraalVM.Version.CURRENT.getVersionAsString()
                            + ". Please upgrade to this version.");
        }
        if (!version.isSupported()) {
            log.warn("You are using an older version of GraalVM or Mandrel : " + version.getVersionAsString() + "."
                    + " Quarkus currently supports " + GraalVM.Version.CURRENT.getVersionAsString()
                    + ". Please upgrade to this version.");
        }
    }

    private static NativeImageBuildLocalRunner getNativeImageBuildLocalRunner(NativeConfig nativeConfig) {
        String executableName = getNativeImageExecutableName();
        if (nativeConfig.graalvmHome().isPresent()) {
            File file = Paths.get(nativeConfig.graalvmHome().get(), "bin", executableName).toFile();
            if (file.exists()) {
                return new NativeImageBuildLocalRunner(file.getAbsolutePath());
            }
        }

        File javaHome = nativeConfig.javaHome();
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

    private static String detectPIE() {
        return testGCCArgument("-pie");
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
            private String pie = "";
            private GraalVM.Version graalVMVersion = null;
            private String nativeImageName;
            private boolean classpathIsBroken;
            private boolean containerBuild;
            private Optional<NativeImageAgentConfigDirectoryBuildItem> nativeImageAgentConfigDirectory = Optional.empty();

            public Builder setNativeConfig(NativeConfig nativeConfig) {
                this.nativeConfig = nativeConfig;
                return this;
            }

            public Builder setNativeImageAgentConfigDirectory(
                    Optional<NativeImageAgentConfigDirectoryBuildItem> nativeImageAgentConfigDirectory) {
                this.nativeImageAgentConfigDirectory = nativeImageAgentConfigDirectory;
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

            public Builder setContainerBuild(boolean containerBuild) {
                this.containerBuild = containerBuild;
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

            public Builder setPIE(String pie) {
                this.pie = pie;
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

            @SuppressWarnings("deprecation")
            public NativeImageInvokerInfo build() {
                List<String> nativeImageArgs = new ArrayList<>();
                boolean enableSslNative = false;
                boolean inlineBeforeAnalysis = nativeConfig.inlineBeforeAnalysis();
                boolean addAllCharsets = nativeConfig.addAllCharsets();
                boolean enableHttpsUrlHandler = nativeConfig.enableHttpsUrlHandler();
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
                    if ("all".equals(includeLocales)) {
                        log.warn(
                                "Your application is setting the 'quarkus.locales' configuration key to include 'all'. " +
                                        "All JDK locales, languages, currencies, etc. will be included, inflating the size of the executable.");
                        addExperimentalVMOption(nativeImageArgs, "-H:+IncludeAllLocales");
                    } else {
                        addExperimentalVMOption(nativeImageArgs, "-H:IncludeLocales=" + includeLocales);
                    }
                }

                nativeImageArgs.add("-J-Dfile.encoding=" + nativeConfig.fileEncoding());

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

                if (nativeConfig.debug().enabled() && graalVMVersion.compareTo(GraalVM.Version.VERSION_23_0_0) >= 0) {
                    /*
                     * Instruct GraalVM / Mandrel to keep more accurate information about source locations when generating
                     * debug info for debugging and monitoring tools. This parameter may break compatibility with Truffle.
                     * Affected users should explicitly pass {@code -H:-TrackNodeSourcePosition} through
                     * {@code quarkus.native.additional-build-args} to override it.
                     *
                     * See https://github.com/quarkusio/quarkus/issues/30772 for more details.
                     */
                    addExperimentalVMOption(nativeImageArgs, "-H:+TrackNodeSourcePosition");
                    /* See https://github.com/Karm/mandrel-integration-tests/issues/154 for more details. */
                    addExperimentalVMOption(nativeImageArgs, "-H:+DebugCodeInfoUseSourceMappings");
                }

                /**
                 * This makes sure the Kerberos integration module is made available in case any library
                 * refers to it (e.g. the PostgreSQL JDBC requires it, seems plausible that many others will as well):
                 * the module is not available by default on Java 17.
                 * No flag was introduced as this merely exposes the visibility of the module, it doesn't
                 * control its actual inclusion which will depend on the usual analysis.
                 */
                nativeImageArgs.add("-J--add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED");
                nativeImageArgs.add("-J--add-exports=java.security.jgss/sun.security.jgss=ALL-UNNAMED");

                //address https://github.com/quarkusio/quarkus-quickstarts/issues/993
                nativeImageArgs.add("-J--add-opens=java.base/java.text=ALL-UNNAMED");
                // kogito-dmn-quickstart is failing if we don't have this
                nativeImageArgs.add("-J--add-opens=java.base/java.io=ALL-UNNAMED");
                // mybatis extension
                nativeImageArgs.add("-J--add-opens=java.base/java.lang.invoke=ALL-UNNAMED");
                // required by camel-quarkus-xstream
                nativeImageArgs.add("-J--add-opens=java.base/java.util=ALL-UNNAMED");

                if (nativeConfig.enableReports()) {
                    addExperimentalVMOption(nativeImageArgs, "-H:PrintAnalysisCallTreeType=CSV");
                }

                // only available in GraalVM 22.3.0+.
                if (graalVMVersion.compareTo(GraalVM.Version.VERSION_22_3_0) >= 0) {
                    if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_0_0) < 0) {
                        // Used to retrieve build time information in 22.3. Starting with 23.0 this info is included in
                        // the build output json file so there is no need to generate extra files.
                        nativeImageArgs.add("-H:+CollectImageBuildStatistics");
                        nativeImageArgs.add("-H:ImageBuildStatisticsFile=" + nativeImageName + "-timing-stats.json");
                    }
                    // For getting the build output stats as a JSON file
                    addExperimentalVMOption(nativeImageArgs,
                            "-H:BuildOutputJSONFile=" + nativeImageName + "-build-output-stats.json");
                }

                // only available in GraalVM 23.0+, we want a file with the list of built artifacts
                if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_0_0) >= 0) {
                    addExperimentalVMOption(nativeImageArgs, "-H:+GenerateBuildArtifactsFile");
                }

                // only available in GraalVM 23.1.0+
                if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_1_0) >= 0) {
                    if (graalVMVersion.compareTo(GraalVM.Version.VERSION_24_0_0) < 0) {
                        // Enabled by default in GraalVM 24.0.0.
                        nativeImageArgs.add("--strict-image-heap");
                    }
                }

                /*
                 * Any parameters following this call are forced over the user provided parameters in
                 * quarkus.native.additional-build-args. So if you need a parameter to be overridable through
                 * quarkus.native.additional-build-args please make sure to add it before this call.
                 */
                handleAdditionalProperties(nativeImageArgs);

                addExperimentalVMOption(nativeImageArgs, "-H:+AllowFoldMethods");

                if (nativeConfig.headless()) {
                    nativeImageArgs.add("-J-Djava.awt.headless=true");
                }

                if (nativeConfig.enableFallbackImages()) {
                    nativeImageArgs.add("--auto-fallback");
                } else {
                    //Default: be strict as those fallback images aren't very useful
                    //and tend to cover up real problems.
                    nativeImageArgs.add("--no-fallback");
                }

                if (!classpathIsBroken) {
                    nativeImageArgs.add("--link-at-build-time");
                }

                if (nativeConfig.reportErrorsAtRuntime()) {
                    nativeImageArgs.add("--report-unsupported-elements-at-runtime");
                }
                if (nativeConfig.reportExceptionStackTraces()) {
                    addExperimentalVMOption(nativeImageArgs, "-H:+ReportExceptionStackTraces");
                }
                if (nativeConfig.debug().enabled()) {
                    nativeImageArgs.add("-g");
                    addExperimentalVMOption(nativeImageArgs, "-H:DebugInfoSourceSearchPath=" + APP_SOURCES);
                }
                if (nativeConfig.debugBuildProcess()) {
                    String debugBuildProcessHost;
                    if (containerBuild) {
                        debugBuildProcessHost = "0.0.0.0";
                    } else {
                        debugBuildProcessHost = "localhost";
                    }
                    nativeImageArgs
                            .add("-J-Xrunjdwp:transport=dt_socket,address=" + debugBuildProcessHost + ":"
                                    + DEBUG_BUILD_PROCESS_PORT + ",server=y,suspend=y");
                }
                if (nativeConfig.dumpProxies()) {
                    nativeImageArgs.add("-Dsun.misc.ProxyGenerator.saveGeneratedFiles=true");
                }
                if (nativeConfig.nativeImageXmx().isPresent()) {
                    nativeImageArgs.add("-J-Xmx" + nativeConfig.nativeImageXmx().get());
                }
                List<String> protocols = new ArrayList<>(2);
                if (nativeConfig.enableHttpUrlHandler()) {
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
                    addExperimentalVMOption(nativeImageArgs, "-H:-InlineBeforeAnalysis");
                }
                if (!pie.isEmpty()) {
                    nativeImageArgs.add("-H:NativeLinkerOption=" + pie);
                }

                if (!nativeConfig.enableIsolates()) {
                    addExperimentalVMOption(nativeImageArgs, "-H:-SpawnIsolates");
                }
                if (!nativeConfig.enableJni()) {
                    log.warn(
                            "Your application is setting the deprecated 'quarkus.native.enable-jni' configuration key to false."
                                    + " Please consider removing this configuration key as it is ignored (JNI is always enabled) and it"
                                    + " will be removed in a future Quarkus version.");
                }
                if (nativeConfig.enableServer()) {
                    log.warn(
                            "Your application is setting the deprecated 'quarkus.native.enable-server' configuration key to true."
                                    + " Please consider removing this configuration key as it is ignored"
                                    + " (The Native image build server is always disabled) and it"
                                    + " will be removed in a future Quarkus version.");
                }
                if (nativeConfig.enableVmInspection()) {
                    addExperimentalVMOption(nativeImageArgs, "-H:+AllowVMInspection");
                }
                if (nativeConfig.march().isPresent()) {
                    nativeImageArgs.add("-march=" + nativeConfig.march().get());
                }

                List<NativeConfig.MonitoringOption> monitoringOptions = new ArrayList<>();
                monitoringOptions.add(NativeConfig.MonitoringOption.HEAPDUMP);
                if (nativeConfig.monitoring().isPresent()) {
                    monitoringOptions.addAll(nativeConfig.monitoring().get());
                }
                nativeImageArgs.add("--enable-monitoring=" + monitoringOptions.stream()
                        .distinct()
                        .map(o -> o.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(",")));

                if (nativeConfig.autoServiceLoaderRegistration()) {
                    addExperimentalVMOption(nativeImageArgs, "-H:+UseServiceLoaderFeature");
                    if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_1_0) < 0) {
                        // When enabling, at least print what exactly is being added. Only possible in <23.1.0
                        nativeImageArgs.add("-H:+TraceServiceLoaderFeature");
                    }
                } else {
                    addExperimentalVMOption(nativeImageArgs, "-H:-UseServiceLoaderFeature");
                }
                // This option has no effect on GraalVM 23.1+
                if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_1_0) < 0) {
                    if (nativeConfig.fullStackTraces()) {
                        nativeImageArgs.add("-H:+StackTrace");
                    } else {
                        nativeImageArgs.add("-H:-StackTrace");
                    }
                }

                if (nativeConfig.enableDashboardDump()) {
                    addExperimentalVMOption(nativeImageArgs,
                            "-H:DashboardDump=" + outputTargetBuildItem.getBaseName() + "_dashboard.dump");
                    addExperimentalVMOption(nativeImageArgs, "-H:+DashboardAll");
                }

                if (nativeImageSecurityProviders != null && !nativeImageSecurityProviders.isEmpty()) {
                    String additionalSecurityProviders = nativeImageSecurityProviders.stream()
                            .map(p -> p.getSecurityProvider())
                            .collect(Collectors.joining(","));
                    addExperimentalVMOption(nativeImageArgs, "-H:AdditionalSecurityProviders=" + additionalSecurityProviders);
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
                    nativeMinimalJavaVersions.stream()
                            .filter(a -> !graalVMVersion.jdkVersionGreaterOrEqualTo(a))
                            .forEach(a -> log.warnf("Expected: Java %s, Actual: Java %s. %s",
                                    a.minVersion, graalVMVersion.javaVersion, a.warning));
                }

                if (unsupportedOSes != null && !unsupportedOSes.isEmpty()) {
                    final String errs = unsupportedOSes.stream()
                            .filter(o -> o.triggerError(containerBuild))
                            .map(o -> o.error)
                            .collect(Collectors.joining(", "));
                    if (!errs.isEmpty()) {
                        throw new UnsupportedOperationException(errs);
                    }
                }

                nativeImageAgentConfigDirectory
                        .ifPresent(dir -> nativeImageArgs.add("-H:ConfigurationFileDirectories=" + dir.getDirectory()));

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
                if (nativeConfig.additionalBuildArgs().isPresent()) {
                    List<String> strings = nativeConfig.additionalBuildArgs().get();
                    for (String buildArg : strings) {
                        String trimmedBuildArg = buildArg.trim();
                        if (trimmedBuildArg.contains(TRUST_STORE_SYSTEM_PROPERTY_MARKER) && containerBuild) {
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
                                            + "' to volume root directory '" + outputDir.toAbsolutePath() + "'", e);
                                }
                            }
                        } else {
                            command.add(trimmedBuildArg);
                        }
                    }
                }
            }

            private void addExperimentalVMOption(List<String> nativeImageArgs, String option) {
                if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_1_0) >= 0) {
                    nativeImageArgs.add("-H:+UnlockExperimentalVMOptions");
                }
                nativeImageArgs.add(option);
                if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_1_0) >= 0) {
                    nativeImageArgs.add("-H:-UnlockExperimentalVMOptions");
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
