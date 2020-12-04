package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
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
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.NativeConfig.ContainerRuntime;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.ProcessUtil;

public class NativeImageBuildStep {

    private static final Logger log = Logger.getLogger(NativeImageBuildStep.class);
    private static final String DEBUG_BUILD_PROCESS_PORT = "5005";
    private static final String GRAALVM_HOME = "GRAALVM_HOME";

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
    private static final String CONTAINER_BUILD_VOLUME_PATH = "/project";
    private static final String TRUST_STORE_SYSTEM_PROPERTY_MARKER = "-Djavax.net.ssl.trustStore=";
    private static final String MOVED_TRUST_STORE_NAME = "trustStore";
    public static final String APP_SOURCES = "app-sources";

    @BuildStep(onlyIf = NativeBuild.class)
    ArtifactResultBuildItem result(NativeImageBuildItem image) {
        return new ArtifactResultBuildItem(image.getPath(), PackageConfig.NATIVE, Collections.emptyMap());
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

        HashMap<String, String> env = new HashMap<>(System.getenv());
        List<String> nativeImage;

        String noPIE = "";

        boolean isContainerBuild = nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild;
        if (!isContainerBuild && SystemUtils.IS_OS_LINUX) {
            noPIE = detectNoPIE();
        }

        nativeImage = getNativeImage(nativeConfig, processInheritIODisabled, outputDir, env);
        final GraalVM.Version graalVMVersion = GraalVM.Version.ofBinary(nativeImage);

        if (graalVMVersion.isDetected()) {
            checkGraalVMVersion(graalVMVersion);
        } else {
            log.error("Unable to get GraalVM version from the native-image binary.");
        }

        try {
            List<String> command = new ArrayList<>(nativeImage);
            if (nativeConfig.cleanupServer && !graalVMVersion.isMandrel()) {
                List<String> cleanup = new ArrayList<>(nativeImage);
                cleanup.add("--server-shutdown");
                final ProcessBuilder pb = new ProcessBuilder(cleanup.toArray(new String[0]));
                pb.directory(outputDir.toFile());
                final Process process = ProcessUtil.launchProcess(pb, processInheritIODisabled.isPresent());
                process.waitFor();
            }
            boolean enableSslNative = false;
            for (NativeImageSystemPropertyBuildItem prop : nativeImageProperties) {
                //todo: this should be specific build items
                if (prop.getKey().equals("quarkus.ssl.native") && prop.getValue() != null) {
                    enableSslNative = Boolean.parseBoolean(prop.getValue());
                } else if (prop.getKey().equals("quarkus.jni.enable") && prop.getValue().equals("false")) {
                    log.warn("Your application is setting the deprecated 'quarkus.jni.enable' configuration key to false."
                            + " Please consider removing this configuration key as it is ignored (JNI is always enabled) and it"
                            + " will be removed in a future Quarkus version.");
                } else if (prop.getKey().equals("quarkus.native.enable-all-security-services") && prop.getValue() != null) {
                    nativeConfig.enableAllSecurityServices |= Boolean.parseBoolean(prop.getValue());
                } else if (prop.getKey().equals("quarkus.native.enable-all-charsets") && prop.getValue() != null) {
                    nativeConfig.addAllCharsets |= Boolean.parseBoolean(prop.getValue());
                } else {
                    // todo maybe just -D is better than -J-D in this case
                    if (prop.getValue() == null) {
                        command.add("-J-D" + prop.getKey());
                    } else {
                        command.add("-J-D" + prop.getKey() + "=" + prop.getValue());
                    }
                }
            }
            command.add("-J-Duser.language=" + System.getProperty("user.language"));
            // Native image runtime uses the host's (i.e. build time) value of file.encoding
            // system property. We intentionally default this to UTF-8 to avoid platform specific
            // defaults to be picked up which can then result in inconsistent behaviour in the
            // generated native application
            command.add("-J-Dfile.encoding=UTF-8");

            if (enableSslNative) {
                nativeConfig.enableHttpsUrlHandler = true;
                nativeConfig.enableAllSecurityServices = true;
            }

            handleAdditionalProperties(nativeConfig, command, isContainerBuild, outputDir);
            command.add("--initialize-at-build-time=");
            command.add(
                    "-H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy$BySpaceAndTime"); //the default collection policy results in full GC's 50% of the time
            command.add("-H:+JNI");
            command.add("-jar");
            command.add(runnerJarName);

            if (nativeConfig.enableFallbackImages) {
                command.add("-H:FallbackThreshold=5");
            } else {
                //Default: be strict as those fallback images aren't very useful
                //and tend to cover up real problems.
                command.add("-H:FallbackThreshold=0");
            }

            if (nativeConfig.reportErrorsAtRuntime) {
                command.add("-H:+ReportUnsupportedElementsAtRuntime");
            }
            if (nativeConfig.reportExceptionStackTraces) {
                command.add("-H:+ReportExceptionStackTraces");
            }
            if (nativeConfig.debug.enabled) {
                if (graalVMVersion.isMandrel() || graalVMVersion.isNewerThan(GraalVM.Version.VERSION_20_1)) {
                    command.add("-g");
                    command.add("-H:DebugInfoSourceSearchPath=" + APP_SOURCES);
                }
            }
            if (nativeConfig.debugBuildProcess) {
                command.add("-J-Xrunjdwp:transport=dt_socket,address=" + DEBUG_BUILD_PROCESS_PORT + ",server=y,suspend=y");
            }
            if (nativeConfig.enableReports) {
                command.add("-H:+PrintAnalysisCallTree");
            }
            if (nativeConfig.dumpProxies) {
                command.add("-Dsun.misc.ProxyGenerator.saveGeneratedFiles=true");
                if (nativeConfig.enableServer) {
                    log.warn(
                            "Options dumpProxies and enableServer are both enabled: this will get the proxies dumped in an unknown external working directory");
                }
            }
            if (nativeConfig.nativeImageXmx.isPresent()) {
                command.add("-J-Xmx" + nativeConfig.nativeImageXmx.get());
            }
            List<String> protocols = new ArrayList<>(2);
            if (nativeConfig.enableHttpUrlHandler) {
                protocols.add("http");
            }
            if (nativeConfig.enableHttpsUrlHandler) {
                protocols.add("https");
            }
            if (nativeConfig.addAllCharsets) {
                command.add("-H:+AddAllCharsets");
            } else {
                command.add("-H:-AddAllCharsets");
            }
            if (!protocols.isEmpty()) {
                command.add("-H:EnableURLProtocols=" + String.join(",", protocols));
            }
            if (nativeConfig.enableAllSecurityServices) {
                command.add("--enable-all-security-services");
            }
            if (!noPIE.isEmpty()) {
                command.add("-H:NativeLinkerOption=" + noPIE);
            }

            if (!nativeConfig.enableIsolates) {
                command.add("-H:-SpawnIsolates");
            }
            if (!nativeConfig.enableJni) {
                log.warn("Your application is setting the deprecated 'quarkus.native.enable-jni' configuration key to false."
                        + " Please consider removing this configuration key as it is ignored (JNI is always enabled) and it"
                        + " will be removed in a future Quarkus version.");
            }
            if (!nativeConfig.enableServer && !SystemUtils.IS_OS_WINDOWS && !graalVMVersion.isMandrel()) {
                command.add("--no-server");
            }
            if (nativeConfig.enableVmInspection) {
                command.add("-H:+AllowVMInspection");
            }
            if (nativeConfig.autoServiceLoaderRegistration) {
                command.add("-H:+UseServiceLoaderFeature");
                //When enabling, at least print what exactly is being added:
                command.add("-H:+TraceServiceLoaderFeature");
            } else {
                command.add("-H:-UseServiceLoaderFeature");
            }
            if (nativeConfig.fullStackTraces) {
                command.add("-H:+StackTrace");
            } else {
                command.add("-H:-StackTrace");
            }
            String executableName = outputTargetBuildItem.getBaseName() + packageConfig.runnerSuffix;
            command.add(executableName);

            log.info(String.join(" ", command).replace("$", "\\$"));
            CountDownLatch errorReportLatch = new CountDownLatch(1);
            final ProcessBuilder processBuilder = new ProcessBuilder(command).directory(outputDir.toFile());
            final Process process = ProcessUtil.launchProcessStreamStdOut(processBuilder, processInheritIODisabled.isPresent());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(new ErrorReplacingProcessReader(process.getErrorStream(), outputDir.resolve("reports").toFile(),
                    errorReportLatch));
            executor.shutdown();
            errorReportLatch.await();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw imageGenerationFailed(exitCode, command);
            }
            if (SystemUtils.IS_OS_WINDOWS && !(isContainerBuild)) {
                //once image is generated it gets added .exe on Windows
                executableName = executableName + ".exe";
            }
            Path generatedImage = outputDir.resolve(executableName);
            Path finalPath = outputTargetBuildItem.getOutputDirectory().resolve(executableName);
            IoUtils.copy(generatedImage, finalPath);
            Files.delete(generatedImage);
            if (nativeConfig.debug.enabled) {
                if (graalVMVersion.isMandrel() || graalVMVersion.isNewerThan(GraalVM.Version.VERSION_20_1)) {
                    final String sources = "sources";
                    final Path generatedSources = outputDir.resolve(sources);
                    final Path finalSources = outputTargetBuildItem.getOutputDirectory().resolve(sources);
                    IoUtils.copy(generatedSources, finalSources);
                    IoUtils.recursiveDelete(generatedSources);
                }
            }
            System.setProperty("native.image.path", finalPath.toAbsolutePath().toString());

            if (objcopyExists(env)) {
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

    private static List<String> getNativeImage(NativeConfig nativeConfig,
            Optional<ProcessInheritIODisabled> processInheritIODisabled,
            Path outputDir, Map<String, String> env) {
        boolean isContainerBuild = nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild;
        if (isContainerBuild) {
            return setupContainerBuild(nativeConfig, processInheritIODisabled, outputDir);
        } else {
            Optional<String> graal = nativeConfig.graalvmHome;
            File java = nativeConfig.javaHome;
            if (graal.isPresent()) {
                env.put(GRAALVM_HOME, graal.get());
            }
            if (java == null) {
                // try system property first - it will be the JAVA_HOME used by the current JVM
                String home = System.getProperty(JAVA_HOME_SYS);
                if (home == null) {
                    // No luck, somewhat a odd JVM not enforcing this property
                    // try with the JAVA_HOME environment variable
                    home = env.get(JAVA_HOME_ENV);
                }

                if (home != null) {
                    java = new File(home);
                }
            }
            return getNativeImageExecutable(graal, java, env, nativeConfig, processInheritIODisabled, outputDir);
        }
    }

    public static List<String> setupContainerBuild(NativeConfig nativeConfig,
            Optional<ProcessInheritIODisabled> processInheritIODisabled, Path outputDir) {
        List<String> nativeImage;
        final ContainerRuntime containerRuntime = nativeConfig.containerRuntime
                .orElseGet(NativeImageBuildStep::detectContainerRuntime);
        log.infof("Using %s to run the native image builder", containerRuntime.getExecutableName());
        // E.g. "/usr/bin/docker run -v {{PROJECT_DIR}}:/project --rm quarkus/graalvm-native-image"
        nativeImage = new ArrayList<>();

        String outputPath = outputDir.toAbsolutePath().toString();
        if (SystemUtils.IS_OS_WINDOWS) {
            outputPath = FileUtil.translateToVolumePath(outputPath);
        }
        Collections.addAll(nativeImage, containerRuntime.getExecutableName(), "run", "-v",
                outputPath + ":" + CONTAINER_BUILD_VOLUME_PATH + ":z", "--env", "LANG=C");

        if (SystemUtils.IS_OS_LINUX) {
            String uid = getLinuxID("-ur");
            String gid = getLinuxID("-gr");
            if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                Collections.addAll(nativeImage, "--user", uid + ":" + gid);
                if (containerRuntime == ContainerRuntime.PODMAN) {
                    // Needed to avoid AccessDeniedExceptions
                    nativeImage.add("--userns=keep-id");
                }
            }
        }
        nativeConfig.containerRuntimeOptions.ifPresent(nativeImage::addAll);
        if (nativeConfig.debugBuildProcess && nativeConfig.publishDebugBuildProcessPort) {
            // publish the debug port onto the host if asked for
            nativeImage.add("--publish=" + DEBUG_BUILD_PROCESS_PORT + ":" + DEBUG_BUILD_PROCESS_PORT);
        }
        Collections.addAll(nativeImage, "--rm", nativeConfig.builderImage);

        if (containerRuntime == ContainerRuntime.DOCKER || containerRuntime == ContainerRuntime.PODMAN) {
            // we pull the docker image in order to give users an indication of which step the process is at
            // it's not strictly necessary we do this, however if we don't the subsequent version command
            // will appear to block and no output will be shown
            log.info("Checking image status " + nativeConfig.builderImage);
            Process pullProcess = null;
            try {
                final ProcessBuilder pb = new ProcessBuilder(
                        Arrays.asList(containerRuntime.getExecutableName(), "pull", nativeConfig.builderImage));
                pullProcess = ProcessUtil.launchProcess(pb, processInheritIODisabled.isPresent());
                pullProcess.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to pull builder image " + nativeConfig.builderImage, e);
            } finally {
                if (pullProcess != null) {
                    pullProcess.destroy();
                }
            }
        }
        return nativeImage;
    }

    /**
     * @return {@link ContainerRuntime#DOCKER} if it's available, or {@link ContainerRuntime#PODMAN} if the podman
     *         executable exists in the environment or if the docker executable is an alias to podman
     * @throws IllegalStateException if no container runtime was found to build the image
     */
    private static ContainerRuntime detectContainerRuntime() {
        // Docker version 19.03.14, build 5eb3275d40
        String dockerVersionOutput = getVersionOutputFor(ContainerRuntime.DOCKER);
        boolean dockerAvailable = dockerVersionOutput.contains("Docker version");
        // Check if Podman is installed
        // podman version 2.1.1
        String podmanVersionOutput = getVersionOutputFor(ContainerRuntime.PODMAN);
        boolean podmanAvailable = podmanVersionOutput.startsWith("podman version");
        if (dockerAvailable) {
            // Check if "docker" is an alias to "podman"
            if (dockerVersionOutput.equals(podmanVersionOutput)) {
                return ContainerRuntime.PODMAN;
            }
            return ContainerRuntime.DOCKER;
        } else if (podmanAvailable) {
            return ContainerRuntime.PODMAN;
        } else {
            throw new IllegalStateException("No container runtime was found to run the native image builder");
        }
    }

    private static String getVersionOutputFor(ContainerRuntime containerRuntime) {
        Process versionProcess = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(containerRuntime.getExecutableName(), "--version")
                    .redirectErrorStream(true);
            versionProcess = pb.start();
            versionProcess.waitFor();
            return new String(FileUtil.readFileContents(versionProcess.getInputStream()), StandardCharsets.UTF_8);
        } catch (IOException | InterruptedException e) {
            // If an exception is thrown in the process, just return an empty String
            log.debugf(e, "Failure to read version output from %s", containerRuntime.getExecutableName());
            return "";
        } finally {
            if (versionProcess != null) {
                versionProcess.destroy();
            }
        }
    }

    private void copyJarSourcesToLib(OutputTargetBuildItem outputTargetBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        Path targetDirectory = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + "-native-image-source-jar");
        Path libDir = targetDirectory.resolve(JarResultBuildStep.LIB);

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

        try {
            Files.walk(javaSourcesPath).forEach(path -> {
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
                            IoUtils.copy(Paths.get(configuredTrustStorePath), outputDir.resolve(MOVED_TRUST_STORE_NAME));
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

    private static List<String> getNativeImageExecutable(Optional<String> graalVmHome, File javaHome, Map<String, String> env,
            NativeConfig nativeConfig, Optional<ProcessInheritIODisabled> processInheritIODisabled, Path outputDir) {
        String imageName = SystemUtils.IS_OS_WINDOWS ? "native-image.cmd" : "native-image";
        if (graalVmHome.isPresent()) {
            File file = Paths.get(graalVmHome.get(), "bin", imageName).toFile();
            if (file.exists()) {
                return Collections.singletonList(file.getAbsolutePath());
            }
        }

        if (javaHome != null) {
            File file = new File(javaHome, "bin/" + imageName);
            if (file.exists()) {
                return Collections.singletonList(file.getAbsolutePath());
            }
        }

        // System path
        String systemPath = env.get(PATH);
        if (systemPath != null) {
            String[] pathDirs = systemPath.split(File.pathSeparator);
            for (String pathDir : pathDirs) {
                File dir = new File(pathDir);
                if (dir.isDirectory()) {
                    File file = new File(dir, imageName);
                    if (file.exists()) {
                        return Collections.singletonList(file.getAbsolutePath());
                    }
                }
            }
        }

        if (SystemUtils.IS_OS_LINUX) {
            log.warn("Cannot find the `" + imageName + "` in the GRAALVM_HOME, JAVA_HOME and System " +
                    "PATH. Install it using `gu install native-image`. Attempting to fall back to docker.");
            return setupContainerBuild(nativeConfig, processInheritIODisabled, outputDir);
        } else {
            throw new RuntimeException("Cannot find the `" + imageName + "` in the GRAALVM_HOME, JAVA_HOME and System " +
                    "PATH. Install it using `gu install native-image`");
        }
    }

    private static String getLinuxID(String option) {
        Process process;

        try {
            StringBuilder responseBuilder = new StringBuilder();
            String line;

            ProcessBuilder idPB = new ProcessBuilder().command("id", option);
            idPB.redirectError(new File("/dev/null"));
            idPB.redirectInput(new File("/dev/null"));

            process = idPB.start();
            try (InputStream inputStream = process.getInputStream()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    safeWaitFor(process);
                    return responseBuilder.toString();
                }
            } catch (Throwable t) {
                safeWaitFor(process);
                throw t;
            }
        } catch (IOException e) { //from process.start()
            //swallow and return null id
            return null;
        }
    }

    static void safeWaitFor(Process process) {
        boolean intr = false;
        try {
            for (;;)
                try {
                    process.waitFor();
                    return;
                } catch (InterruptedException ex) {
                    intr = true;
                }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
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

    private boolean objcopyExists(Map<String, String> env) {
        // System path
        String systemPath = env.get(PATH);
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

    //https://github.com/quarkusio/quarkus/issues/11573
    //https://github.com/oracle/graal/issues/1610
    List<RuntimeReinitializedClassBuildItem> graalVmWorkaround(NativeConfig nativeConfig,
            NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            Optional<ProcessInheritIODisabled> processInheritIODisabled) {
        Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();
        HashMap<String, String> env = new HashMap<>(System.getenv());
        List<String> nativeImage = getNativeImage(nativeConfig, processInheritIODisabled, outputDir, env);
        GraalVM.Version version = GraalVM.Version.ofBinary(nativeImage);
        if (version.isNewerThan(GraalVM.Version.VERSION_20_2)) {
            // https://github.com/oracle/graal/issues/2841
            return Collections.emptyList();
        }
        return Arrays.asList(new RuntimeReinitializedClassBuildItem(ThreadLocalRandom.class.getName()),
                new RuntimeReinitializedClassBuildItem("java.lang.Math$RandomNumberGeneratorHolder"));
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

            static final Version VERSION_20_1 = new Version("GraalVM 20.1", 20, 1, Distribution.ORACLE);
            static final Version VERSION_20_2 = new Version("GraalVM 20.2", 20, 2, Distribution.ORACLE);

            static final Version MINIMUM = VERSION_20_1;
            static final Version CURRENT = VERSION_20_2;

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

            private static Version ofBinary(List<String> nativeImage) {
                final Version graalVMVersion;
                try {
                    List<String> versionCommand = new ArrayList<>(nativeImage);
                    versionCommand.add("--version");

                    Process versionProcess = new ProcessBuilder(versionCommand.toArray(new String[0]))
                            .redirectErrorStream(true)
                            .start();
                    versionProcess.waitFor();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(versionProcess.getInputStream(), StandardCharsets.UTF_8))) {
                        graalVMVersion = of(reader.lines());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get GraalVM version", e);
                }
                return graalVMVersion;
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
}
