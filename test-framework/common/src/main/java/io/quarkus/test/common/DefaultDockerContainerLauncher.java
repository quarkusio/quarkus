package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.createStartedFunction;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static io.quarkus.test.common.LauncherUtil.waitForStartedFunction;
import static java.lang.ProcessBuilder.Redirect.DISCARD;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;
import io.quarkus.deployment.pkg.steps.NativeImageBuildLocalContainerRunner;
import io.quarkus.deployment.util.BoundedProcessRunner;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.common.utils.StringUtil;

/**
 * Default container-runtime implementation of {@link DockerContainerArtifactLauncher}.
 * <p>
 * Explicit startup-archive training mounts fresh host output into the container, preserves inherited
 * {@code JAVA_TOOL_OPTIONS}, and validates the produced file or directory. The launcher owns its uniquely named
 * integration-test container and uses bounded cleanup before consuming or removing mounted training intermediates.
 */
public class DefaultDockerContainerLauncher implements DockerContainerArtifactLauncher {
    private static final Logger log = Logger.getLogger(DefaultDockerContainerLauncher.class);

    private static final String AOT_DIR = "aot";
    private static final String AOT_FILE_NAME = "app.aot";
    private static final String AOT_CONF_FILE_NAME = "app.aotconf";
    private static final Duration PROCESS_FORCE_TIMEOUT = Duration.ofSeconds(5);

    private int httpPort;
    private int httpsPort;
    private long waitTimeSeconds;
    private Duration shutdownTimeout;
    private String testProfile;
    private List<String> argLine;
    private Map<String, String> env;
    private ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult;
    private String containerImage;
    private boolean pullRequired;
    private boolean generateAotFile;
    private List<String> additionalRecordingArgs;
    private Map<Integer, Integer> additionalExposedPorts;

    private Map<String, String> volumeMounts;
    private Map<String, String> labels;
    private final Map<String, String> systemProps = new HashMap<>();
    private final String containerName = "quarkus-integration-test-" + RandomStringUtils.insecure().next(5, true, false);
    private String containerRuntimeBinaryName;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Optional<String> entryPoint;
    private List<String> programArgs;
    private Optional<String> containerWorkingDirectory;
    private String outputTargetDirectory;
    private Optional<JvmStartupArchiveTraining> startupArchiveTraining;
    private Process containerProcess;

    @Override
    public void init(DockerContainerArtifactLauncher.DockerInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
        this.shutdownTimeout = initContext.shutdownTimeout();
        this.testProfile = initContext.testProfile();
        this.argLine = initContext.argLine();
        this.env = initContext.env();
        this.devServicesLaunchResult = initContext.getDevServicesLaunchResult();
        this.containerImage = initContext.containerImage();
        this.pullRequired = initContext.pullRequired();
        this.additionalExposedPorts = initContext.additionalExposedPorts();
        this.volumeMounts = initContext.volumeMounts();
        this.labels = initContext.labels();
        this.entryPoint = initContext.entryPoint();
        this.containerWorkingDirectory = initContext.containerWorkingDirectory();
        this.programArgs = initContext.programArgs();
        this.generateAotFile = initContext.generateAotFile();
        this.additionalRecordingArgs = initContext.additionalRecordingArgs();
        this.outputTargetDirectory = initContext.outputTargetDirectory();
        this.startupArchiveTraining = initContext.startupArchiveTraining();
    }

    @Override
    public LaunchResult runToCompletion(String[] argz) {
        try {
            final ContainerRuntimeUtil.ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime();
            containerRuntimeBinaryName = containerRuntime.getExecutableName();

            if (pullRequired) {
                log.infof("Pulling container image '%s'", containerImage);
                try {
                    int pullResult = new ProcessBuilder().redirectError(DISCARD).redirectOutput(DISCARD)
                            .command(containerRuntimeBinaryName, "pull", containerImage).start().waitFor();
                    if (pullResult > 0) {
                        throw new RuntimeException("Pulling container image '" + containerImage + "' completed unsuccessfully");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Unable to pull container image '" + containerImage + "'", e);
                }
            }

            final List<String> args = new ArrayList<>();
            args.add(containerRuntimeBinaryName);
            args.add("run");
            if (!argLine.isEmpty()) {
                args.addAll(argLine);
            }
            args.add("--name");
            args.add(containerName);
            args.add("-i"); // Interactive, write logs to stdout
            args.add("--rm");

            if (!volumeMounts.isEmpty() || startupArchiveTraining.isPresent()) {
                args.addAll(NativeImageBuildLocalContainerRunner.getVolumeAccessArguments(containerRuntime, containerImage));
            }

            if (httpPort != 0) {
                args.add("-p");
                args.add(httpPort + ":" + httpPort);
            }
            if (httpsPort != 0) {
                args.add("-p");
                args.add(httpsPort + ":" + httpsPort);
            }
            if (entryPoint.isPresent()) {
                args.add("--entrypoint");
                args.add(entryPoint.get());
            }
            for (Map.Entry<Integer, Integer> entry : additionalExposedPorts.entrySet()) {
                args.add("-p");
                args.add(entry.getKey() + ":" + entry.getValue());
            }
            for (Map.Entry<String, String> entry : volumeMounts.entrySet()) {
                NativeImageBuildLocalContainerRunner.addVolumeParameter(entry.getKey(), entry.getValue(), args,
                        containerRuntime);
            }

            // if the dev services resulted in creating a dedicated network, then use it
            if (devServicesLaunchResult.networkId() != null) {
                args.add("--net=" + devServicesLaunchResult.networkId());
            }

            args.addAll(toEnvVar("quarkus.log.category.\"io.quarkus\".level", "INFO"));
            if (DefaultJarLauncher.HTTP_PRESENT) {
                args.addAll(toEnvVar("quarkus.http.port", "" + httpPort));
                args.addAll(toEnvVar("quarkus.http.ssl-port", "" + httpsPort));
                args.addAll(toEnvVar("test.url", LauncherUtil.generateTestUrl()));
            }
            if (testProfile != null) {
                args.addAll(toEnvVar("quarkus.profile", testProfile));
            }

            for (var e : systemProps.entrySet()) {
                args.addAll(toEnvVar(e.getKey(), e.getValue()));
            }

            for (var e : env.entrySet()) {
                if (startupArchiveTraining.isPresent() && "JAVA_TOOL_OPTIONS".equals(e.getKey())) {
                    continue;
                }
                args.addAll(envAsLaunchArg(e.getKey(), e.getValue()));
            }

            handleAotFileArgs(args, containerRuntime);

            for (var e : labels.entrySet()) {
                args.add("--label");
                args.add(e.getKey() + "=" + e.getValue());
            }
            args.add(containerImage);
            args.addAll(programArgs);
            args.addAll(Arrays.asList(argz));

            log.infof("Executing \"%s\"", String.join(" ", args));

            final Process containerProcess = new ProcessBuilder(args).start();

            ProcessReader error = new ProcessReader(containerProcess.getErrorStream());
            ProcessReader stdout = new ProcessReader(containerProcess.getInputStream());
            Thread t1 = new Thread(error, "Error stream reader");
            t1.start();
            Thread t2 = new Thread(stdout, "Stdout stream reader");
            t2.start();
            t1.join();
            t2.join();
            byte[] s = stdout.get();
            byte[] e = error.get();
            return new LaunchResult(containerProcess.waitFor(), s, e);
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Running to completion failed.", ex);
        }
    }

    @Override
    public ListeningAddresses start() throws IOException {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        LogRuntimeConfig logRuntimeConfig = config.getConfigMapping(LogRuntimeConfig.class);

        final ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime();
        containerRuntimeBinaryName = containerRuntime.getExecutableName();

        if (pullRequired) {
            log.infof("Pulling container image '%s'", containerImage);
            try {
                int pullResult = new ProcessBuilder().redirectError(DISCARD).redirectOutput(DISCARD)
                        .command(containerRuntimeBinaryName, "pull", containerImage).start().waitFor();
                if (pullResult > 0) {
                    throw new RuntimeException("Pulling container image '" + containerImage + "' completed unsuccessfully");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Unable to pull container image '" + containerImage + "'", e);
            }
        }

        if (httpPort == 0) {
            httpPort = getRandomPort();
        }
        if (httpsPort == 0) {
            httpsPort = getRandomPort();
        }

        final List<String> args = new ArrayList<>();
        args.add(containerRuntimeBinaryName);
        args.add("run");
        if (!argLine.isEmpty()) {
            args.addAll(argLine);
        }
        args.add("--name");
        args.add(containerName);
        args.add("-i"); // Interactive, write logs to stdout
        args.add("--rm");

        if (!volumeMounts.isEmpty() || startupArchiveTraining.isPresent()) {
            args.addAll(NativeImageBuildLocalContainerRunner.getVolumeAccessArguments(containerRuntime, containerImage));
        }

        args.add("-p");
        args.add(httpPort + ":" + httpPort);
        args.add("-p");
        args.add(httpsPort + ":" + httpsPort);
        if (entryPoint.isPresent()) {
            args.add("--entrypoint");
            args.add(entryPoint.get());
        }
        for (Map.Entry<Integer, Integer> entry : additionalExposedPorts.entrySet()) {
            args.add("-p");
            args.add(entry.getKey() + ":" + entry.getValue());
        }
        for (Map.Entry<String, String> entry : volumeMounts.entrySet()) {
            NativeImageBuildLocalContainerRunner.addVolumeParameter(entry.getKey(), entry.getValue(), args, containerRuntime);
        }
        // if the dev services resulted in creating a dedicated network, then use it
        if (devServicesLaunchResult.networkId() != null) {
            args.add("--net=" + devServicesLaunchResult.networkId());
        }

        args.addAll(toEnvVar("quarkus.log.category.\"io.quarkus\".level", "INFO"));
        if (DefaultJarLauncher.HTTP_PRESENT) {
            args.addAll(toEnvVar("quarkus.http.port", "" + httpPort));
            args.addAll(toEnvVar("quarkus.http.ssl-port", "" + httpsPort));
            // This won't be correct when using the random port, but it's really only used by us for the rest client tests
            // in the main module, since those tests hit the application itself
            args.addAll(toEnvVar("test.url", LauncherUtil.generateTestUrl()));
        }
        if (testProfile != null) {
            args.addAll(toEnvVar("quarkus.profile", testProfile));
        }

        for (var e : systemProps.entrySet()) {
            args.addAll(toEnvVar(e.getKey(), e.getValue()));
        }

        for (var e : env.entrySet()) {
            if (startupArchiveTraining.isPresent() && "JAVA_TOOL_OPTIONS".equals(e.getKey())) {
                continue;
            }
            args.addAll(envAsLaunchArg(e.getKey(), e.getValue()));
        }

        handleAotFileArgs(args, containerRuntime);

        for (var e : labels.entrySet()) {
            args.add("--label");
            args.add(e.getKey() + "=" + e.getValue());
        }
        args.add(containerImage);
        args.addAll(programArgs);

        final Path logPath = logRuntimeConfig.file().path().toPath();
        try {
            Files.deleteIfExists(logPath);
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }
        } catch (FileSystemException e) {
            log.warnf("Log file %s deletion failed, could happen on Windows, we can carry on.", logPath);
        }

        log.infof("Executing \"%s\"", String.join(" ", args));

        final Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction = createStartedFunction();

        // We rely on the container writing log to stdout. If it just writes to a logfile inside itself, we would have
        // to mount /work/ directory to get quarkus.log.
        containerProcess = new ProcessBuilder(args)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()))
                .start();

        if (startedFunction != null) {
            waitForStartedFunction(startedFunction, containerProcess, waitTimeSeconds, logPath);
            return ListeningAddresses.EMPTY;
        } else {
            log.info("Wait for server to start by capturing listening data...");
            ListeningAddresses result = waitForCapturedListeningData(containerProcess, logPath, waitTimeSeconds);
            result.address().ifPresent(listeningAddress -> log.infof("Server started on port %s", listeningAddress.port()));
            return result;
        }
    }

    private void handleAotFileArgs(List<String> args, ContainerRuntime containerRuntime) throws IOException {
        if (startupArchiveTraining.isPresent()) {
            StartupArchiveContainerPlan plan = StartupArchiveContainerPlan.create(startupArchiveTraining.get(),
                    additionalRecordingArgs);
            plan.prepareHostOutput();
            args.addAll(toEnvVar("JAVA_TOOL_OPTIONS",
                    appendJavaToolOptions(inheritedJavaToolOptions(), plan.recordingJavaToolOptions())));
            NativeImageBuildLocalContainerRunner.addVolumeParameter(plan.hostDirectory().toString(),
                    plan.containerDirectory(), args, containerRuntime);
            return;
        }
        if (generateAotFile) {
            if (containerWorkingDirectory.isPresent()) {
                args.addAll(toEnvVar("JAVA_TOOL_OPTIONS",
                        "-XX:AOTMode=record -XX:AOTConfiguration=%s/%s".formatted(AOT_DIR, AOT_CONF_FILE_NAME)));
                args.addAll(additionalRecordingArgs);
                Path containerAotDir = Path.of(outputTargetDirectory).resolve(AOT_DIR);
                Files.createDirectories(containerAotDir);
                containerAotDir.toFile().setReadable(true, false);
                containerAotDir.toFile().setWritable(true, false);
                containerAotDir.toFile().setExecutable(true, false);
                Files.deleteIfExists(containerAotDir.resolve(AOT_CONF_FILE_NAME));
                NativeImageBuildLocalContainerRunner.addVolumeParameter(containerAotDir.toAbsolutePath().toString(),
                        containerWorkingDirectory.get() + "/%s".formatted(AOT_DIR), args, containerRuntime);
            } else {
                // TODO: figure it out
                log.warn("AOT file cannot be generated because the working directory could not be determined.");
            }

        }
    }

    private int getRandomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public void includeAsSysProps(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    private static List<String> envAsLaunchArg(String name, String value) {
        return List.of("--env", String.format("%s=%s", name, value));
    }

    private List<String> toEnvVar(String property, String value) {
        if ((property != null) && (!property.isEmpty())) {
            return envAsLaunchArg(convertPropertyToEnvVar(property), value);
        }
        return Collections.emptyList();
    }

    private String convertPropertyToEnvVar(String property) {
        return StringUtil.replaceNonAlphanumericByUnderscores(property).toUpperCase();
    }

    @Override
    public void close() {
        boolean interrupted = false;
        boolean containerStopSucceeded = false;
        Process dockerKillProcess = null;
        try {
            dockerKillProcess = new ProcessBuilder(containerRuntimeBinaryName, "kill", "--signal=SIGINT",
                    containerName)
                    .redirectError(DISCARD)
                    .redirectOutput(DISCARD).start();
            log.debug("Wait for container to stop");
            if (!dockerKillProcess.waitFor(10, TimeUnit.SECONDS)) {
                if (!BoundedProcessRunner.forceTerminateAndWait(dockerKillProcess, PROCESS_FORCE_TIMEOUT)) {
                    log.warnf("Container stop helper for '%s' did not terminate after it was forcibly destroyed",
                            containerName);
                }
                log.errorf("Timed out stopping container '%s'", containerName);
            } else if (dockerKillProcess.exitValue() != 0) {
                log.errorf("Unable to stop container '%s'; helper exited with code %d", containerName,
                        dockerKillProcess.exitValue());
            } else {
                containerStopSucceeded = true;
            }
        } catch (InterruptedException e) {
            interrupted = true;
            if (dockerKillProcess != null
                    && !BoundedProcessRunner.forceTerminateAndWait(dockerKillProcess, PROCESS_FORCE_TIMEOUT)) {
                log.warnf("Interrupted container stop helper for '%s' did not terminate", containerName);
            }
            log.errorf("Interrupted while stopping container '%s'", containerName);
        } catch (IOException e) {
            log.errorf("Unable to stop container '%s'", containerName);
        }

        if (containerProcess != null && containerStopSucceeded && !interrupted) {
            try {
                containerProcess.waitFor(getAdjustedShutdownTimeout().getSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (!containerStopSucceeded || containerProcess != null && containerProcess.isAlive()) {
            removeOwnedContainer();
            if (Thread.interrupted()) {
                interrupted = true;
            }
        }
        if (containerProcess != null && containerProcess.isAlive()
                && !BoundedProcessRunner.forceTerminateAndWait(containerProcess, PROCESS_FORCE_TIMEOUT)) {
            log.warnf("Container process '%s' did not terminate after it was forcibly destroyed", containerName);
        }

        RuntimeException trainingFailure = null;
        if (startupArchiveTraining.isPresent()) {
            JvmStartupArchiveTraining training = startupArchiveTraining.get();
            try {
                if (training.type() == JvmStartupOptimizerArchiveType.AOT) {
                    Path aotConfiguration = training.aotConfigurationDestination();
                    if (Files.isRegularFile(aotConfiguration)) {
                        createStartupArchiveFromAotConfiguration(
                                StartupArchiveContainerPlan.create(training, additionalRecordingArgs));
                    } else {
                        throw new IllegalStateException("The AOT recording configuration was not produced at "
                                + aotConfiguration
                                + ". The application may not have stopped gracefully; increasing quarkus.shutdown.timeout "
                                + "may help.");
                    }
                }
                training.validateProducedArchive();
            } catch (RuntimeException e) {
                trainingFailure = e;
            }
        } else if (generateAotFile) {
            Path aotConfigFile = Path.of(outputTargetDirectory).resolve(AOT_DIR).resolve(AOT_CONF_FILE_NAME);
            if (Files.exists(aotConfigFile)) {
                createAotFileFromAotConfFile(aotConfigFile);
            } else {
                log.warnf("The AOT conf file %s was not generated, the AOT-optimized container image won't be created. " +
                        "A possible cause could be that your application didn't stop gracefully and was killed forcibly. " +
                        "Raising quarkus.shutdown.timeout might help solve the issue.", aotConfigFile);
            }
        }

        log.debug("Container stopped");
        executorService.shutdown();

        recordMetadata();
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        if (trainingFailure != null) {
            throw trainingFailure;
        }
    }

    private void removeOwnedContainer() {
        Process removeProcess = null;
        try {
            removeProcess = new ProcessBuilder(containerRuntimeBinaryName, "rm", "--force", containerName)
                    .redirectError(DISCARD)
                    .redirectOutput(DISCARD)
                    .start();
            if (!removeProcess.waitFor(10, TimeUnit.SECONDS)) {
                if (!BoundedProcessRunner.forceTerminateAndWait(removeProcess, PROCESS_FORCE_TIMEOUT)) {
                    log.warnf("Forced container-removal helper for '%s' did not terminate", containerName);
                }
                log.warnf("Timed out forcibly removing container '%s'", containerName);
            } else if (removeProcess.exitValue() != 0) {
                log.warnf("Unable to forcibly remove container '%s'; helper exited with code %d", containerName,
                        removeProcess.exitValue());
            }
        } catch (InterruptedException e) {
            if (removeProcess != null
                    && !BoundedProcessRunner.forceTerminateAndWait(removeProcess, PROCESS_FORCE_TIMEOUT)) {
                log.warnf("Interrupted container-removal helper for '%s' did not terminate", containerName);
            }
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.warnf("Unable to forcibly remove container '%s': %s", containerName, e.getMessage());
        }
    }

    private Duration getAdjustedShutdownTimeout() {
        return shutdownTimeout
                .plus(generateAotFile || startupArchiveTraining.isPresent() ? Duration.ofMinutes(1) : Duration.ofSeconds(10));
    }

    static String appendJavaToolOptions(String inherited, String startupArchiveOption) {
        if (inherited == null || inherited.isBlank()) {
            return startupArchiveOption;
        }
        return inherited + " " + startupArchiveOption;
    }

    private String inheritedJavaToolOptions() throws IOException {
        String configured = env.get("JAVA_TOOL_OPTIONS");
        return configured == null ? inspectJavaToolOptions(containerRuntimeBinaryName, containerImage) : configured;
    }

    private static String inspectJavaToolOptions(String containerRuntime, String image) throws IOException {
        try {
            BoundedProcessRunner.Result result = BoundedProcessRunner.capture(new ProcessBuilder(
                    containerRuntime,
                    "image",
                    "inspect",
                    "--format={{range .Config.Env}}{{println .}}{{end}}",
                    image),
                    Duration.ofSeconds(30),
                    PROCESS_FORCE_TIMEOUT,
                    "Inspecting environment of base container image " + image);
            if (result.exitCode() != 0) {
                throw new IOException(
                        "Unable to inspect environment of base container image " + image + ": " + result.output());
            }
            return result.output().lines()
                    .filter(line -> line.startsWith("JAVA_TOOL_OPTIONS="))
                    .map(line -> line.substring("JAVA_TOOL_OPTIONS=".length()))
                    .findFirst()
                    .orElse("");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while inspecting environment of base container image " + image, e);
        }
    }

    private void createAotFileFromAotConfFile(Path aotConfigFile) {
        aotConfigFile.toFile().setReadable(true, false);
        List<String> args = new ArrayList<>();
        args.add(containerRuntimeBinaryName);
        args.add("run");
        if (!argLine.isEmpty()) {
            args.addAll(argLine);
        }
        args.add("--name");
        args.add(containerName);
        args.add("-i"); // Interactive, write logs to stdout
        args.add("--rm");

        ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime();
        if (!volumeMounts.isEmpty()) {
            args.addAll(NativeImageBuildLocalContainerRunner.getVolumeAccessArguments(containerRuntime, containerImage));
        }

        args.addAll(toEnvVar("JAVA_TOOL_OPTIONS",
                "-XX:AOTMode=create -XX:AOTConfiguration=%s/%s -XX:AOTCache=%s/%s".formatted(AOT_DIR,
                        AOT_CONF_FILE_NAME, AOT_DIR, AOT_FILE_NAME)));
        Path containerAotDir = Path.of(outputTargetDirectory).resolve(AOT_DIR);
        Path aotFilePath = containerAotDir.resolve(AOT_FILE_NAME);
        try {
            Files.deleteIfExists(aotFilePath);
        } catch (IOException ignored) {

        }
        NativeImageBuildLocalContainerRunner.addVolumeParameter(containerAotDir.toAbsolutePath().toString(),
                containerWorkingDirectory.get() + "/%s".formatted(AOT_DIR), args, containerRuntime);
        args.add(containerImage);
        args.addAll(programArgs);

        Process process = null;
        boolean processTerminated = true;
        try {
            process = new ProcessBuilder(args)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                processTerminated = BoundedProcessRunner.forceTerminateAndWait(process, PROCESS_FORCE_TIMEOUT);
                log.warn("AOT creation process timed out");
                return;
            }
            if (Files.exists(aotFilePath)) {
                log.infof("AOT file '%s' created", aotFilePath.toAbsolutePath());
            } else {
                log.warnf("AOT file '%s' was not created, the AOT-optimized container image won't be created", aotFilePath);
            }
        } catch (InterruptedException e) {
            if (process != null) {
                processTerminated = BoundedProcessRunner.forceTerminateAndWait(process, PROCESS_FORCE_TIMEOUT);
            }
            Thread.currentThread().interrupt();
            log.warn("AOT creation process was interrupted", e);
        } catch (IOException e) {
            log.warn("Unable to create AOT file", e);
        } finally {
            if (processTerminated) {
                try {
                    Files.deleteIfExists(aotConfigFile);
                } catch (IOException e) {
                    log.debug("Unable to delete AOT config file", e);
                }
            } else {
                log.warnf("Retaining AOT configuration file '%s' because its process did not terminate", aotConfigFile);
            }
        }
    }

    private void createStartupArchiveFromAotConfiguration(StartupArchiveContainerPlan plan) {
        List<String> args = new ArrayList<>();
        args.add(containerRuntimeBinaryName);
        args.add("run");
        if (!argLine.isEmpty()) {
            args.addAll(argLine);
        }
        args.add("--name");
        args.add(containerName);
        args.add("-i");
        args.add("--rm");

        ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime();
        args.addAll(NativeImageBuildLocalContainerRunner.getVolumeAccessArguments(containerRuntime, containerImage));
        NativeImageBuildLocalContainerRunner.addVolumeParameter(plan.hostDirectory().toString(),
                plan.containerDirectory(), args, containerRuntime);

        Process process = null;
        boolean processTerminated = true;
        try {
            args.addAll(toEnvVar("JAVA_TOOL_OPTIONS",
                    appendJavaToolOptions(inheritedJavaToolOptions(), plan.createJavaToolOptions().orElseThrow())));
            args.add(containerImage);
            args.addAll(programArgs);
            process = new ProcessBuilder(args)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(getAdjustedShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                processTerminated = BoundedProcessRunner.forceTerminateAndWait(process, PROCESS_FORCE_TIMEOUT);
                if (!processTerminated) {
                    throw new IllegalStateException(
                            "The AOT startup-archive create container did not terminate after it was forcibly destroyed");
                }
                throw new IllegalStateException("The AOT startup-archive create container timed out");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        "The AOT startup-archive create container failed with exit code " + process.exitValue());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to run the AOT startup-archive create container", e);
        } catch (InterruptedException e) {
            if (process != null) {
                processTerminated = BoundedProcessRunner.forceTerminateAndWait(process, PROCESS_FORCE_TIMEOUT);
            }
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running the AOT startup-archive create container", e);
        } finally {
            // Retain the mounted recording configuration while a possibly live container can still read it.
            if (processTerminated) {
                try {
                    Files.deleteIfExists(plan.training().aotConfigurationDestination());
                } catch (IOException e) {
                    log.debug("Unable to delete AOT configuration file", e);
                }
            } else {
                log.warnf("Retaining AOT configuration file '%s' because its process did not terminate",
                        plan.training().aotConfigurationDestination());
            }
        }
    }

    /**
     * Maps typed training metadata to one host mount and the JVM options visible inside the container.
     * <p>
     * AOT training records an intermediate configuration and therefore has a separate create phase. SCC training
     * populates its directory during the integration-test run and has no create phase.
     */
    static record StartupArchiveContainerPlan(
            JvmStartupArchiveTraining training,
            Path hostDirectory,
            String containerDirectory,
            String recordingJavaToolOptions,
            Optional<String> createJavaToolOptions) {

        static StartupArchiveContainerPlan create(JvmStartupArchiveTraining training,
                List<String> additionalRecordingArguments) {
            String recordingArguments;
            Optional<String> createArguments;
            switch (training.type()) {
                case AOT -> {
                    recordingArguments = "-XX:AOTMode=record -XX:AOTConfiguration="
                            + training.containerAotConfigurationPath();
                    createArguments = Optional.of("-XX:AOTMode=create -XX:AOTConfiguration="
                            + training.containerAotConfigurationPath() + " "
                            + training.type().renderRuntimeOption(training.containerArchivePath()));
                }
                case SCC -> {
                    recordingArguments = "-Xshareclasses:name=quarkus-app,cacheDir="
                            + training.containerArchivePath();
                    createArguments = Optional.empty();
                }
                case AppCDS -> throw new IllegalArgumentException(
                        "AppCDS is not supported by integration-test startup-archive training");
                default -> throw new IllegalArgumentException("Unsupported startup-archive type " + training.type());
            }
            if (!additionalRecordingArguments.isEmpty()) {
                String additional = String.join(" ", additionalRecordingArguments);
                recordingArguments += " " + additional;
                createArguments = createArguments.map(value -> value + " " + additional);
            }
            return new StartupArchiveContainerPlan(training, training.hostDirectory().toAbsolutePath(),
                    training.containerDirectory().orElseThrow(), recordingArguments, createArguments);
        }

        /**
         * Prepares a fresh host destination with permissions suitable for a container mount.
         */
        void prepareHostOutput() throws IOException {
            training.prepareContainerMountedHostOutput();
        }
    }

    private void recordMetadata() {
        Path quarkusArtifactMetadataPath = Path.of(outputTargetDirectory).resolve("quarkus-container-it.properties");
        Properties properties = new Properties();
        properties.put("original-container-image", containerImage);
        if (containerWorkingDirectory.isPresent()) {
            properties.put("container-working-directory", containerWorkingDirectory.get());
        }
        Path aotFile = startupArchiveTraining
                .filter(training -> training.type() == JvmStartupOptimizerArchiveType.AOT)
                .map(JvmStartupArchiveTraining::destination)
                .orElseGet(() -> Path.of(outputTargetDirectory).resolve(AOT_DIR).resolve(AOT_FILE_NAME));
        if (Files.exists(aotFile)) {
            properties.setProperty("aot-file", aotFile.toAbsolutePath().toString());
        }
        try {
            PropertyUtils.store(properties, quarkusArtifactMetadataPath, "Generated by Quarkus - Do not edit manually");
        } catch (IOException e) {
            log.warn("Unable to write `quarkus-container-it.properties` metadata file", e);
        }
    }
}
