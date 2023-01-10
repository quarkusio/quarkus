package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.createStartedFunction;
import static io.quarkus.test.common.LauncherUtil.updateConfigForPort;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static io.quarkus.test.common.LauncherUtil.waitForStartedFunction;
import static java.lang.ProcessBuilder.Redirect.DISCARD;
import static java.lang.ProcessBuilder.Redirect.PIPE;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang3.RandomStringUtils;

import io.quarkus.runtime.util.ContainerRuntimeUtil;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.common.utils.StringUtil;

public class DefaultDockerContainerLauncher implements DockerContainerArtifactLauncher {

    private int httpPort;
    private int httpsPort;
    private long waitTimeSeconds;
    private String testProfile;
    private List<String> argLine;
    private ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult;
    private String containerImage;
    private boolean pullRequired;
    private Map<Integer, Integer> additionalExposedPorts;

    private final Map<String, String> systemProps = new HashMap<>();

    private boolean isSsl;

    private String containerName;

    private String containerRuntimeBinaryName;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void init(DockerContainerArtifactLauncher.DockerInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
        this.testProfile = initContext.testProfile();
        this.argLine = initContext.argLine();
        this.devServicesLaunchResult = initContext.getDevServicesLaunchResult();
        this.containerImage = initContext.containerImage();
        this.pullRequired = initContext.pullRequired();
        this.additionalExposedPorts = initContext.additionalExposedPorts();
    }

    @Override
    public LaunchResult runToCompletion(String[] args) {
        throw new UnsupportedOperationException("not implemented for docker yet");
    }

    @Override
    public void start() throws IOException {

        containerRuntimeBinaryName = determineBinary();

        if (pullRequired) {
            System.out.println("Pulling container image '" + containerImage + "'");
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

        List<String> args = new ArrayList<>();
        args.add(containerRuntimeBinaryName);
        args.add("run");
        if (!argLine.isEmpty()) {
            args.addAll(argLine);
        }
        args.add("--name");
        containerName = "quarkus-integration-test-" + RandomStringUtils.random(5, true, false);
        args.add(containerName);
        args.add("--rm");
        args.add("-p");
        args.add(httpPort + ":" + httpPort);
        args.add("-p");
        args.add(httpsPort + ":" + httpsPort);
        for (var entry : additionalExposedPorts.entrySet()) {
            args.add("-p");
            args.add(entry.getKey() + ":" + entry.getValue());
        }
        // if the dev services resulted in creating a dedicated network, then use it
        if (devServicesLaunchResult.networkId() != null) {
            args.add("--net=" + devServicesLaunchResult.networkId());
        }

        if (DefaultJarLauncher.HTTP_PRESENT) {
            args.addAll(toEnvVar("quarkus.http.port", "" + httpPort));
            args.addAll(toEnvVar("quarkus.http.ssl-port", "" + httpsPort));
            // this won't be correct when using the random port but it's really only used by us for the rest client tests
            // in the main module, since those tests hit the application itself
            args.addAll(toEnvVar("test.url", TestHTTPResourceManager.getUri()));
        }
        if (testProfile != null) {
            args.addAll(toEnvVar("quarkus.profile", testProfile));
        }

        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.addAll(toEnvVar(e.getKey(), e.getValue()));
        }
        args.add(containerImage);

        Path logFile = PropertyTestUtil.getLogFilePath();
        Files.deleteIfExists(logFile);
        Files.createDirectories(logFile.getParent());

        Path containerLogFile = Paths.get("target", "container.log");
        Files.createDirectories(containerLogFile.getParent());
        FileOutputStream containerLogOutputStream = new FileOutputStream(containerLogFile.toFile(), true);

        System.out.println("Executing \"" + String.join(" ", args) + "\"");

        Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction = createStartedFunction();

        // the idea here is to obtain the logs of the application simply by redirecting all its output the a file
        // this is done in contrast with the JarLauncher and NativeImageLauncher because in the case of the container
        // the log itself is written inside the container
        Process quarkusProcess = new ProcessBuilder(args).redirectError(PIPE).redirectOutput(PIPE).start();
        InputStream tee = new TeeInputStream(quarkusProcess.getInputStream(), new FileOutputStream(logFile.toFile()));
        executorService.submit(() -> IOUtils.copy(tee, containerLogOutputStream));

        if (startedFunction != null) {
            IntegrationTestStartedNotifier.Result result = waitForStartedFunction(startedFunction, quarkusProcess,
                    waitTimeSeconds, logFile);
            isSsl = result.isSsl();
        } else {
            ListeningAddress result = waitForCapturedListeningData(quarkusProcess, logFile, waitTimeSeconds);
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

    private List<String> toEnvVar(String property, String value) {
        if ((property != null) && (!property.isEmpty())) {
            List<String> result = new ArrayList<>(2);
            result.add("--env");
            result.add(String.format("%s=%s", convertPropertyToEnvVar(property), value));
            return result;
        }
        return Collections.emptyList();
    }

    private String convertPropertyToEnvVar(String property) {
        return StringUtil.replaceNonAlphanumericByUnderscores(property).toUpperCase();
    }

    @Override
    public void close() {
        try {
            Process dockerStopProcess = new ProcessBuilder(containerRuntimeBinaryName, "stop", containerName)
                    .redirectError(DISCARD)
                    .redirectOutput(DISCARD).start();
            dockerStopProcess.waitFor(10, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            System.out.println("Unable to stop container '" + containerName + "'");
        }
        executorService.shutdown();
    }

}
