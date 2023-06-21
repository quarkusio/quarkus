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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;

import io.quarkus.runtime.util.ContainerRuntimeUtil;
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

    private Map<String, String> labels;
    private final Map<String, String> systemProps = new HashMap<>();
    private boolean isSsl;
    private final String containerName = "quarkus-integration-test-" + RandomStringUtils.random(5, true, false);
    private String containerRuntimeBinaryName;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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
        this.labels = initContext.labels();
    }

    @Override
    public LaunchResult runToCompletion(String[] args) {
        throw new UnsupportedOperationException("not implemented for docker yet");
    }

    @Override
    public void start() throws IOException {

        containerRuntimeBinaryName = determineBinary();

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
        args.add("-p");
        args.add(httpPort + ":" + httpPort);
        args.add("-p");
        args.add(httpsPort + ":" + httpsPort);
        for (Map.Entry<Integer, Integer> entry : additionalExposedPorts.entrySet()) {
            args.add("-p");
            args.add(entry.getKey() + ":" + entry.getValue());
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
            final ListeningAddress result = waitForCapturedListeningData(containerProcess, logFile, waitTimeSeconds);
            updateConfigForPort(result.getPort());
            isSsl = result.isSsl();
        }
    }

    private String determineBinary() {
        return ContainerRuntimeUtil.detectContainerRuntime().getExecutableName();
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
        try {
            final Process dockerStopProcess = new ProcessBuilder(containerRuntimeBinaryName, "stop", containerName)
                    .redirectError(DISCARD)
                    .redirectOutput(DISCARD).start();
            dockerStopProcess.waitFor(10, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            log.errorf("Unable to stop container '%s'", containerName);
        }
        executorService.shutdown();
    }
}
