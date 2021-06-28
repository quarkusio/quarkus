package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.installAndGetSomeConfig;
import static io.quarkus.test.common.LauncherUtil.updateConfigForPort;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static java.lang.ProcessBuilder.Redirect.DISCARD;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.Config;

import io.quarkus.test.common.http.TestHTTPResourceManager;

public class DockerContainerLauncher implements ArtifactLauncher {

    private static final int DEFAULT_PORT = 8081;
    private static final int DEFAULT_HTTPS_PORT = 8444;
    private static final long DEFAULT_WAIT_TIME = 60;

    private final String containerImage;
    private final String profile;
    private Process quarkusProcess;
    private int httpPort;
    private int httpsPort;
    private final long jarWaitTime;
    private final Map<String, String> systemProps = new HashMap<>();
    private final boolean pullRequired;

    private boolean isSsl;

    private DockerContainerLauncher(String containerImage, Config config, boolean pullRequired) {
        this(containerImage,
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                config.getValue("quarkus.test.jar-wait-time", OptionalLong.class).orElse(DEFAULT_WAIT_TIME),
                config.getOptionalValue("quarkus.test.native-image-profile", String.class)
                        .orElse(null),
                pullRequired);
    }

    public DockerContainerLauncher(String containerImage, boolean pullRequired) {
        this(containerImage, installAndGetSomeConfig(), pullRequired);
    }

    private DockerContainerLauncher(String containerImage, int httpPort, int httpsPort, long jarWaitTime, String profile,
            boolean pullRequired) {
        this.containerImage = containerImage;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.jarWaitTime = jarWaitTime;
        this.profile = profile;
        this.pullRequired = pullRequired;
    }

    public void start() throws IOException {

        if (pullRequired) {
            System.out.println("Pulling container image '" + containerImage + "'");
            try {
                int pullResult = new ProcessBuilder().redirectError(DISCARD).redirectOutput(DISCARD)
                        .command(List.of("docker", "pull", containerImage).toArray(new String[0])).start().waitFor();
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
        args.add("docker"); // TODO: determine this dynamically?
        args.add("run");
        args.add("--rm");
        args.add("-p");
        args.add(httpPort + ":" + httpPort);
        args.add("-p");
        args.add(httpsPort + ":" + httpsPort);
        args.addAll(toEnvVar("quarkus.http.port", "" + httpPort));
        args.addAll(toEnvVar("quarkus.http.ssl-port", "" + httpsPort));
        // this won't be correct when using the random port but it's really only used by us for the rest client tests
        // in the main module, since those tests hit the application itself
        args.addAll(toEnvVar("test.url", TestHTTPResourceManager.getUri()));
        if (profile != null) {
            args.addAll(toEnvVar("quarkus.profile", profile));
        }

        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.addAll(toEnvVar(e.getKey(), e.getValue()));
        }
        args.add(containerImage);

        Path logFile = PropertyTestUtil.getLogFilePath();
        Files.deleteIfExists(logFile);
        Files.createDirectories(logFile.getParent());

        System.out.println("Executing " + args);

        // the idea here is to obtain the logs of the application simply by redirecting all its output the a file
        // this is done in contrast with the JarLauncher and NativeImageLauncher because in the case of the container
        // the log itself is written inside the container
        quarkusProcess = new ProcessBuilder(args).redirectError(logFile.toFile()).redirectOutput(logFile.toFile()).start();

        ListeningAddress result = waitForCapturedListeningData(quarkusProcess, logFile, jarWaitTime);
        updateConfigForPort(result.getPort());
        isSsl = result.isSsl();
    }

    private int getRandomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public boolean listensOnSsl() {
        return isSsl;
    }

    public void addSystemProperties(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    private List<String> toEnvVar(String property, String value) {
        if ((property != null) && (!property.isEmpty())) {
            List<String> result = new ArrayList<>(2);
            result.add("--env");
            result.add(String.format("%s=%s", convertPropertyToEnVar(property), value));
            return result;
        }
        return Collections.emptyList();
    }

    private String convertPropertyToEnVar(String property) {
        return property.toUpperCase().replace('-', '_').replace('.', '_').replace('/', '_');
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
