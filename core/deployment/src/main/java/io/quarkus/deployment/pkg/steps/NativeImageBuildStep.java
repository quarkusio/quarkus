package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.GlobUtil;

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

    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

    /**
     * The name of the environment variable containing the system path.
     */
    private static final String PATH = "PATH";

    private static final int OOM_ERROR_VALUE = 137;
    private static final String QUARKUS_XMX_PROPERTY = "quarkus.native.native-image-xmx";

    @BuildStep(onlyIf = NativeBuild.class)
    ArtifactResultBuildItem result(NativeImageBuildItem image) {
        return new ArtifactResultBuildItem(image.getPath(), PackageConfig.NATIVE, Collections.emptyMap());
    }

    @BuildStep
    public NativeImageBuildItem build(NativeConfig nativeConfig, NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            PackageConfig packageConfig,
            List<NativeImageSystemPropertyBuildItem> nativeImageProperties) {
        Path runnerJar = nativeImageSourceJarBuildItem.getPath();
        log.info("Building native image from " + runnerJar);
        Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();

        final String runnerJarName = runnerJar.getFileName().toString();

        HashMap<String, String> env = new HashMap<>(System.getenv());
        List<String> nativeImage;

        String noPIE = "";

        if (nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild) {
            String containerRuntime = nativeConfig.containerRuntime.orElse("docker");
            // E.g. "/usr/bin/docker run -v {{PROJECT_DIR}}:/project --rm quarkus/graalvm-native-image"
            nativeImage = new ArrayList<>();

            String outputPath = outputDir.toAbsolutePath().toString();
            if (IS_WINDOWS) {
                outputPath = FileUtil.translateToVolumePath(outputPath);
            }
            Collections.addAll(nativeImage, containerRuntime, "run", "-v", outputPath + ":/project:z", "--env", "LANG=C");

            if (IS_LINUX) {
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
                log.info("Pulling image " + nativeConfig.builderImage);
                Process pullProcess = null;
                try {
                    pullProcess = new ProcessBuilder(Arrays.asList(containerRuntime, "pull", nativeConfig.builderImage))
                            .inheritIO()
                            .start();
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
            if (IS_LINUX) {
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
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(versionProcess.getInputStream(), StandardCharsets.UTF_8))) {
                graalVMVersion = reader.lines().filter((l) -> l.startsWith("GraalVM Version")).findFirst();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get GraalVM version", e);
        }

        if (graalVMVersion.isPresent()) {
            checkGraalVMVersion(graalVMVersion.get());
        } else {
            log.error("Unable to get GraalVM version from the native-image binary.");
        }

        try {
            List<String> command = new ArrayList<>(nativeImage);
            if (nativeConfig.cleanupServer) {
                List<String> cleanup = new ArrayList<>(nativeImage);
                cleanup.add("--server-shutdown");
                ProcessBuilder pb = new ProcessBuilder(cleanup.toArray(new String[0]));
                pb.directory(outputDir.toFile());
                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process process = pb.start();
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
                } else if (prop.getKey().equals("quarkus.native.enable-all-timezones") && prop.getValue() != null) {
                    nativeConfig.includeAllTimeZones |= Boolean.parseBoolean(prop.getValue());
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
            command.add("-J-Dfile.encoding=" + System.getProperty("file.encoding"));

            if (enableSslNative) {
                nativeConfig.enableHttpsUrlHandler = true;
                nativeConfig.enableAllSecurityServices = true;
            }

            nativeConfig.additionalBuildArgs.ifPresent(l -> l.stream().map(String::trim).forEach(command::add));
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
            if (nativeConfig.debugSymbols) {
                command.add("-g");
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
            if (nativeConfig.includeAllTimeZones) {
                command.add("-H:+IncludeAllTimeZones");
            } else {
                command.add("-H:-IncludeAllTimeZones");
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
            if (!nativeConfig.enableServer && !IS_WINDOWS) {
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

            Process process = new ProcessBuilder(command)
                    .directory(outputDir.toFile())
                    .inheritIO()
                    .start();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(new ErrorReplacingProcessReader(process.getErrorStream(), outputDir.resolve("reports").toFile(),
                    errorReportLatch));
            executor.shutdown();
            errorReportLatch.await();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw imageGenerationFailed(exitCode, command);
            }
            if (IS_WINDOWS && !(nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild)) {
                //once image is generated it gets added .exe on Windows
                executableName = executableName + ".exe";
            }
            Path generatedImage = outputDir.resolve(executableName);
            Path finalPath = outputTargetBuildItem.getOutputDirectory().resolve(executableName);
            IoUtils.copy(generatedImage, finalPath);
            Files.delete(generatedImage);
            System.setProperty("native.image.path", finalPath.toAbsolutePath().toString());

            return new NativeImageBuildItem(finalPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build native image", e);
        }
    }

    private RuntimeException imageGenerationFailed(int exitValue, List<String> command) {
        if (exitValue == OOM_ERROR_VALUE) {
            if (command.contains("docker") && !IS_LINUX) {
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
        final List<String> obsoleteGraalVmVersions = Arrays.asList("1.0.0", "19.0.", "19.1.", "19.2.", "19.3.0");
        final boolean vmVersionIsObsolete = obsoleteGraalVmVersions.stream().anyMatch(v -> version.contains(" " + v));
        if (vmVersionIsObsolete) {
            throw new IllegalStateException("Out of date version of GraalVM detected: " + version + "."
                    + " Quarkus currently supports GraalVM 19.3.1 and 20.0.0. Please upgrade GraalVM to one of these versions.");
        }
    }

    private static File getNativeImageExecutable(Optional<String> graalVmHome, File javaHome, Map<String, String> env) {
        String imageName = IS_WINDOWS ? "native-image.cmd" : "native-image";
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

}
