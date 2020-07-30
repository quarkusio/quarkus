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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.GlobUtil;
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

    @BuildStep(onlyIf = NativeBuild.class)
    ArtifactResultBuildItem result(NativeImageBuildItem image) {
        return new ArtifactResultBuildItem(image.getPath(), PackageConfig.NATIVE, Collections.emptyMap());
    }

    @BuildStep
    public NativeImageBuildItem build(NativeConfig nativeConfig, NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            PackageConfig packageConfig,
            List<NativeImageSystemPropertyBuildItem> nativeImageProperties,
            final Optional<ProcessInheritIODisabled> processInheritIODisabled) {
        Path runnerJar = nativeImageSourceJarBuildItem.getPath();
        log.info("Building native image from " + runnerJar);
        Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();

        final String runnerJarName = runnerJar.getFileName().toString();

        HashMap<String, String> env = new HashMap<>(System.getenv());
        List<String> nativeImage;

        String noPIE = "";

        boolean isContainerBuild = nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild;
        if (isContainerBuild) {
            String containerRuntime = nativeConfig.containerRuntime.orElse("docker");
            // E.g. "/usr/bin/docker run -v {{PROJECT_DIR}}:/project --rm quarkus/graalvm-native-image"
            nativeImage = new ArrayList<>();

            String outputPath = outputDir.toAbsolutePath().toString();
            if (SystemUtils.IS_OS_WINDOWS) {
                outputPath = FileUtil.translateToVolumePath(outputPath);
            }
            Collections.addAll(nativeImage, containerRuntime, "run", "-v",
                    outputPath + ":" + CONTAINER_BUILD_VOLUME_PATH + ":z", "--env", "LANG=C");

            if (SystemUtils.IS_OS_LINUX) {
                if ("docker".equals(containerRuntime)) {
                    String uid = getLinuxID("-ur");
                    String gid = getLinuxID("-gr");
                    if (uid != null && gid != null && !"".equals(uid) && !"".equals(gid)) {
                        Collections.addAll(nativeImage, "--user", uid + ":" + gid);
                    }
                } else if ("podman".equals(containerRuntime)) {
                    // Needed to avoid AccessDeniedExceptions
                    nativeImage.add("--userns=keep-id");
                }
            }
            nativeConfig.containerRuntimeOptions.ifPresent(nativeImage::addAll);
            if (nativeConfig.debugBuildProcess && nativeConfig.publishDebugBuildProcessPort) {
                // publish the debug port onto the host if asked for
                nativeImage.add("--publish=" + DEBUG_BUILD_PROCESS_PORT + ":" + DEBUG_BUILD_PROCESS_PORT);
            }
            Collections.addAll(nativeImage, "--rm", nativeConfig.builderImage);

            if ("docker".equals(containerRuntime) || "podman".equals(containerRuntime)) {
                // we pull the docker image in order to give users an indication of which step the process is at
                // it's not strictly necessary we do this, however if we don't the subsequent version command
                // will appear to block and no output will be shown
                log.info("Checking image status " + nativeConfig.builderImage);
                Process pullProcess = null;
                try {
                    final ProcessBuilder pb = new ProcessBuilder(
                            Arrays.asList(containerRuntime, "pull", nativeConfig.builderImage));
                    pullProcess = ProcessUtil.launchProcess(pb, processInheritIODisabled);
                    pullProcess.waitFor();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("Failed to pull builder image " + nativeConfig.builderImage, e);
                } finally {
                    if (pullProcess != null) {
                        pullProcess.destroy();
                    }
                }
            }

        } else {
            if (SystemUtils.IS_OS_LINUX) {
                noPIE = detectNoPIE();
            }

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
            nativeImage = Collections.singletonList(getNativeImageExecutable(graal, java, env).getAbsolutePath());
        }

        final Optional<String> graalVMVersion;

        try {
            List<String> versionCommand = new ArrayList<>(nativeImage);
            versionCommand.add("--version");

            Process versionProcess = new ProcessBuilder(versionCommand.toArray(new String[0]))
                    .redirectErrorStream(true)
                    .start();
            versionProcess.waitFor();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(versionProcess.getInputStream(), StandardCharsets.UTF_8))) {
                graalVMVersion = reader.lines().filter((l) -> l.startsWith("GraalVM Version")).findFirst();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get GraalVM version", e);
        }

        boolean isMandrel = false;
        if (graalVMVersion.isPresent()) {
            checkGraalVMVersion(graalVMVersion.get());
            isMandrel = graalVMVersion.get().contains("Mandrel");
        } else {
            log.error("Unable to get GraalVM version from the native-image binary.");
        }

        try {
            List<String> command = new ArrayList<>(nativeImage);
            if (nativeConfig.cleanupServer) {
                List<String> cleanup = new ArrayList<>(nativeImage);
                cleanup.add("--server-shutdown");
                final ProcessBuilder pb = new ProcessBuilder(cleanup.toArray(new String[0]));
                pb.directory(outputDir.toFile());
                final Process process = ProcessUtil.launchProcess(pb, processInheritIODisabled);
                process.waitFor();
            }
            Boolean enableSslNative = false;
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
            nativeConfig.resources.includes.ifPresent(l -> l.stream()
                    .map(GlobUtil::toRegexPattern)
                    .map(re -> "-H:IncludeResources=" + re.trim())
                    .forEach(command::add));
            command.add("--initialize-at-build-time=");
            command.add("-H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy$BySpaceAndTime"); //the default collection policy results in full GC's 50% of the time
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
                command.add("-H:GenerateDebugInfo=1");
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
            if (!nativeConfig.enableServer && !SystemUtils.IS_OS_WINDOWS && !isMandrel) {
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

            log.info(String.join(" ", command));
            CountDownLatch errorReportLatch = new CountDownLatch(1);
            final ProcessBuilder processBuilder = new ProcessBuilder(command).directory(outputDir.toFile());
            final Process process = ProcessUtil.launchProcessStreamStdOut(processBuilder, processInheritIODisabled);
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
            System.setProperty("native.image.path", finalPath.toAbsolutePath().toString());

            if (objcopyExists(env)) {
                if (nativeConfig.debug.enabled) {
                    splitDebugSymbols(finalPath);
                }
                // Strip debug symbols regardless, because the underlying JDK might contain them
                objcopy("--strip-debug", finalPath.toString());
            }

            return new NativeImageBuildItem(finalPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build native image", e);
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

    private void checkGraalVMVersion(String version) {
        log.info("Running Quarkus native-image plugin on " + version);
        final List<String> obsoleteGraalVmVersions = Arrays.asList("1.0.0", "19.0.", "19.1.", "19.2.", "19.3.", "20.0.");
        final boolean vmVersionIsObsolete = obsoleteGraalVmVersions.stream().anyMatch(v -> version.contains(" " + v));
        if (vmVersionIsObsolete) {
            throw new IllegalStateException("Out of date version of GraalVM detected: " + version + "."
                    + " Quarkus currently supports 20.1.0. Please upgrade GraalVM to this version.");
        }
    }

    private static File getNativeImageExecutable(Optional<String> graalVmHome, File javaHome, Map<String, String> env) {
        String imageName = SystemUtils.IS_OS_WINDOWS ? "native-image.cmd" : "native-image";
        if (graalVmHome.isPresent()) {
            File file = Paths.get(graalVmHome.get(), "bin", imageName).toFile();
            if (file.exists()) {
                return file;
            }
        }

        if (javaHome != null) {
            File file = new File(javaHome, "bin/" + imageName);
            if (file.exists()) {
                return file;
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
                        return file;
                    }
                }
            }
        }

        throw new RuntimeException("Cannot find the `" + imageName + "` in the GRAALVM_HOME, JAVA_HOME and System " +
                "PATH. Install it using `gu install native-image`");

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
            if (intr)
                Thread.currentThread().interrupt();
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

        log.debug("Cannot find executable (objcopy) to separate symbols from executable.");
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
        log.infof("Execute %s", command);
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
}
