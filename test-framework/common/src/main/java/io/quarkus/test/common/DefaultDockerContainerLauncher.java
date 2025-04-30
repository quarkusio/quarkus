package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.createStartedFunction;
import static io.quarkus.test.common.LauncherUtil.updateConfigForPort;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static io.quarkus.test.common.LauncherUtil.waitForStartedFunction;
import static java.lang.ProcessBuilder.Redirect.DISCARD;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.steps.NativeImageBuildLocalContainerRunner;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.common.utils.StringUtil;

public class DefaultDockerContainerLauncher implements DockerContainerArtifactLauncher {

    private static final Logger log = Logger.getLogger(DefaultDockerContainerLauncher.class);

    private int httpPort;
    private int httpsPort;
    private long waitTimeSeconds;
    private String testProfile;
    private List<String> argLine;
    private Map<String, String> env;
    private ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult;
    private String containerImage;
    private boolean pullRequired;
    private Map<Integer, Integer> additionalExposedPorts;

    private Map<String, String> volumeMounts;
    private Map<String, String> labels;
    private final Map<String, String> systemProps = new HashMap<>();
    private boolean isSsl;
    private final String containerName = "quarkus-integration-test-" + RandomStringUtils.insecure().next(5, true, false);
    private String containerRuntimeBinaryName;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Optional<String> entryPoint;
    private List<String> programArgs;

    @Override
    public void init(DockerContainerArtifactLauncher.DockerInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
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
        this.programArgs = initContext.programArgs();
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

            System.setProperty("test.url", TestHTTPResourceManager.getUri());

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

            if (!volumeMounts.isEmpty()) {
                args.addAll(NativeImageBuildLocalContainerRunner.getVolumeAccessArguments(containerRuntime));
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
                // This won't be correct when using the random port, but it's really only used by us for the rest client tests
                // in the main module, since those tests hit the application itself
                args.addAll(toEnvVar("test.url", TestHTTPResourceManager.getUri()));
            }
            if (testProfile != null) {
                args.addAll(toEnvVar("quarkus.profile", testProfile));
            }

            for (var e : systemProps.entrySet()) {
                args.addAll(toEnvVar(e.getKey(), e.getValue()));
            }

            for (var e : env.entrySet()) {
                args.addAll(envAsLaunchArg(e.getKey(), e.getValue()));
            }

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
    public void start() throws IOException {

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

        System.setProperty("test.url", TestHTTPResourceManager.getUri());

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

        if (!volumeMounts.isEmpty()) {
            args.addAll(NativeImageBuildLocalContainerRunner.getVolumeAccessArguments(containerRuntime));
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
            args.addAll(toEnvVar("test.url", TestHTTPResourceManager.getUri()));
        }
        if (testProfile != null) {
            args.addAll(toEnvVar("quarkus.profile", testProfile));
        }

        for (var e : systemProps.entrySet()) {
            args.addAll(toEnvVar(e.getKey(), e.getValue()));
        }

        for (var e : env.entrySet()) {
            args.addAll(envAsLaunchArg(e.getKey(), e.getValue()));
        }

        for (var e : labels.entrySet()) {
            args.add("--label");
            args.add(e.getKey() + "=" + e.getValue());
        }
        args.add(containerImage);
        args.addAll(programArgs);

        final Path logFile = PropertyTestUtil.getLogFilePath();
        try {
            Files.deleteIfExists(logFile);
            Files.createDirectories(logFile.getParent());
        } catch (FileSystemException e) {
            log.warnf("Log file %s deletion failed, could happen on Windows, we can carry on.", logFile);
        }

        log.infof("Executing \"%s\"", String.join(" ", args));

        final Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction = createStartedFunction();

        // We rely on the container writing log to stdout. If it just writes to a logfile inside itself, we would have
        // to mount /work/ directory to get quarkus.log.
        final Process containerProcess = new ProcessBuilder(args)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()))
                .start();

        if (startedFunction != null) {
            final IntegrationTestStartedNotifier.Result result = waitForStartedFunction(startedFunction, containerProcess,
                    waitTimeSeconds, logFile);
            isSsl = result.isSsl();
        } else {
            log.info("Wait for server to start by capturing listening data...");
            final ListeningAddress result = waitForCapturedListeningData(containerProcess, logFile, waitTimeSeconds);
            log.infof("Server started on port %s", result.getPort());
            updateConfigForPort(result.getPort());
            isSsl = result.isSsl();
        }
    }

    private int getRandomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public boolean listensOnSsl() {
        return isSsl;
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
        log.info("Close the container");
        try {
            final Process dockerStopProcess = new ProcessBuilder(containerRuntimeBinaryName, "stop", containerName)
                    .redirectError(DISCARD)
                    .redirectOutput(DISCARD).start();
            log.debug("Wait for container to stop");
            dockerStopProcess.waitFor(10, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            log.errorf("Unable to stop container '%s'", containerName);
        }
        log.debug("Container stopped");
        executorService.shutdown();
    }
}
