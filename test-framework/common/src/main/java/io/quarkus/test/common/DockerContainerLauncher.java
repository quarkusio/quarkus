package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.installAndGetSomeConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private int port;
    private final int httpsPort;
    private final long jarWaitTime;
    private final Map<String, String> systemProps = new HashMap<>();

    private DockerContainerLauncher(String containerImage, Config config) {
        this(containerImage,
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                config.getValue("quarkus.test.jar-wait-time", OptionalLong.class).orElse(DEFAULT_WAIT_TIME),
                config.getOptionalValue("quarkus.test.native-image-profile", String.class)
                        .orElse(null));
    }

    public DockerContainerLauncher(String containerImage) {
        this(containerImage, installAndGetSomeConfig());
    }

    public DockerContainerLauncher(String containerImage, int port, int httpsPort, long jarWaitTime, String profile) {
        this.containerImage = containerImage;
        this.port = port;
        this.httpsPort = httpsPort;
        this.jarWaitTime = jarWaitTime;
        this.profile = profile;
    }

    public void start() throws IOException {

        System.setProperty("test.url", TestHTTPResourceManager.getUri());

        List<String> args = new ArrayList<>();
        args.add("docker"); // TODO: determine this dynamically?
        args.add("run");
        args.add("--rm");
        args.add("-p");
        args.add(port + ":" + port);
        args.add("-p");
        args.add(httpsPort + ":" + httpsPort);
        args.addAll(toEnvVar("quarkus.http.port", "" + port));
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

        System.out.println("Executing " + args);

        quarkusProcess = Runtime.getRuntime().exec(args.toArray(new String[0]));
        port = LauncherUtil.doStart(quarkusProcess, port, httpsPort, jarWaitTime, null);
    }

    public boolean isDefaultSsl() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", port));
            return false;
        } catch (IOException e) {
            return true;
        }
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
        return property.toUpperCase().replace('-', '_').replace('.', '_');
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
